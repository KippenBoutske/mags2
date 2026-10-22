package nl.cita.magister.live

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import nl.cita.magister.MainActivity
import nl.cita.magister.R
import nl.cita.magister.work.MagsNotifier

object LiveLessonNotifier {
    const val CHANNEL_ID = "mags-live-lesson"
    const val NOTIF_ID = 4201
    private const val REQ_TAP = 4202
    private const val REQ_HIDE = 4203

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Volgende les",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Huidige of volgende les op schooldagen" },
        )
    }

    fun post(context: Context, snap: LiveSnapshot) {
        if (!MagsNotifier.canNotify(context)) {
            cancel(context)
            return
        }
        ensureChannel(context)
        val tap = PendingIntent.getActivity(
            context,
            REQ_TAP,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_PAGE, "rooster")
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val hide = PendingIntent.getBroadcast(
            context,
            REQ_HIDE,
            Intent(context, LiveLessonReceiver::class.java).setAction(LiveLessonReceiver.ACTION_HIDE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_live)
            .setContentTitle(snap.title)
            .setContentText(snap.text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(tap)
            .setShowWhen(true)
            .setWhen(snap.whenMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(snap.countdown)
            .setShortCriticalText(snap.chip)
            .addAction(0, "Verbergen voor vandaag", hide)
            .setStyle(progressStyle(snap) ?: NotificationCompat.BigTextStyle().bigText(snap.text))

        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 36 && manager != null && manager.canPostPromotedNotifications()) {
            builder.setRequestPromotedOngoing(true)
        }
        NotificationManagerCompat.from(context).notify(NOTIF_ID, builder.build())
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID)
    }

    private fun progressStyle(snap: LiveSnapshot): NotificationCompat.Style? {
        if (snap.segments.isEmpty()) return null
        return runCatching {
            val style = NotificationCompat.ProgressStyle()
                .setProgress(snap.progress)
                .setStyledByProgress(true)
            snap.segments.forEach { segment ->
                style.addProgressSegment(
                    NotificationCompat.ProgressStyle.Segment(segment.lengthMinutes)
                        .setColor(segment.color),
                )
            }
            style
        }.getOrNull()
    }
}
