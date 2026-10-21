package nl.cita.magister.crash

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import nl.cita.magister.MainActivity

/**
 * Framework Views only — no Compose, crypto, DataStore or Material3.
 * Draait in process `:crash` zodat een kapotte main-process dit scherm niet meeneemt.
 */
class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trace = intent.getStringExtra(CrashGuard.EXTRA_TRACE)
            ?: CrashGuard.readTrace(this)
            ?: "Geen stacktrace bewaard."

        val pad = dp(20)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, dp(28), pad, pad)
            setBackgroundColor(0xFF101828.toInt())
        }

        root.addView(
            TextView(this).apply {
                text = "Mags is gestopt"
                setTextColor(0xFFF4C36A.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                typeface = Typeface.DEFAULT_BOLD
            },
        )
        root.addView(
            TextView(this).apply {
                text = "De app crashte bij het starten of tijdens gebruik. " +
                    "Kopieer de stacktrace hieronder en stuur die door — dan is de volgende crash te diagnosticeren."
                setTextColor(0xFFF4F1EA.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(0, dp(10), 0, dp(12))
            },
        )

        val stack = TextView(this).apply {
            text = trace
            setTextIsSelectable(true)
            typeface = Typeface.MONOSPACE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(0xFFE8E4D9.toInt())
            setTextIsSelectable(true)
        }
        root.addView(
            ScrollView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f,
                )
                addView(stack)
            },
        )

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, 0)
        }
        buttons.addView(
            Button(this).apply {
                text = "Kopieer stacktrace"
                setOnClickListener {
                    val clipboard = getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("mags-crash", trace))
                    Toast.makeText(this@CrashActivity, "Stacktrace gekopieerd", Toast.LENGTH_SHORT).show()
                }
            },
        )
        buttons.addView(
            Button(this).apply {
                text = "Opnieuw starten"
                setOnClickListener { restartApp() }
            },
        )
        root.addView(buttons, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { gravity = Gravity.BOTTOM })

        setContentView(root)
    }

    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
