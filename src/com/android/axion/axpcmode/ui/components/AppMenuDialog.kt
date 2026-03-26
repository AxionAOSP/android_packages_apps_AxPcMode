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

import android.view.Display
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.android.axion.axpcmode.utils.AppInfo
import com.android.axion.axpcmode.utils.AppUtils

@Composable
fun AppMenuDialog(app: AppInfo, onDismiss: () -> Unit, onUnpin: () -> Unit, displayId: Int = Display.DEFAULT_DISPLAY) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            colors =
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = BitmapPainter(app.icon.toBitmap().asImageBitmap()),
                        contentDescription = app.label,
                        modifier = Modifier.size(48.dp),
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(text = app.label, style = MaterialTheme.typography.titleMedium)
                }

                HorizontalDivider()

                Column(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = {
                            AppUtils.launchAppInfo(context, app.packageName, displayId)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("App Info")
                    }

                    TextButton(onClick = { onUnpin() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Unpin from Desktop")
                    }
                }

                HorizontalDivider()

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text("Cancel")
                }
            }
        }
    }
}
