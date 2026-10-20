package org.fossify.phone

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.animation.PathInterpolator
import com.google.android.material.tabs.TabLayout
import org.fossify.phone.R
import kotlin.math.max

/**
 * A standalone animated selection pill for Fossify's existing TabLayout.
 * Keeps TabLayout's tab selection and accessibility behaviour intact.
 */
class SlidingPillTabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TabLayout(context, attrs, defStyleAttr) {
    private val density = resources.displayMetrics.density
    private val horizontalInset = 7f * density
    private val verticalInset = 5f * density
    private val radius = 22f * density
    private val pill = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = context.getColor(R.color.color_primary)
        alpha = 28
    }

    private var pillLeft = Float.NaN
    private var pillRight = Float.NaN
    private var pillTop = Float.NaN
    private var pillBottom = Float.NaN
    private var animator: ValueAnimator? = null
    private var firstLayout = true

    init {
        setSelectedTabIndicator(null as android.graphics.drawable.Drawable?)
        setSelectedTabIndicatorHeight(0)
        addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = movePillTo(tab.position, true)
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = movePillTo(tab.position, true)
        })
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (tabCount > 0 && (firstLayout || changed || pillLeft.isNaN())) {
            firstLayout = false
            movePillTo(selectedTabPosition.coerceAtLeast(0), false)
        }
    }

    private fun movePillTo(position: Int, animated: Boolean) {
        if (position !in 0 until tabCount) return
        val child = getChildAt(position) ?: return
        if (width == 0 || child.width == 0) return

        val targetLeft = child.left + horizontalInset
        val targetRight = child.right - horizontalInset
        val targetTop = verticalInset
        val targetBottom = height - verticalInset

        animator?.cancel()
        if (!animated || pillLeft.isNaN() || pillRight.isNaN()) {
            pillLeft = targetLeft.toFloat()
            pillRight = targetRight.toFloat()
            pillTop = targetTop
            pillBottom = targetBottom
            invalidate()
            return
        }

        val startLeft = pillLeft
        val startRight = pillRight
        val startTop = if (pillTop.isNaN()) targetTop else pillTop
        val startBottom = if (pillBottom.isNaN()) targetBottom else pillBottom
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 360L
            interpolator = PathInterpolator(0.22f, 1f, 0.36f, 1f)
            addUpdateListener { value ->
                val fraction = value.animatedValue as Float
                val leftFraction = smooth(fraction * 0.94f)
                val rightFraction = smooth(((fraction - 0.08f) / 0.92f).coerceIn(0f, 1f))
                pillLeft = startLeft + (targetLeft - startLeft) * leftFraction
                pillRight = startRight + (targetRight - startRight) * rightFraction
                pillTop = startTop + (targetTop - startTop) * fraction
                pillBottom = startBottom + (targetBottom - startBottom) * fraction
                invalidate()
            }
            start()
        }
    }

    private fun smooth(value: Float): Float {
        val fraction = value.coerceIn(0f, 1f)
        return fraction * fraction * (3f - 2f * fraction)
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (!pillLeft.isNaN() && !pillRight.isNaN() && !pillTop.isNaN() && !pillBottom.isNaN()) {
            pill.set(pillLeft, pillTop, max(pillLeft, pillRight), pillBottom)
            canvas.drawRoundRect(pill, radius, radius, paint)
        }
        super.dispatchDraw(canvas)
    }
}
