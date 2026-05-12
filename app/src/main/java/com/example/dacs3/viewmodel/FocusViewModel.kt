package com.example.dacs3.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dacs3.data.repository.HistoryRepository
import com.example.dacs3.data.repository.PresetRepository
import com.example.dacs3.data.repository.UserRepository
import com.example.dacs3.model.TimerPreset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FocusViewModel : ViewModel() {

    private val presetRepository = PresetRepository()
    private val historyRepository = HistoryRepository()
    private val userRepository = UserRepository()

    private var timerJob: Job? = null

    var userName by mutableStateOf("Loading...")
        private set

    var userTitle by mutableStateOf("Member")
        private set

    var totalDeepWorkHours by mutableIntStateOf(0)
        private set

    var presetsList by mutableStateOf<List<TimerPreset>>(emptyList())
        private set

    var currentPreset by mutableStateOf<TimerPreset?>(null)
        private set

    var isFocusMode by mutableStateOf(true)
        private set

    var totalFocusSeconds by mutableIntStateOf(25 * 60)
        private set

    var timeLeft by mutableIntStateOf(25 * 60)
        private set

    var isRunning by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        listenUser()
        listenPresets()
    }

    private fun listenUser() {
        userRepository.listenUserName(
            onResult = { name ->
                userName = name
            },
            onError = { message ->
                errorMessage = message
            }
        )
    }

    private fun listenPresets() {
        presetRepository.listenPresets(
            onResult = { presets ->
                presetsList = presets

                if (currentPreset == null && presets.isNotEmpty()) {
                    setPresetWithoutStart(presets.first())
                }
            },
            onError = { message ->
                errorMessage = message
            }
        )
    }

    fun toggleTimer() {
        if (isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        if (timerJob?.isActive == true) return

        isRunning = true

        timerJob = viewModelScope.launch {
            while (isRunning && timeLeft > 0) {
                delay(1000L)
                timeLeft--
            }

            if (isRunning && timeLeft == 0) {
                finishCurrentTimer()
            }
        }
    }

    private fun pauseTimer() {
        isRunning = false
        timerJob?.cancel()
        timerJob = null
    }

    private fun finishCurrentTimer() {
        timerJob?.cancel()
        timerJob = null

        if (isFocusMode) {
            val presetName = currentPreset?.title ?: "Focus Timer"
            val duration = currentPreset?.focusMin ?: 25

            historyRepository.saveFocusHistory(
                presetName = presetName,
                durationMinutes = duration,
                onError = { message ->
                    errorMessage = message
                }
            )
        }

        isFocusMode = !isFocusMode

        val nextMinutes = if (isFocusMode) {
            currentPreset?.focusMin ?: 25
        } else {
            currentPreset?.breakMin ?: 5
        }

        totalFocusSeconds = nextMinutes * 60
        timeLeft = totalFocusSeconds

        startTimer()
    }

    fun restartTimer() {
        pauseTimer()

        isFocusMode = true

        val minutes = currentPreset?.focusMin ?: 25

        totalFocusSeconds = minutes * 60
        timeLeft = totalFocusSeconds
    }

    fun skipTimer() {
        timeLeft = 0
        finishCurrentTimer()
    }

    fun activatePreset(preset: TimerPreset) {
        pauseTimer()

        currentPreset = preset
        isFocusMode = true
        totalFocusSeconds = preset.focusMin * 60
        timeLeft = totalFocusSeconds

        startTimer()
    }

    private fun setPresetWithoutStart(preset: TimerPreset) {
        currentPreset = preset
        isFocusMode = true
        totalFocusSeconds = preset.focusMin * 60
        timeLeft = totalFocusSeconds
    }

    fun savePreset(preset: TimerPreset) {
        presetRepository.savePreset(
            preset = preset,
            onError = { message ->
                errorMessage = message
            }
        )
    }

    fun deletePreset(id: Long) {
        presetRepository.deletePreset(
            presetId = id,
            onError = { message ->
                errorMessage = message
            }
        )
    }

    fun createCustomTimer(minutes: Int) {
        val safeMinutes = if (minutes <= 0) 25 else minutes

        val customPreset = TimerPreset(
            id = System.currentTimeMillis(),
            title = "Focus $safeMinutes m",
            focusMin = safeMinutes,
            breakMin = 5
        )

        savePreset(customPreset)
        setPresetWithoutStart(customPreset)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}