package com.android.axion.axpcmode.services

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.flow.*

data class MediaSessionData(
    val title: String,
    val artist: String,
    val packageName: String,
    val isPlaying: Boolean,
    val albumArt: Bitmap? = null,
    val primaryColor: Color? = null,
)

class MediaRepository(private val context: Context) {
    private val _activeSession = MutableStateFlow<MediaSessionData?>(null)
    val activeSession: StateFlow<MediaSessionData?> = _activeSession.asStateFlow()

    private var currentController: MediaController? = null

    private val callback =
        object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                updateSessionData()
            }

            override fun onMetadataChanged(metadata: MediaMetadata?) {
                updateSessionData()
            }

            override fun onSessionDestroyed() {
                currentController = null
                _activeSession.value = null
            }
        }

    fun onMediaTokensUpdated(tokens: List<MediaSession.Token>) {

        val controllers =
            try {
                tokens.mapNotNull { token ->
                    try {
                        MediaController(context, token)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to create MediaController for token", e)
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create MediaControllers", e)
                emptyList()
            }

        val playingController =
            controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: controllers.firstOrNull()

        if (playingController?.sessionToken != currentController?.sessionToken) {
            try {
                currentController?.unregisterCallback(callback)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister callback", e)
            }
            currentController = playingController
            try {
                currentController?.registerCallback(callback)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register callback", e)
                currentController = null
            }
            updateSessionData()
        } else if (playingController == null) {
            try {
                currentController?.unregisterCallback(callback)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister callback on cleanup", e)
            }
            currentController = null
            _activeSession.value = null
        } else {

            updateSessionData()
        }
    }

    private fun updateSessionData() {
        val controller = currentController
        if (controller == null) {
            _activeSession.value = null
            return
        }

        val metadata = controller.metadata
        val playbackState = controller.playbackState

        val bitmap =
            metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)

        val color =
            bitmap?.let {
                val palette = Palette.from(it).generate()
                val vibrant = palette.getVibrantColor(0)
                if (vibrant != 0) Color(vibrant)
                else {
                    val muted = palette.getMutedColor(0)
                    if (muted != 0) Color(muted) else null
                }
            }

        _activeSession.value =
            MediaSessionData(
                title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Title",
                artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist",
                packageName = controller.packageName,
                isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING,
                albumArt = bitmap,
                primaryColor = color,
            )
    }

    fun togglePlayPause() {
        val pbState = currentController?.playbackState?.state
        if (pbState == PlaybackState.STATE_PLAYING) {
            currentController?.transportControls?.pause()
        } else {
            currentController?.transportControls?.play()
        }
    }

    fun skipToNext() {
        currentController?.transportControls?.skipToNext()
    }

    fun skipToPrevious() {
        currentController?.transportControls?.skipToPrevious()
    }

    fun onDestroy() {
        try {
            currentController?.unregisterCallback(callback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister callback in onDestroy", e)
        }
        currentController = null
        _activeSession.value = null
        Log.d(TAG, "MediaRepository destroyed")
    }

    companion object {
        private const val TAG = "MediaRepository"
    }
}
