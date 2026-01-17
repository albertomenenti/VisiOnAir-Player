package net.radiovisionair.player.schedule

import java.time.Instant

data class ShowInfo(
    val title: String,
    val description: String,
    val source: String,
    val validUntil: Instant? = null
)
