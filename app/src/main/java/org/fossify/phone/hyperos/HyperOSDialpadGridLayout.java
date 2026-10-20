package org.fossify.phone.hyperos;

import android.content.Context;
import android.util.AttributeSet;
import androidx.constraintlayout.widget.ConstraintLayout;

/**
 * ConstraintLayout host for Fossify's existing dialpad key views.
 *
 * The key XML expresses its 3-column / 4-row geometry through ConstraintLayout constraints.
 * A FrameLayout-based custom host does not solve those constraints, so the XML's layout rules
 * are ignored and a second, competing placement algorithm is needed. Using ConstraintLayout
 * lets the existing XML constraints do their intended job while retaining Fossify view IDs and
 * existing click/DTMF listeners.
 *
 * This is a layout-host compatibility fix only. It does not claim to port Xiaomi's original
 * DialPadDigitsKeyContainer or Folme animation runtime.
 */
public class HyperOSDialpadGridLayout extends ConstraintLayout {
    public HyperOSDialpadGridLayout(Context context) {
        this(context, null);
    }

    public HyperOSDialpadGridLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HyperOSDialpadGridLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setClipChildren(false);
        setClipToPadding(false);
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }
}
