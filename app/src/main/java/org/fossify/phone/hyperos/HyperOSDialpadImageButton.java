package org.fossify.phone.hyperos;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageButton;

/**
 * Compatibility port of Xiaomi Contacts' DialpadImageButton interaction logic.
 *
 * Source: decompiled HyperOS Contacts DialpadImageButton.java. This keeps the original
 * touch-exploration hover/long-hover, content-description, accessibility-click, and pressed
 * callback behavior while removing Xiaomi-only HapticImageButton/MIUI dependencies.
 * It intentionally does not claim to implement Folme press animations; those live in Xiaomi's
 * separate animation layer and require a compatible Folme implementation.
 */
public class HyperOSDialpadImageButton extends ImageButton {
    private static final int LONG_HOVER_DELAY = ViewConfiguration.getLongPressTimeout() * 2;

    private final AccessibilityManager accessibilityManager;
    private final Rect hitBounds = new Rect();
    private boolean longHovered;
    private CharSequence longHoverDescription;
    private CharSequence originalDescription;
    private boolean wasClickable;
    private boolean wasLongClickable;
    private Runnable longHoverRunnable;
    private OnPressedListener onPressedListener;

    public interface OnPressedListener {
        void onPressed(View view, boolean pressed);
    }

    public HyperOSDialpadImageButton(Context context) {
        this(context, null);
    }

    public HyperOSDialpadImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        accessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
    }

    private void clearLongHover() {
        if (longHoverRunnable != null) removeCallbacks(longHoverRunnable);
        setLongHovered(false);
    }

    private void accessibilityClick() {
        if (isPressed()) return;
        setPressed(true);
        sendAccessibilityEvent(1);
        setPressed(false);
    }

    private void setLongHovered(boolean hovered) {
        if (longHovered == hovered) return;
        longHovered = hovered;
        if (!hovered) {
            super.setContentDescription(originalDescription);
        } else {
            originalDescription = getContentDescription();
            super.setContentDescription(longHoverDescription);
        }
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        if (accessibilityManager != null && accessibilityManager.isEnabled()
                && accessibilityManager.isTouchExplorationEnabled()) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_HOVER_ENTER) {
                wasClickable = isClickable();
                wasLongClickable = isLongClickable();
                if (wasLongClickable && longHoverDescription != null) {
                    if (longHoverRunnable == null) {
                        longHoverRunnable = () -> {
                            setLongHovered(true);
                            announceForAccessibility(longHoverDescription);
                        };
                    }
                    postDelayed(longHoverRunnable, LONG_HOVER_DELAY);
                }
                setClickable(false);
                setLongClickable(false);
            } else if (action == MotionEvent.ACTION_HOVER_EXIT) {
                if (hitBounds.contains((int) event.getX(), (int) event.getY())) {
                    if (longHovered) performLongClick();
                    else accessibilityClick();
                }
                clearLongHover();
                setClickable(wasClickable);
                setLongClickable(wasLongClickable);
            }
        }
        return super.onHoverEvent(event);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        hitBounds.set(0, 0, width, height);
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (action != android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK) {
            return super.performAccessibilityAction(action, arguments);
        }
        accessibilityClick();
        return true;
    }

    @Override
    public void setContentDescription(CharSequence description) {
        if (longHovered) originalDescription = description;
        else super.setContentDescription(description);
    }

    public void setLongHoverContentDescription(CharSequence description) {
        longHoverDescription = description;
        if (longHovered) super.setContentDescription(description);
    }

    public void setOnPressedListener(OnPressedListener listener) {
        onPressedListener = listener;
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        if (pressed) performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        if (onPressedListener != null) onPressedListener.onPressed(this, pressed);
    }
}
