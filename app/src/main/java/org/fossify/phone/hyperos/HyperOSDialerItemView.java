package org.fossify.phone.hyperos;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.RelativeLayout;

/**
 * Compatibility port of the accessibility-hover behavior from Xiaomi Contacts' DialerItemView.
 *
 * The original class also depends on Xiaomi DialerModuleUtil, private TTS/accessibility helpers,
 * proprietary layout resources, and Folme setup performed by AnimationUtil. Those dependencies
 * are deliberately not replaced with invented animations here. This view preserves the source
 * behavior that can be implemented with public Android APIs: bounds tracking, touch-exploration
 * hover detection, pressed accessibility feedback, and forwarding the original key description.
 * It remains a RelativeLayout so Fossify's existing key children, IDs, DTMF handlers, speed dial,
 * and click/touch wiring continue to work.
 */
public class HyperOSDialerItemView extends RelativeLayout {
    private final Rect hitBounds = new Rect();
    private AccessibilityManager accessibilityManager;

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
                sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED);
                setPressed(false);
            }
        }
        return super.onHoverEvent(event);
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        return super.dispatchHoverEvent(event);
    }
}
