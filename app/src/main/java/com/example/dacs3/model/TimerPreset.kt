package com.example.dacs3.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class TimerPreset(
    val id: Long = 0L,
    val title: String = "New Preset",
    val focusMin: Int = 25,
    val breakMin: Int = 5,
    val colorHex: String = "#3F51B5"
) {
    constructor() : this(0L, "New Preset", 25, 5, "#3F51B5")
}
//