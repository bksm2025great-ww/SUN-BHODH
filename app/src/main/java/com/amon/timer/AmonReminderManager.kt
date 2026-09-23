package com.amon.timer

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

// =============================================================================
// 📬 1. डाकिया (RECEIVER): जो ठीक समय पर ऊपर स्टेटस बार में मैसेज दिखाएगा
// =============================================================================
class AmonReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderType = intent.getStringExtra("REMINDER_TYPE") ?: return

        val (title, message) = when (reminderType) {
            "MORNING" -> Pair(
                "🌅 भोर की पहली किरण, पहला पौधा!",
                "बॉस, आज का पहला फोकस सेशन शुरू करें? आपका फ़ॉरेस्ट और अमोन एक ताज़ातरीन शुरुआत के इंतज़ार में हैं। 🌱"
            )
            "DEEP_WORK" -> Pair(
                "⚡ 10:00 AM: डीप वर्क पावर मोड!",
                "दिन का सबसे ताज़ा दिमाग, सबसे बड़ा काम! 25 मिनट का एक सॉलिड सेशन, और आप आज बाकी दुनिया से मीलों आगे निकल जाएँगे। 🚀"
            )
            "EVENING" -> Pair(
                "🌇 04:30 PM: शाम की जंग, आलस पर जीत!",
                "थकान को अपने लक्ष्यों के बीच मत आने दीजिए बॉस। शाम का एक फोकस्ड सेशन आज की स्ट्रीक को पक्का कर देगा! 🔥"
            )
            "NIGHT" -> Pair(
                "📖 फोन को विश्राम, किताबों को सलाम!",
                "अब स्क्रीन को अलविदा कहने का वक्त है। किसी अच्छी किताब के पन्ने पलटें, दिमाग शांत रखें और एक गहरी नींद की ओर बढ़ें। 🌙"
            )
            else -> return
        }

        AmonReminderManager.showNotification(context, title, message)
        
        // अगले दिन के लिए अलार्म को दोबारा शेड्यूल करना
        AmonReminderManager.scheduleAllReminders(context)
    }
}

// =============================================================================
// ⏰ 2. मुंशी (MANAGER): जो चारों समय के अलार्म घड़ी में सेट करता है
// =============================================================================
object AmonReminderManager {
    private const val CHANNEL_ID = "amon_daily_reminders"
    private const val CHANNEL_NAME = "Daily Study & Focus Reminders"

    // चारों अलार्म सेट करना
    fun scheduleAllReminders(context: Context) {
        scheduleReminder(context, 5, 30, "MORNING", 101)
        scheduleReminder(context, 10, 0, "DEEP_WORK", 102)
        scheduleReminder(context, 16, 30, "EVENING", 103)
        scheduleReminder(context, 20, 0, "NIGHT", 104)
    }

    private fun scheduleReminder(
        context: Context,
        hour: Int,
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
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // अगर आज का यह समय पहले ही निकल चुका है, तो कल के लिए सेट करो
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // फोन को जगाकर तय समय पर अलार्म फायर करना
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    // नोटिफिकेशन बॉक्स बनाना और स्क्रीन पर दिखाना
    fun showNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // एंड्रॉइड 8+ के लिए नोटिफिकेशन चैनल
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

        // क्लिक करने पर Amon Timer ऐप खुले
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
