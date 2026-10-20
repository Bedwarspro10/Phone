package org.fossify.phone.hyperos

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Small Folme-independent implementation of the damped-harmonic spring used by
 * the decompiled MIUIX SpringEasing/DampedHarmonicMotion sources.
 *
 * Folme's SpringEasing maps (damping, responseSeconds) to zeta and omega=2*pi/response.
 * This implementation preserves those parameters and evaluates the closed-form motion.
 * It intentionally uses Android property setters only; no Xiaomi APIs are referenced.
 */
internal object HyperMotionEngine {
    data class Profile(val damping: Double, val responseSeconds: Double) {
        init {
            require(damping >= 0.0)
            require(responseSeconds > 0.0)
        }
    }

    // Values confirmed in FolmeTouch's constructor in the supplied decompiled source.
    val TOUCH_DOWN = Profile(damping = 0.99, responseSeconds = 0.15)
    val TOUCH_UP = Profile(damping = 0.99, responseSeconds = 0.30)
    val SPECIAL_ALPHA = Profile(damping = 0.90, responseSeconds = 0.20)

    private val running = java.util.WeakHashMap<View, MutableMap<String, ValueAnimator>>()

    fun scale(view: View, target: Float, profile: Profile) {
        animate(view, "scaleX", view.scaleX.toDouble(), target.toDouble(), profile) { view.scaleX = it.toFloat() }
        animate(view, "scaleY", view.scaleY.toDouble(), target.toDouble(), profile) { view.scaleY = it.toFloat() }
    }

    fun alpha(view: View, target: Float, profile: Profile) {
        animate(view, "alpha", view.alpha.toDouble(), target.toDouble(), profile) { view.alpha = it.toFloat() }
    }

    private fun animate(
        view: View,
        property: String,
        start: Double,
        target: Double,
        profile: Profile,
        setter: (Double) -> Unit
    ) {
        val byProperty = running.getOrPut(view) { mutableMapOf() }
        byProperty.remove(property)?.cancel()
        if (abs(target - start) < 0.0005) {
            setter(target)
            return
        }
        val spring = ClosedSpring(profile.damping, 2.0 * PI / profile.responseSeconds)
        // Evaluate the source-equivalent spring at normalized time. The 2 s cap is a
        // safety guard; normal termination occurs when the curve has settled near target.
        var wasCancelled = false
        lateinit var animator: ValueAnimator
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = (profile.responseSeconds * 4_000.0).toLong().coerceIn(180L, 900L)
            interpolator = LinearInterpolator()
            addUpdateListener { a ->
                val seconds = (a.animatedFraction.toDouble() * duration) / 1000.0
                val progress = spring.progress(seconds)
                setter(start + (target - start) * progress)
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: android.animation.Animator) {
                    wasCancelled = true
                }
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (byProperty[property] === animator) byProperty.remove(property)
                    if (!wasCancelled) setter(target)
                }
            })
        }
        byProperty[property] = animator
        animator.start()
    }

    /** Normalized closed-form Folme spring: x(0)=0, x'(0)=0, target=1. */
    private class ClosedSpring(private val zeta: Double, private val omega: Double) {
        fun progress(t: Double): Double {
            if (t <= 0.0) return 0.0
            val alpha = -zeta * omega
            val discriminant = (2.0 * zeta * omega).let { it * it - 4.0 * omega * omega }
            val displacement: Double = when {
                discriminant < -1e-8 -> {
                    val beta = sqrt(-discriminant) / 2.0
                    val c1 = -1.0
                    val c2 = alpha / beta
                    exp(alpha * t) * (c1 * kotlin.math.cos(beta * t) + c2 * kotlin.math.sin(beta * t))
                }
                abs(discriminant) <= 1e-8 -> {
                    val r = -zeta * omega
                    val c1 = -1.0
                    val c2 = r
                    (c1 + c2 * t) * exp(r * t)
                }
                else -> {
                    val root = sqrt(discriminant)
                    val p = 2.0 * zeta * omega
                    val r1 = (root - p) / 2.0
                    val r2 = (-root - p) / 2.0
                    val c1 = (0.0 - (-1.0 * r2)) / root
                    val c2 = -(0.0 - (-1.0 * r1)) / root
                    c1 * exp(r1 * t) + c2 * exp(r2 * t)
                }
            }
            return 1.0 + displacement
        }
    }
}
