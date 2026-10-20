package org.fossify.phone.hyperos;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.fossify.phone.R;

/**
 * Compatibility port of the standard 3-column / 4-row keypad measurement and placement
 * algorithm from Xiaomi Contacts' DialPadDigitsKeyContainer.
 *
 * The original class dynamically creates Xiaomi DialerItemView / DialpadImageButton instances
 * and depends on MIUI settings, Folme, tiny-screen helpers and Xiaomi resources. This version
 * deliberately retains Fossify's existing XML key children and IDs so the current ViewBinding,
 * DTMF, long-press, speed-dial and call logic remain connected. It ports the source layout
 * geometry only; it does not claim to port Folme animations or Xiaomi's private services.
 */
public class HyperOSDialpadGridLayout extends FrameLayout {
    private static final int[] KEY_IDS = new int[] {
            R.id.dialpad_1_holder,
            R.id.dialpad_2_holder,
            R.id.dialpad_3_holder,
            R.id.dialpad_4_holder,
            R.id.dialpad_5_holder,
            R.id.dialpad_6_holder,
            R.id.dialpad_7_holder,
            R.id.dialpad_8_holder,
            R.id.dialpad_9_holder,
            R.id.dialpad_asterisk_holder,
            R.id.dialpad_0_holder,
            R.id.dialpad_hashtag_holder
    };

    private final int defaultKeyHeight;

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
        defaultKeyHeight = getResources().getDimensionPixelSize(R.dimen.dialpad_button_size);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            width = getSuggestedMinimumWidth();
        }
        int horizontalPadding = getPaddingLeft() + getPaddingRight();
        int verticalPadding = getPaddingTop() + getPaddingBottom();
        int keyWidth = Math.max(1, (width - horizontalPadding) / 3);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int desiredHeight = verticalPadding + (defaultKeyHeight * 4);
        int height;
        if (heightMode == MeasureSpec.EXACTLY) {
            height = MeasureSpec.getSize(heightMeasureSpec);
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(desiredHeight, MeasureSpec.getSize(heightMeasureSpec));
        } else {
            height = desiredHeight;
        }
        int contentHeight = Math.max(4, height - verticalPadding);
        int keyHeight = Math.max(1, contentHeight / 4);

        for (int id : KEY_IDS) {
            View key = findViewById(id);
            if (key == null || key.getVisibility() == GONE) {
                continue;
            }
            ViewGroup.LayoutParams rawParams = key.getLayoutParams();
            int marginStart = 0;
            int marginEnd = 0;
            int marginTop = 0;
            int marginBottom = 0;
            if (rawParams instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) rawParams;
                marginStart = margins.getMarginStart();
                marginEnd = margins.getMarginEnd();
                marginTop = margins.topMargin;
                marginBottom = margins.bottomMargin;
            }
            int childWidth = Math.max(1, keyWidth - marginStart - marginEnd);
            int childHeight = Math.max(1, keyHeight - marginTop - marginBottom);
            key.measure(
                    MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        int contentWidth = Math.max(1, width - getPaddingLeft() - getPaddingRight());
        int contentHeight = Math.max(4, height - getPaddingTop() - getPaddingBottom());
        int cellWidth = contentWidth / 3;
        int cellHeight = contentHeight / 4;

        for (int index = 0; index < KEY_IDS.length; index++) {
            View key = findViewById(KEY_IDS[index]);
            if (key == null || key.getVisibility() == GONE) {
                continue;
            }
            int row = index / 3;
            int column = index % 3;
            int marginStart = 0;
            int marginEnd = 0;
            int marginTop = 0;
            int marginBottom = 0;
            ViewGroup.LayoutParams rawParams = key.getLayoutParams();
            if (rawParams instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) rawParams;
                marginStart = margins.getMarginStart();
                marginEnd = margins.getMarginEnd();
                marginTop = margins.topMargin;
                marginBottom = margins.bottomMargin;
            }
            int childLeft = getPaddingLeft() + column * cellWidth + marginStart;
            int childTop = getPaddingTop() + row * cellHeight + marginTop;
            int childRight = getPaddingLeft() + (column + 1) * cellWidth - marginEnd;
            int childBottom = getPaddingTop() + (row + 1) * cellHeight - marginBottom;
            key.layout(childLeft, childTop, Math.max(childLeft + 1, childRight), Math.max(childTop + 1, childBottom));
        }

        // The optional alternative '+' key is intentionally left GONE, matching Fossify's
        // current dialpad behavior and preserving its existing ViewBinding ID.
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }
}
