package org.fossify.phone.hyperos

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import org.fossify.phone.R
import kotlin.math.max

/** Connects HyperMotion to a Fossify dialpad key without taking over click/DTMF handling. */
object HyperMotionDialpad {
    private const val BACKGROUND_TAG = "hypermotion_background_layer"
    private data class KeyTargets(val background: View, val letters: List<TextView>)

    /** Installs targets once; does not replace Fossify's touch/click listener. */
    fun install(holder: View) {
        if (holder.getTag(R.id.hypermotion_installed_tag) is KeyTargets) return
        val background = ensureIndependentBackground(holder)
        val letters = collectTextViews(holder).filter { isLetterView(it) }
        holder.setTag(R.id.hypermotion_installed_tag, KeyTargets(background, letters))
    }

    /** Call from the existing touch listener. Always returns control to Fossify. */
    fun onTouch(holder: View, event: android.view.MotionEvent) {
        install(holder)
        val targets = holder.getTag(R.id.hypermotion_installed_tag) as? KeyTargets ?: return
        when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                val downScale = automaticDownScale(holder)
                HyperMotionEngine.scale(targets.background, downScale, HyperMotionEngine.TOUCH_DOWN)
                HyperMotionEngine.alpha(targets.background, 1f, HyperMotionEngine.TOUCH_DOWN)
                targets.letters.forEach { HyperMotionEngine.alpha(it, 1f, HyperMotionEngine.TOUCH_DOWN) }
            }
            android.view.MotionEvent.ACTION_UP,
            android.view.MotionEvent.ACTION_CANCEL -> {
                HyperMotionEngine.scale(targets.background, 0.8f, HyperMotionEngine.TOUCH_UP)
                HyperMotionEngine.alpha(targets.background, 0f, HyperMotionEngine.SPECIAL_ALPHA)
                targets.letters.forEach { HyperMotionEngine.alpha(it, 0.6f, HyperMotionEngine.SPECIAL_ALPHA) }
            }
        }
    }

    private fun ensureIndependentBackground(holder: View): View {
        if (holder !is RelativeLayout) return holder
        val existing = holder.findViewWithTag<View>(BACKGROUND_TAG)
        if (existing != null) return existing

        val oldBackground = holder.background
        val oldAlpha = oldBackground?.alpha ?: 255
        val overlayDrawable: Drawable? = oldBackground?.constantState
            ?.newDrawable(holder.resources, holder.context.theme)?.mutate()
            ?: oldBackground?.constantState?.newDrawable()?.mutate()
        overlayDrawable?.alpha = oldAlpha

        // Keep the normal holder background visible at all times. The copied overlay
        // starts transparent and is only shown while pressed, so it cannot double-darken
        // the key when idle or make the key disappear after ACTION_UP.
        val layer = View(holder.context).apply {
            tag = BACKGROUND_TAG
            background = overlayDrawable
            isClickable = false
            isFocusable = false
            alpha = 0f
        }
        holder.addView(layer, 0, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        ))
        return layer
    }

    private fun collectTextViews(root: View): List<TextView> {
        val found = ArrayList<TextView>()
        fun walk(view: View) {
            if (view is TextView) found.add(view)
            if (view is android.view.ViewGroup) {
                for (i in 0 until view.childCount) walk(view.getChildAt(i))
            }
        }
        walk(root)
        return found
    }

    private fun isLetterView(view: TextView): Boolean {
        val entry = try { view.resources.getResourceEntryName(view.id) } catch (_: Exception) { "" }
        return entry.contains("letter", ignoreCase = true) || entry.contains("letters", ignoreCase = true)
    }

    /** FolmeTouch's source-derived auto-scale rule: max((size - 10dp) / size, 0.9). */
    private fun automaticDownScale(view: View): Float {
        val size = max(view.width, view.height).coerceAtLeast(1)
        val distance = 10f * view.resources.displayMetrics.density
        return max((size - distance) / size.toFloat(), 0.9f)
    }
}
