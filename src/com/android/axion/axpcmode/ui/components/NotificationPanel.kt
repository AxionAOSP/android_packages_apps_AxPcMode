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

package com.android.axion.axpcmode.ui.components

import android.app.Notification
import android.app.PendingIntent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.android.axion.axpcmode.services.NotificationGroup
import com.android.axion.axpcmode.services.NotificationRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val safeMaxHeight = (configuration.screenHeightDp - 80).dp

    val groups =
        remember { derivedStateOf { NotificationRepository.getGroupedNotifications() } }.value

    Surface(
        modifier =
            modifier
                .width(360.dp)
                .heightIn(max = safeMaxHeight)
                .padding(top = 12.dp, bottom = 12.dp, end = 12.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                val clearInteractionSource = remember { MutableInteractionSource() }
                val clearPressed by clearInteractionSource.collectIsPressedAsState()
                val clearScale by
                    animateFloatAsState(
                        if (clearPressed) 0.9f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "clear_scale",
                    )

                Box(
                    modifier =
                        Modifier.size(32.dp)
                            .graphicsLayer {
                                scaleX = clearScale
                                scaleY = clearScale
                            }
                            .clickable(
                                interactionSource = clearInteractionSource,
                                indication = null,
                            ) {
                                NotificationRepository.clearAll()
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear All",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            if (groups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No notifications",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = groups, key = { it.groupKey }) { group ->
                        NotificationGroupItem(group = group, modifier = Modifier.animateItem())
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationGroupItem(group: NotificationGroup, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(group.children.size <= 2) }

    val hasClearableNotifications =
        remember(group.children) { group.children.any { it.isClearable } }

    val appInfo =
        remember(group.packageName) {
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(group.packageName, 0)
                Pair(
                    pm.getApplicationLabel(appInfo).toString(),
                    pm.getApplicationIcon(appInfo).toBitmap().asImageBitmap(),
                )
            } catch (e: Exception) {
                Pair(group.packageName, null)
            }
        }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
        modifier =
            modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        )
                ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (group.children.size > 1) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                            .padding(bottom = 8.dp),
                ) {
                    if (appInfo.second != null) {
                        Image(
                            bitmap = appInfo.second!!,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp).clip(CircleShape),
                        )
                    } else {
                        Box(
                            modifier =
                                Modifier.size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = appInfo.first,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )

                    Text(
                        text = "${group.children.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.width(4.dp))

                    Icon(
                        imageVector =
                            if (isExpanded) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )

                    if (hasClearableNotifications) {
                        Spacer(Modifier.width(4.dp))

                        val dismissInteractionSource = remember { MutableInteractionSource() }
                        val dismissPressed by dismissInteractionSource.collectIsPressedAsState()
                        val dismissScale by
                            animateFloatAsState(
                                targetValue = if (dismissPressed) 0.8f else 1f,
                                animationSpec =
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "dismiss_scale",
                            )

                        Box(
                            modifier =
                                Modifier.size(20.dp)
                                    .graphicsLayer {
                                        scaleX = dismissScale
                                        scaleY = dismissScale
                                    }
                                    .clickable(
                                        interactionSource = dismissInteractionSource,
                                        indication = null,
                                    ) {
                                        NotificationRepository.dismissGroup(group.groupKey)
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Close,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }

            AnimatedVisibility(
                visible = isExpanded || group.children.size == 1,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    group.children.forEach { sbn ->
                        NotificationChildItem(sbn = sbn, showAppIcon = group.children.size == 1)
                    }
                }
            }

            if (!isExpanded && group.children.size > 1) {
                val latestNotif = group.children.first()
                val title =
                    latestNotif.notification.extras.getString(Notification.EXTRA_TITLE)
                        ?: ""
                Text(
                    text = "$title and ${group.children.size - 1} more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationChildItem(
    sbn: StatusBarNotification,
    showAppIcon: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val extras = sbn.notification.extras
    val title = extras.getString(Notification.EXTRA_TITLE) ?: "Notification"
    val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
    val timestamp =
        remember(sbn.postTime) {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(sbn.postTime))
        }

    val isClearable = remember(sbn) { sbn.isClearable }

    val appIcon =
        remember(sbn) {
            if (!showAppIcon) return@remember null
            try {
                val drawable = sbn.notification.smallIcon.loadDrawable(context)
                drawable?.toBitmap()?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }

    val largeIcon =
        remember(sbn) {
            try {
                val icon =
                    extras.getParcelable(
                        Notification.EXTRA_LARGE_ICON,
                        Bitmap::class.java,
                    )
                        ?: extras
                            .getParcelable(
                                Notification.EXTRA_LARGE_ICON,
                                Icon::class.java,
                            )
                            ?.loadDrawable(context)
                            ?.toBitmap()
                icon?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by
        animateFloatAsState(
            targetValue = if (isPressed) 0.98f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "scale",
        )

    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { dismissValue ->
                if (
                    isClearable &&
                        (dismissValue == SwipeToDismissBoxValue.EndToStart ||
                            dismissValue == SwipeToDismissBoxValue.StartToEnd)
                ) {
                    NotificationRepository.dismiss(sbn)
                    true
                } else {
                    false
                }
            }
        )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            if (isClearable) {
                val backgroundColor by
                    animateColorAsState(
                        targetValue =
                            when (dismissState.targetValue) {
                                SwipeToDismissBoxValue.EndToStart,
                                SwipeToDismissBoxValue.StartToEnd ->
                                    MaterialTheme.colorScheme.errorContainer
                                else -> Color.Transparent
                            },
                        label = "swipe_bg_color",
                    )
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .background(backgroundColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp),
                    contentAlignment =
                        if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                            Alignment.CenterStart
                        else Alignment.CenterEnd,
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        },
        enableDismissFromStartToEnd = isClearable,
        enableDismissFromEndToStart = isClearable,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier =
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceBright, RoundedCornerShape(8.dp))
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(interactionSource = interactionSource, indication = null) {
                        try {
                            sbn.notification.contentIntent?.send()
                        } catch (e: PendingIntent.CanceledException) {
                            Log.w("NotificationPanel", "Content intent canceled", e)
                        } catch (e: Exception) {
                            Log.e("NotificationPanel", "Failed to open notification", e)
                        }
                    }
                    .padding(4.dp),
        ) {
            if (showAppIcon && appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                    modifier =
                        Modifier.size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(4.dp),
                )
                Spacer(Modifier.width(12.dp))
            } else if (showAppIcon) {
                Box(
                    modifier =
                        Modifier.size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = " • $timestamp",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }

                if (text.isNotEmpty()) {
                    Text(
                        text = text,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                val progress = extras.getInt(Notification.EXTRA_PROGRESS, -1)
                val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
                val progressIndeterminate =
                    extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)

                if (progressIndeterminate) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        modifier =
                            Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                } else if (progress >= 0 && progressMax > 0) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress.toFloat() / progressMax.toFloat() },
                        modifier =
                            Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }

            if (largeIcon != null) {
                Spacer(Modifier.width(8.dp))
                Image(
                    bitmap = largeIcon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop,
                )
            }

            if (isClearable) {
                Spacer(Modifier.width(8.dp))

                val dismissInteractionSource = remember { MutableInteractionSource() }
                val dismissPressed by dismissInteractionSource.collectIsPressedAsState()
                val dismissScale by
                    animateFloatAsState(
                        targetValue = if (dismissPressed) 0.8f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "dismiss_scale",
                    )

                Box(
                    modifier =
                        Modifier.size(20.dp)
                            .graphicsLayer {
                                scaleX = dismissScale
                                scaleY = dismissScale
                            }
                            .clickable(
                                interactionSource = dismissInteractionSource,
                                indication = null,
                                onClick = { NotificationRepository.dismiss(sbn) },
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Close,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}
