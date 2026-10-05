package com.example.data.extensions.media

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Audio played for extensions holding `background` (`OrthoTips.media`, bridge v5).
 *
 * The single source of truth: the extension page, the media notification, the
 * lock screen and Bluetooth headsets are all controllers of this one player,
 * and every change is pushed back to the page. Media3 posts the notification
 * and keeps the service in the foreground while it plays, so playback goes on
 * with the screen off.
 */
class ExtensionMediaService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                /* handleAudioFocus = */ true          // pauses for calls, lowers for other apps
            )
            .setHandleAudioBecomingNoisy(true)         // headphones unplugged → pause
            .setWakeMode(C.WAKE_MODE_NETWORK)          // screen off: keep CPU and Wi-Fi for the stream
            .build()
        session = MediaSession.Builder(this, player)
            .setSessionActivity(openAppIntent())       // tap on the notification → the app
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** Swiped away from recents: a playing stream keeps playing; otherwise the service stops. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    private fun openAppIntent(): PendingIntent {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: Intent()
        return PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
