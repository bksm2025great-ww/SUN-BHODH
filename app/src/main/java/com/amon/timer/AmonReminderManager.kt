package com.amon.timer

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Calendar
import kotlin.random.Random

// =============================================================================
// 📬 1. डाकिया (RECEIVER): जो समय होने पर रैंडम मैसेज ऊपर स्क्रीन पर दिखाएगा
// =============================================================================
class AmonReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderType = intent.getStringExtra("REMINDER_TYPE") ?: return

        // 🎲 चारों पहर के लिए रैंडम मैसेज पूल
        val (title, message) = when (reminderType) {
            "MORNING" -> {
                val morningPool = listOf(
                    Pair(
                        "🌅 Rise & Focus",
                        "Small steps every morning build extraordinary things. Let’s begin."
                    ),
                    Pair(
                        "☀️ Own the Morning",
                        "A fresh day, a quiet mind. Start your first session and set the tone."
                    )
                )
                morningPool.random()
            }
            "AFTERNOON" -> {
                val afternoonPool = listOf(
                    Pair(
                        "Your study timer misses you 👀📚",
                        "A quick 20-minute focus block can completely reset your day. Step in!"
                    ),
                    Pair(
                        "Your study timer misses you 👀📚",
                        "Pause the noise, find your flow. Your quiet study space is waiting."
                    ),
                    Pair(
                        "Your study timer misses you 👀📚",
                        "Momentum is built in the middle of the day. One session changes everything."
                    )
                )
                afternoonPool.random()
            }
            "EVENING" -> {
                val eveningPool = listOf(
                    Pair(
                        "🔥 One More Session",
                        "Fall in love with the process and results will follow. Protect your study streak!"
                    ),
                    Pair(
                        "⚡ Protect Your Streak",
                        "Great days are built in the evening hours. Show up for yourself tonight."
                    )
                )
                eveningPool.random()
            }
            "NIGHT" -> {
                val nightPool = listOf(
                    Pair(
                        "🌙 Calm & Fulfilled",
                        "Close your books with pride. Let tonight feel restful, peaceful, and well-earned."
                    ),
                    Pair(
                        "✨ Finish the Day Strong",
                        "Your goals are one session closer. Wrap up your study with pure satisfaction."
                    )
                )
                nightPool.random()
            }
            else -> return
        }

        // नोटिफिकेशन दिखाना
        AmonReminderManager.showNotification(context, title, message)

        // अगले दिन के लिए सुरक्षित रूप से दोबारा शेड्यूल करना
        AmonReminderManager.scheduleAllReminders(context)
    }
}

// =============================================================================
// ⏰ 2. मुंशी (MANAGER): सुरक्षित और नेचुरल टाइमिंग पर रिमाइंडर लगाने वाला सिस्टम
// =============================================================================
object AmonReminderManager {
    private const val CHANNEL_ID = "amon_daily_reminders"
    private const val CHANNEL_NAME = "Daily Study & Focus Reminders"

    // चारों पहर के रिमाइंडर्स (प्राकृतिक और रैंडम समय के साथ)
    fun scheduleAllReminders(context: Context) {
        try {
            // 🌅 सुबह: लगभग 6:00 से 6:40 AM के बीच कभी भी
            val morningMinute = Random.nextInt(10, 45)
            scheduleSafeReminder(context, 6, morningMinute, "MORNING", 101)

            // ⏳ दोपहर: लगभग 1:15 से 2:00 PM के बीच कभी भी
            val afternoonMinute = Random.nextInt(15, 50)
            scheduleSafeReminder(context, 13, afternoonMinute, "AFTERNOON", 102)

            // 🔥 शाम: लगभग 5:10 से 5:45 PM के बीच कभी भी
            val eveningMinute = Random.nextInt(10, 45)
            scheduleSafeReminder(context, 17, eveningMinute, "EVENING", 103)

            // 🌙 रात: लगभग 9:15 से 9:50 PM के बीच कभी भी
            val nightMinute = Random.nextInt(15, 50)
            scheduleSafeReminder(context, 21, nightMinute, "NIGHT", 104)
        } catch (e: Exception) {
            Log.e("AmonReminder", "Safe schedule error: ${e.localizedMessage}")
        }
    }

    private fun scheduleSafeReminder(
        context: Context,
        baseHour: Int,
        minute: Int,
        type: String,
        requestCode: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, AmonReminderReceiver::class.java).apply {
            putExtra("REMINDER_TYPE", type)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, baseHour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // अगर यह समय आज बीत चुका है, तो कल के लिए सेट करें
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // 🛡️ सुरक्षित इन-एग्जैक्ट सिस्टम: यह किसी भी फोन पर बिना परमिशन के काम करता है और कभी क्रैश नहीं होता
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e("AmonReminder", "Failed to set alarm safely: ${e.localizedMessage}")
        }
    }

    // नोटिफिकेशन ट्रे में मैसेज दिखाना
    fun showNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android 8+ के लिए नोटिफिकेशन चैनल
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily discipline and reading reminders for Amon Timer"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // नोटिफिकेशन पर क्लिक करने पर Amon Timer ऐप खुले
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mascot_amon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}
