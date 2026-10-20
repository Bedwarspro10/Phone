package org.fossify.phone.hyperos;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.RelativeLayout;

/**
 * Fossify-compatible adapter for behavior traced from Xiaomi Contacts' DialerItemView.
 *
 * Source-derived behavior retained here:
 * - tracks the view bounds used by the accessibility hover path;
 * - on touch-exploration hover exit, briefly sets pressed state and sends click feedback;
 * - forwards every pressed-state transition through the same listener role used by Xiaomi's
 *   DialerItemView, so a later Folme adapter can consume the original callback without changing
 *   Fossify's key IDs or its DTMF/call listeners.
 *
 * The original Xiaomi class inflates dialer_item_view and relies on Xiaomi-only resources,
 * DialerModuleUtil, ScreenModeHelper, TinyScreenUtil and Folme. This class intentionally remains
 * a RelativeLayout because the existing Fossify dialpad XML supplies the key's child views.
 * It does not pretend to include Folme or Xiaomi's proprietary typography/resources.
 */
public class HyperOSDialerItemView extends RelativeLayout {
    public interface OnPressedListener {
        void onPressed(View view, boolean pressed);
    }

    private final Rect hitBounds = new Rect();
    private AccessibilityManager accessibilityManager;
    private OnPressedListener onPressedListener;

    public HyperOSDialerItemView(Context context) {
        this(context, null);
    }

    public HyperOSDialerItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HyperOSDialerItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Object service = context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (service instanceof AccessibilityManager) {
            accessibilityManager = (AccessibilityManager) service;
        }
        setFocusable(true);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    /** Matches the listener hook exposed by the original Xiaomi DialerItemView. */
    public void setOnPressedListener(OnPressedListener listener) {
        onPressedListener = listener;
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        OnPressedListener listener = onPressedListener;
        if (listener != null) {
            listener.onPressed(this, pressed);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        hitBounds.set(0, 0, w, h);
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        if (event != null && accessibilityManager != null
                && accessibilityManager.isEnabled()
                && accessibilityManager.isTouchExplorationEnabled()
                && event.getActionMasked() == MotionEvent.ACTION_HOVER_EXIT) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            if (x != 0 && y != 0 && hitBounds.contains(x, y)) {
                setPressed(true);
                sendAccessibilityEvent(
                        android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED);
                setPressed(false);
            }
        }
        return super.onHoverEvent(event);
    }
}
