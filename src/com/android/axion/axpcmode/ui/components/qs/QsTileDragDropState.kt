/*
 * Copyright (C) 2025-2026 AxionOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.axion.axpcmode.ui.components.qs

import android.content.ClipData
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.toRect
import com.android.axion.axpcmode.services.AvailableTileData
import com.android.axion.axpcmode.services.QSTileData

enum class DragType {

    Move,
    Add,
}

val TilePlacementSpec: SpringSpec<IntOffset> =
    SpringSpec(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)

data class ItemPosition(
    val spec: String,
    val index: Int,
    val offset: IntOffset,
    val size: IntSize,
)

class QsTileDragDropState(initialTiles: List<QSTileData>, val columns: Int = 4) {

    private val _tiles: SnapshotStateList<QSTileData> = initialTiles.toMutableStateList()
    val tiles: List<QSTileData>
        get() = _tiles

    private val _itemPositions = mutableStateMapOf<String, ItemPosition>()
    val itemPositions: Map<String, ItemPosition>
        get() = _itemPositions

    var draggedTile by mutableStateOf<QSTileData?>(null)
        private set

    var draggedPosition by mutableStateOf(Offset.Unspecified)
        private set

    var dragType by mutableStateOf<DragType?>(null)
        private set

    val dragInProgress: Boolean
        get() = draggedTile != null

    val draggedSpec: String?
        get() = draggedTile?.spec

    fun isMoving(spec: String): Boolean = draggedTile?.spec == spec

    fun updateItemPosition(spec: String, index: Int, offset: IntOffset, size: IntSize) {
        _itemPositions[spec] = ItemPosition(spec, index, offset, size)
    }

    fun clearItemPositions() {
        _itemPositions.clear()
    }

    fun findItemAtOffset(relativeOffset: Offset): ItemPosition? {
        return _itemPositions.values.firstOrNull { item ->
            val rect = IntRect(item.offset, item.size).toRect()
            rect.contains(relativeOffset)
        }
    }

    fun onStarted(tile: QSTileData, type: DragType = DragType.Move) {
        draggedTile = tile
        dragType = type
    }

    fun onTargeting(targetIndex: Int) {
        val dragged = draggedTile ?: return

        val fromIndex = _tiles.indexOfFirst { it.spec == dragged.spec }

        if (fromIndex == targetIndex) return

        if (fromIndex != -1) {

            val cell = _tiles.removeAt(fromIndex)
            _tiles.add(targetIndex.coerceIn(0, _tiles.size), cell)
        } else if (dragType == DragType.Add) {

            _tiles.add(targetIndex.coerceIn(0, _tiles.size), dragged)
        }
    }

    fun onMoved(offset: Offset) {
        draggedPosition = offset
    }

    fun movedOutOfBounds() {
        val dragged = draggedTile ?: return

        if (dragType == DragType.Add) {

            _tiles.removeAll { it.spec == dragged.spec }
        }
        draggedPosition = Offset.Unspecified
    }

    fun onDrop(): List<String> {
        val result = tileSpecs()
        draggedTile = null
        draggedPosition = Offset.Unspecified
        dragType = null
        return result
    }

    fun onCancelled(originalTiles: List<QSTileData>) {
        val dragged = draggedTile
        if (dragged != null && dragType == DragType.Add) {

            _tiles.removeAll { it.spec == dragged.spec }
        }
        draggedTile = null
        draggedPosition = Offset.Unspecified
        dragType = null
    }

    fun tileSpecs(): List<String> = _tiles.map { it.spec }

    fun addTile(tile: QSTileData) {
        if (_tiles.none { it.spec == tile.spec }) {
            _tiles.add(tile)
        }
    }

    fun removeTile(spec: String): QSTileData? {
        val index = _tiles.indexOfFirst { it.spec == spec }
        return if (index != -1) _tiles.removeAt(index) else null
    }

    fun updateTiles(newTiles: List<QSTileData>) {
        _tiles.clear()
        _tiles.addAll(newTiles)
    }
}

private fun DragAndDropEvent.toOffset(): Offset {
    return toAndroidDragEvent().run { Offset(x, y) }
}

@Composable
fun Modifier.dragAndDropActiveList(
    gridState: LazyGridState,
    contentOffset: () -> Offset,
    dragDropState: QsTileDragDropState,
    onDrop: (List<String>) -> Unit,
): Modifier {
    val target =
        remember(dragDropState) {
            object : DragAndDropTarget {
                override fun onEntered(event: DragAndDropEvent) {}

                override fun onMoved(event: DragAndDropEvent) {
                    val offset = event.toOffset()
                    dragDropState.onMoved(offset)

                    val relativeOffset = offset - contentOffset()

                    val targetItem =
                        gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                            IntRect(item.offset, item.size).toRect().contains(relativeOffset)
                        }

                    targetItem?.let { dragDropState.onTargeting(it.index) }
                }

                override fun onExited(event: DragAndDropEvent) {
                    dragDropState.movedOutOfBounds()
                }

                override fun onDrop(event: DragAndDropEvent): Boolean {
                    val specs = dragDropState.onDrop()
                    onDrop(specs)
                    return true
                }

                override fun onEnded(event: DragAndDropEvent) {

                    if (dragDropState.dragInProgress) {
                        dragDropState.onDrop()
                    }
                }
            }
        }

    return dragAndDropTarget(
        shouldStartDragAndDrop = { event -> event.mimeTypes().contains("text/plain") },
        target = target,
    )
}

@Composable
fun Modifier.dragAndDropActiveGrid(
    contentOffset: () -> Offset,
    dragDropState: QsTileDragDropState,
    onDrop: (List<String>) -> Unit,
): Modifier {
    val target =
        remember(dragDropState) {
            object : DragAndDropTarget {
                override fun onEntered(event: DragAndDropEvent) {}

                override fun onMoved(event: DragAndDropEvent) {
                    val offset = event.toOffset()
                    dragDropState.onMoved(offset)

                    val relativeOffset = offset - contentOffset()
                    val targetItem = dragDropState.findItemAtOffset(relativeOffset)
                    targetItem?.let { dragDropState.onTargeting(it.index) }
                }

                override fun onExited(event: DragAndDropEvent) {
                    dragDropState.movedOutOfBounds()
                }

                override fun onDrop(event: DragAndDropEvent): Boolean {
                    val specs = dragDropState.onDrop()
                    onDrop(specs)
                    return true
                }

                override fun onEnded(event: DragAndDropEvent) {
                    if (dragDropState.dragInProgress) {
                        dragDropState.onDrop()
                    }
                }
            }
        }

    return dragAndDropTarget(
        shouldStartDragAndDrop = { event -> event.mimeTypes().contains("text/plain") },
        target = target,
    )
}

@Composable
fun Modifier.dragAndDropAvailableZone(
    dragDropState: QsTileDragDropState,
    onTileRemoved: (String) -> Unit,
): Modifier {
    val target =
        remember(dragDropState) {
            object : DragAndDropTarget {
                override fun onMoved(event: DragAndDropEvent) {
                    dragDropState.onMoved(event.toOffset())
                }

                override fun onDrop(event: DragAndDropEvent): Boolean {
                    val dragged = dragDropState.draggedTile ?: return false

                    if (dragDropState.dragType == DragType.Move) {
                        onTileRemoved(dragged.spec)
                    }
                    dragDropState.onDrop()
                    return true
                }
            }
        }

    return dragAndDropTarget(
        shouldStartDragAndDrop = { event -> event.mimeTypes().contains("text/plain") },
        target = target,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.dragAndDropTileSource(
    tileData: QSTileData,
    dragDropState: QsTileDragDropState,
    dragType: DragType = DragType.Move,
): Modifier {
    val state by rememberUpdatedState(dragDropState)

    return dragAndDropSource(
        block = {
            detectDragGesturesAfterLongPress(
                onDrag = { _, _ -> },
                onDragStart = {
                    state.onStarted(tileData, dragType)
                    startTransfer(
                        DragAndDropTransferData(ClipData.newPlainText("spec", tileData.spec))
                    )
                },
            )
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.dragAndDropAvailableTileSource(
    tileData: AvailableTileData,
    dragDropState: QsTileDragDropState,
): Modifier {
    val state by rememberUpdatedState(dragDropState)

    val qsTileData =
        remember(tileData) {
            QSTileData(
                spec = tileData.spec,
                label = tileData.label,
                secondaryLabel = "",
                state = 1,
                isTransient = false,
            )
        }

    return dragAndDropSource(
        block = {
            detectDragGesturesAfterLongPress(
                onDrag = { _, _ -> },
                onDragStart = {
                    state.onStarted(qsTileData, DragType.Add)
                    startTransfer(
                        DragAndDropTransferData(ClipData.newPlainText("spec", tileData.spec))
                    )
                },
            )
        }
    )
}
