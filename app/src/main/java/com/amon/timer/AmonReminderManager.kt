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
// 📬 1. डाकिया (RECEIVER): स्मार्ट चेक्स के साथ केवल शांत वक्त पर संदेश पहुँचाएगा
// =============================================================================
class AmonReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderType = intent.getStringExtra("REMINDER_TYPE") ?: return

        // 🛡️ चेक 1: अगर यूजर अभी Amon में पढ़ाई कर रहा है, तो कोई नोटिफिकेशन नहीं जाएगा
        if (TimerService.isTimerRunning.value) {
            Log.d("AmonReminder", "Active study session running. Notification suppressed.")
            return
        }

        // 🛡️ चेक 2: रात 9:00 PM से सुबह 6:00 AM के बीच सख्त कर्फ्यू (पूर्ण सन्नाटा)
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (currentHour >= 21 || currentHour < 6) {
            Log.d("AmonReminder", "Curfew hours active (9 PM - 6 AM). Notification suppressed.")
            return
        }

        // 🎲 25+ गंभीर और प्रेरक संदेशों की लाइब्रेरी (पहरो के अनुसार)
        val (title, message) = when (reminderType) {
            "MORNING" -> {
                val morningPool = listOf(
                    Pair("🌅 Dawn of Discipline", "The best way to predict your day is to create it. Start your morning focus block."),
                    Pair("☀️ Morning Momentum", "Quiet morning hours compound into massive results. One deep session to start."),
                    Pair("🎯 Set the Tone", "Small wins early in the day build unbreakable confidence. Step in."),
                    Pair("🌄 Fresh Day, Clear Mind", "Clear away yesterday’s fatigue. Open Amon and commit to your first goal."),
                    Pair("⚡ The Early Advantage", "Discipline is choosing between what you want now and what you want most.")
                )
                morningPool.random()
            }
            "MIDDAY" -> {
                val middayPool = listOf(
                    Pair("🌤️ Midday Reset", "Take a deep breath and recalibrate your afternoon priorities."),
                    Pair("🎯 Lock In", "Distractions are loud, but focus is powerful. Give 25 minutes of honest work."),
                    Pair("📚 Maintain the Standard", "Great achievements are built on average afternoons handled with extraordinary discipline."),
                    Pair("⏳ Reclaim Your Time", "A short, uninterrupted focus session can completely rescue a drifting afternoon."),
                    Pair("💡 The Flow State", "Don’t count the hours—make the focused minutes count.")
                )
                middayPool.random()
            }
            "AFTERNOON" -> {
                val afternoonPool = listOf(
                    Pair("⚡ Power Through", "The afternoon slump disappears the moment you begin. Start your timer."),
                    Pair("🛡️ Guard Your Attention", "Your attention is your most valuable asset today. Invest it wisely."),
                    Pair("🔥 Build Consistency", "You don't need endless hours, just dedicated blocks of genuine deep focus."),
                    Pair("⏱️ Session in Sight", "Set your target subject and eliminate all background noise."),
                    Pair("📈 Compound Effort", "Every 25-minute block is an investment in your mastery.")
                )
                afternoonPool.random()
            }
            "EVENING" -> {
                val eveningPool = listOf(
                    Pair("🌆 Golden Hour Study", "The day isn't over yet. Put in the work that separates you from the crowd."),
                    Pair("🔥 Protect Your Streak", "Show up for yourself this evening. Keep your study streak alive."),
                    Pair("🏛️ Build the Habit", "Motivation gets you started; disciplined evening focus keeps you growing."),
                    Pair("🎯 Finish What You Started", "Close out today's study goals before the evening winds down."),
                    Pair("⚡ Unbroken Focus", "One clean evening session turns a good day into an exceptional one.")
                )
                eveningPool.random()
            }
            "NIGHT_WRAP" -> {
                val nightPool = listOf(
                    Pair("🌙 Final Sprint", "One final focused session to close the books with total satisfaction."),
                    Pair("✨ Wrap Up the Day", "Earn your rest tonight with a calm, deliberate review of your goals."),
                    Pair("🏆 Dignity of Effort", "Finish today with pride. Tomorrow builds on the discipline of tonight."),
                    Pair("📖 Final Chapter of the Day", "A quick evening session seals in everything you learned today."),
                    Pair("🌌 Close the Day Strong", "End your study routine with discipline, then rest without regret.")
                )
                nightPool.random()
            }
            else -> return
        }

        // नोटिफिकेशन दिखाना
        AmonReminderManager.showNotification(context, title, message)

        // अगले दिन के लिए लचीले समय पर दोबारा शेड्यूल करना
        AmonReminderManager.scheduleAllReminders(context)
    }
}

// =============================================================================
// ⏰ 2. मुंशी (MANAGER): दिन में ठीक 5 लचीले स्लॉट और 9 PM कर्फ्यू
// =============================================================================
object AmonReminderManager {
    private const val CHANNEL_ID = "amon_daily_reminders"
    private const val CHANNEL_NAME = "Daily Study & Focus Reminders"
    private const val SINGLE_NOTIFICATION_ID = 1001 // एकल ID: ट्रे में कभी नोटिफिकेशन्स का ढेर नहीं लगेगा

    fun scheduleAllReminders(context: Context) {
        try {
            // 1. 🌅 सुबह: 6:25 AM से 7:45 AM के बीच (6-8 AM विंडो में लचीला)
            val morningMinute = Random.nextInt(25, 55)
            scheduleSafeReminder(context, 6, morningMinute, "MORNING", 101)

            // 2. ☀️ दोपहर 1: 12:15 PM से 12:45 PM के बीच
            val middayMinute = Random.nextInt(15, 45)
            scheduleSafeReminder(context, 12, middayMinute, "MIDDAY", 102)

            // 3. 🌤️ दोपहर 2: 3:15 PM से 3:50 PM के बीच
            val afternoonMinute = Random.nextInt(15, 50)
            scheduleSafeReminder(context, 15, afternoonMinute, "AFTERNOON", 103)

            // 4. 🌆 शाम: 6:10 PM से 6:45 PM के बीच
            val eveningMinute = Random.nextInt(10, 45)
            scheduleSafeReminder(context, 18, eveningMinute, "EVENING", 104)

            // 5. 🌙 रात: 8:10 PM से 8:35 PM के बीच (सख्ती से 9:00 PM से पहले समाप्त)
            val nightMinute = Random.nextInt(10, 35)
            scheduleSafeReminder(context, 20, nightMinute, "NIGHT_WRAP", 105)
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

    fun showNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily discipline and focus reminders for Amon Timer"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

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

        // 🌟 सिंगल ID: नया नोटिफिकेशन आने पर पुराना रिप्लेस होगा, बार-बार ढेर नहीं लगेगा
        notificationManager.notify(SINGLE_NOTIFICATION_ID, notification)
    }
}
