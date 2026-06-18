package com.example.dacs3.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.dacs3.viewmodel.TimerManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*

class TimerService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var timerJob: Job? = null
    private val CHANNEL_ID = "TimerChannel"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        when (intent?.action) {
            "TOGGLE" -> if (TimerManager.isRunning.value) pauseTimer() else startTimer()
            "SKIP" -> skipTimer()
        }
        return START_NOT_STICKY
    }

    private fun startTimer() {
        TimerManager.isRunning.value = true
        startForeground(1, buildNotif()) // Hiện thông báo dính chặt trên thanh trạng thái

        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (TimerManager.timeLeft.value > 0 && TimerManager.isRunning.value) {
                delay(1000)
                TimerManager.timeLeft.value -= 1
                updateNotif() // Cập nhật số giây trên thông báo
            }
            if (TimerManager.timeLeft.value == 0) {
                finishCycle()
            }
        }
    }

    private fun pauseTimer() {
        TimerManager.isRunning.value = false
        timerJob?.cancel()
        updateNotif()
    }

    private fun skipTimer() {
        TimerManager.timeLeft.value = 0
        pauseTimer()
        serviceScope.launch { finishCycle() }
    }

    private fun finishCycle() {
        val isFocus = TimerManager.isFocusMode.value

        val intent = Intent("com.example.dacs3.TIMER_FINISHED")
        intent.setPackage(packageName) // Yêu cầu Android gửi đích danh nội bộ app, chống bị chặn
        sendBroadcast(intent)
        // ---------------------------------------------

        // 1. LƯU FIREBASE NGẦM (Chỉ lưu khi là chế độ Focus)
        if (isFocus) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val db = FirebaseFirestore.getInstance()
                val userRef = db.collection("users").document(uid)

                val focusMinutes = TimerManager.currentFocusMin.value

                // A. Lưu vào bảng lịch sử (history)
                val historyData = hashMapOf(
                    "id" to System.currentTimeMillis().toString(),
                    "presetName" to TimerManager.currentPresetTitle.value,
                    "durationMinutes" to focusMinutes,
                    "timestamp" to System.currentTimeMillis()
                )
                userRef.collection("history").document(historyData["id"] as String).set(historyData)

                // B. Tính toán Streak và Tổng giờ trên bảng Users
                userRef.get().addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val lastDate = doc.getString("lastFocusDate") ?: ""
                        var currentStreak = doc.getLong("currentStreak")?.toInt() ?: 0
                        val totalMin = doc.getLong("totalFocusMinutes")?.toInt() ?: 0

                        // Lấy ngày hôm nay và ngày hôm qua theo định dạng chuẩn
                        val cal = java.util.Calendar.getInstance()
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        val todayStr = sdf.format(cal.time)

                        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                        val yesterdayStr = sdf.format(cal.time)

                        // Cập nhật logic Streak
                        if (lastDate == todayStr) {
                            // Đã học ca nào đó trong hôm nay rồi -> Streak giữ nguyên
                        } else if (lastDate == yesterdayStr) {
                            // Hôm qua có học, hôm nay học tiếp -> Nối chuỗi
                            currentStreak += 1
                        } else {
                            // Nghỉ quá 1 ngày (gãy chuỗi) hoặc mới học lần đầu -> Bắt đầu chuỗi mới
                            currentStreak = 1
                        }

                        // Cập nhật lại vào DB
                        userRef.update(
                            mapOf(
                                "lastFocusDate" to todayStr,
                                "streak" to currentStreak,
                                "totalFocusMinutes" to (totalMin + focusMinutes)
                            )
                        )
                    }
                }
            }
        }

        // 2. ĐỔI CHẾ ĐỘ & SET LẠI GIỜ
        TimerManager.isFocusMode.value = !isFocus
        val nextMin = if (TimerManager.isFocusMode.value) TimerManager.currentFocusMin.value else TimerManager.currentBreakMin.value
        TimerManager.totalFocusSeconds.value = nextMin * 60
        TimerManager.timeLeft.value = nextMin * 60

        // 3. BẮN THÔNG BÁO RUNG MÁY (Push notification của hệ thống)
        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val alertNotif = NotificationCompat.Builder(this@TimerService, CHANNEL_ID)
            .setContentTitle(if (isFocus) "Hết giờ tập trung!" else "Hết giờ nghỉ!")
            .setContentText("Quay lại app để tiếp tục.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()
        notifManager.notify(2, alertNotif)
        startTimer()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Timer Background", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotif(): Notification {
        val min = TimerManager.timeLeft.value / 60
        val sec = TimerManager.timeLeft.value % 60
        val mode = if (TimerManager.isFocusMode.value) "Focus" else "Break"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mindful Flow - $mode")
            .setContentText(String.format("Còn lại: %02d:%02d", min, sec))
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotif() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, buildNotif())
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
    }
}