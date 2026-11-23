package nl.cita.magister

import android.view.View
import android.view.animation.Interpolator

internal fun splashIconOrNull(iconView: () -> View?): View? =
    runCatching { iconView() }.getOrNull()

internal fun animateSplashIcon(iconView: () -> View?, interpolator: Interpolator) {
    splashIconOrNull(iconView)?.animate()
        ?.scaleX(0.65f)
        ?.scaleY(0.65f)
        ?.rotation(32f)
        ?.alpha(0f)
        ?.setDuration(220)
        ?.setInterpolator(interpolator)
        ?.start()
}
