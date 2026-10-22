package nl.cita.magister.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import nl.cita.magister.MainActivity
import nl.cita.magister.R
import nl.cita.magister.notify.GradePing
import nl.cita.magister.notify.SchedulePing

object MagsNotifier {
    const val CHANNEL_ID = "mags-updates"
    private const val GRADE_ID = 4101
    private const val SCHEDULE_ID = 4102

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Cijfers en rooster",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Nieuwe cijfers en roosterwijzigingen" },
        )
    }

    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun grades(context: Context, pings: List<GradePing>) {
        if (pings.isEmpty() || !canNotify(context)) return
        ensureChannel(context)
        val title = if (pings.size == 1) "Nieuw cijfer" else "${pings.size} nieuwe cijfers"
        val body = pings.take(3).joinToString(" · ") { ping ->
            listOfNotNull(ping.subject, ping.value).joinToString(" ")
        }
        post(context, GRADE_ID, title, body)
    }

    fun schedule(context: Context, pings: List<SchedulePing>) {
        if (pings.isEmpty() || !canNotify(context)) return
        ensureChannel(context)
        val title = "Rooster gewijzigd"
        val body = pings.take(3).joinToString(" · ") { ping ->
            val kind = when (ping.kind) {
                SchedulePing.Kind.Cancelled -> "uitval"
                SchedulePing.Kind.Changed -> "gewijzigd"
                SchedulePing.Kind.New -> "nieuw"
            }
            "${ping.title} ($kind)"
        }
        post(context, SCHEDULE_ID, title, body)
    }

    private fun post(context: Context, id: Int, title: String, body: String) {
        val launch = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(launch)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
