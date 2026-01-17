package net.radiovisionair.player

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import net.radiovisionair.player.databinding.ActivityMainBinding
import net.radiovisionair.player.playback.PlaybackService
import net.radiovisionair.player.ui.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private var service: PlaybackService? = null
    private var bound: Boolean = false

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            renderPlayState(isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            binding.progress.isVisible = playbackState == Player.STATE_BUFFERING
        }

        override fun onPlayerError(error: PlaybackException) {
            binding.progress.isVisible = false
            binding.status.text = getString(R.string.status_error)
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val b = binder as? PlaybackService.LocalBinder ?: return
            service = b.getService()
            bound = true

            service?.addListener(playerListener)
            renderPlayState(service?.isPlaying() == true)
            binding.progress.isVisible = service?.playbackState() == Player.STATE_BUFFERING
            binding.status.text = getString(R.string.status_ready)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            service = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        maybeRequestNotificationsPermission()

        binding.playPause.setOnClickListener {
            ensureServiceStarted()
            service?.toggle()
        }

        // Fetch & refresh "programma in onda" dalla pagina Programmazione.
        viewModel.startRefreshing { info ->
            runOnUiThread {
                binding.nowTitle.text = info.title
                binding.nowDesc.text = info.description
                binding.source.text = getString(R.string.source_fmt, info.source)
                service?.setNowPlayingForNotification(info.title)
            }
        }

        ensureServiceStarted()
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, PlaybackService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        if (bound) {
            service?.removeListener(playerListener)
            unbindService(connection)
            bound = false
        }
        super.onStop()
    }

    private fun ensureServiceStarted() {
        // Start as a normal service (the app is in foreground); it will become foreground only when playing.
        try {
            startService(Intent(this, PlaybackService::class.java))
        } catch (_: Exception) {
            // Fallback for stricter environments.
            ContextCompat.startForegroundService(this, Intent(this, PlaybackService::class.java))
        }
    }

    private fun renderPlayState(isPlaying: Boolean) {
        binding.progress.isVisible = false
        if (isPlaying) {
            binding.playPause.setIconResource(R.drawable.ic_pause)
            binding.playPause.text = getString(R.string.pause)
            binding.status.text = getString(R.string.status_playing)
        } else {
            binding.playPause.setIconResource(R.drawable.ic_play)
            binding.playPause.text = getString(R.string.play)
            binding.status.text = getString(R.string.status_paused)
        }
    }

    private fun maybeRequestNotificationsPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        val perm = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) return
        ActivityCompat.requestPermissions(this, arrayOf(perm), 1001)
    }
}
