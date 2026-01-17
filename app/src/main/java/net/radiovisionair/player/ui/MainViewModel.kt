package net.radiovisionair.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.radiovisionair.player.schedule.ProgrammazioneRepository
import net.radiovisionair.player.schedule.ShowInfo
import java.time.Duration
import java.time.Instant

class MainViewModel(
    private val repo: ProgrammazioneRepository = ProgrammazioneRepository()
) : ViewModel() {

    @Volatile
    var latest: ShowInfo? = null
        private set

    private var job: Job? = null

    fun startRefreshing(onUpdate: (ShowInfo) -> Unit) {
        if (job != null) return
        job = viewModelScope.launch {
            while (true) {
                val info = repo.nowPlaying()
                latest = info
                onUpdate(info)

                val now = Instant.now()
                val delayMs = info.validUntil
                    ?.let { Duration.between(now, it).toMillis() }
                    ?.coerceIn(60_000L, 30 * 60_000L) // 1..30 min
                    ?: 5 * 60_000L

                delay(delayMs)
            }
        }
    }

    override fun onCleared() {
        job?.cancel()
        job = null
        super.onCleared()
    }
}
