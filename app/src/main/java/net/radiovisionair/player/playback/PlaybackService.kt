package net.radiovisionair.player.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerNotificationManager
import net.radiovisionair.player.MainActivity
import net.radiovisionair.player.R

class PlaybackService : Service() {

    companion object {
        const val STREAM_URL = "https://live.s1.radiovisionair.net:8443/?ver=710800"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "visionair_playback"
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    private lateinit var player: ExoPlayer
    private var notificationManager: PlayerNotificationManager? = null

    @Volatile
    private var lastNowPlaying: String? = null

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this).build().apply {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
            setAudioAttributes(audioAttributes, true)
            setHandleAudioBecomingNoisy(true)

            setMediaItem(MediaItem.fromUri(STREAM_URL))
            prepare()
        }

        setupNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Keep the service alive; controls are handled by binding.
        return START_STICKY
    }

    fun play() {
        if (!player.isPlaying) player.play()
    }

    fun pause() {
        if (player.isPlaying) player.pause()
    }

    fun toggle() {
        if (player.isPlaying) pause() else play()
    }

    fun isPlaying(): Boolean = player.isPlaying

    fun playbackState(): Int = player.playbackState

    fun addListener(listener: Player.Listener) {
        player.addListener(listener)
    }

    fun removeListener(listener: Player.Listener) {
        player.removeListener(listener)
    }

    fun setNowPlayingForNotification(title: String?) {
        lastNowPlaying = title
        notificationManager?.invalidate()
    }

    private fun setupNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Riproduzione VisiOnAir",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controlli riproduzione in background"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val descriptionAdapter = object : PlayerNotificationManager.MediaDescriptionAdapter {
            override fun getCurrentContentTitle(player: Player): CharSequence =
                getString(R.string.app_name)

            override fun createCurrentContentIntent(player: Player): PendingIntent? = contentIntent

            override fun getCurrentContentText(player: Player): CharSequence? =
                lastNowPlaying

            override fun getCurrentLargeIcon(
                player: Player,
                callback: PlayerNotificationManager.BitmapCallback
            ) = null
        }

        notificationManager = PlayerNotificationManager.Builder(this, NOTIFICATION_ID, CHANNEL_ID)
            .setMediaDescriptionAdapter(descriptionAdapter)
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean
                ) {
                    if (ongoing) {
                        startForeground(notificationId, notification)
                    } else {
                        stopForeground(false)
                    }
                }

                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    stopForeground(true)
                    stopSelf()
                }
            })
            .build().apply {
                setUseNextAction(false)
                setUsePreviousAction(false)
                setUseFastForwardAction(false)
                setUseRewindAction(false)
                setUseStopAction(false)
                setSmallIcon(R.drawable.ic_radio)
                setPlayer(player)
            }
    }

    override fun onDestroy() {
        notificationManager?.setPlayer(null)
        notificationManager = null
        player.release()
        stopForeground(true)
        super.onDestroy()
    }
}
