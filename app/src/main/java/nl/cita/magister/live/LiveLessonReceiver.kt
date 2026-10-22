package nl.cita.magister.live

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LiveLessonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (intent?.action) {
                    ACTION_HIDE -> LiveLessonScheduler.hideToday(context)
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    -> LiveLessonScheduler.tick(context, refreshIfNeeded = false)
                    else -> LiveLessonScheduler.tick(context, refreshIfNeeded = true)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_TICK = "nl.cita.magister.live.TICK"
        const val ACTION_HIDE = "nl.cita.magister.live.HIDE"
    }
}
