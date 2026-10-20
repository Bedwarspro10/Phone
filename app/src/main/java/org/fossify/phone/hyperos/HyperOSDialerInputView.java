package org.fossify.phone.hyperos;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.fossify.phone.R;

/**
 * Compatibility port of Xiaomi Contacts' com.android.contacts.dialer.view.DialerInputView.
 *
 * The measurement/layout flow is derived from the supplied decompiled HyperOS source. Xiaomi's
 * ScreenModeHelper and TinyScreenUtil branches are intentionally omitted because they depend on
 * private device configuration; the standard-device path and child positioning are retained.
 * The child IDs are mapped to Fossify's existing input and clear-button IDs so its existing
 * DialpadActivity text, call, clear, and keyboard logic continues to own behavior.
 */
public class HyperOSDialerInputView extends LinearLayout {
    private EditText digits;
    private View clearButton;
    private TextView digitsLabel;
    private int shadowHeight;

    public HyperOSDialerInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HyperOSDialerInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        digits = findViewById(R.id.dialpad_input);
        clearButton = findViewById(R.id.dialpad_clear_char);
        digitsLabel = findViewById(R.id.hyperos_dialpad_digits_label);
        if (digits != null) {
            // Same alignment used by Xiaomi's original DialerInputView.
            digits.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        }
        shadowHeight = getResources().getDimensionPixelSize(R.dimen.hyperos_dialpad_shadow_height);
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        super.dispatchTouchEvent(event);
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (digits == null || clearButton == null || digitsLabel == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int resolvedWidth = View.resolveSize(0, widthMeasureSpec);
        int minHeight = getSuggestedMinimumHeight();
        digitsLabel.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

        ViewGroup.LayoutParams clearParams = clearButton.getLayoutParams();
        clearButton.measure(
                MeasureSpec.makeMeasureSpec(clearParams.width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(clearParams.height, MeasureSpec.EXACTLY));

        ViewGroup.MarginLayoutParams clearMargins = (ViewGroup.MarginLayoutParams) clearButton.getLayoutParams();
        int clearWidth = clearButton.getMeasuredWidth();
        int availableInputWidth = Math.max(0, resolvedWidth - clearWidth - clearMargins.getMarginEnd());
        digits.measure(
                MeasureSpec.makeMeasureSpec(availableInputWidth, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

        int contentHeight = digitsLabel.getMeasuredHeight() + digits.getMeasuredHeight() + getPaddingTop();
        int height = contentHeight - shadowHeight
                + getResources().getDimensionPixelSize(R.dimen.hyperos_dialpad_padding_top);
        setMeasuredDimension(resolvedWidth, Math.max(minHeight, height));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (digits == null || clearButton == null || digitsLabel == null) {
            super.onLayout(changed, left, top, right, bottom);
            return;
        }

        int contentTop = top + getPaddingTop();
        boolean isLtr = getLayoutDirection() == View.LAYOUT_DIRECTION_LTR;
        ViewGroup.MarginLayoutParams clearMargins = (ViewGroup.MarginLayoutParams) clearButton.getLayoutParams();
        int clearWidth = clearButton.getMeasuredWidth();
        int clearHeight = clearButton.getMeasuredHeight();
        int inputHeight = digits.getMeasuredHeight();
        int clearTop = contentTop + ((inputHeight - clearHeight) / 2);

        if (isLtr) {
            int clearRight = right - clearMargins.getMarginEnd();
            clearButton.layout(clearRight - clearWidth, clearTop, clearRight, clearTop + clearHeight);
        } else {
            int clearLeft = left + clearMargins.getMarginEnd();
            clearButton.layout(clearLeft, clearTop, clearLeft + clearWidth, clearTop + clearHeight);
        }

        int sideInset = clearWidth + clearMargins.getMarginEnd();
        int inputLeft = left + sideInset;
        int inputRight = right - sideInset;
        ViewGroup.MarginLayoutParams inputMargins = (ViewGroup.MarginLayoutParams) digits.getLayoutParams();
        int inputTop = contentTop + inputMargins.topMargin;
        int inputBottom = inputTop + inputHeight;
        digits.setGravity(android.view.Gravity.CENTER);
        if (digits.getMeasuredWidth() > inputRight - inputLeft) {
            digits.setGravity(android.view.Gravity.CENTER);
        }
        digits.layout(inputLeft, inputTop, inputRight, inputBottom);

        int labelHeight = digitsLabel.getMeasuredHeight();
        int labelTop = inputBottom - (shadowHeight / 2);
        digitsLabel.layout(left, labelTop, right, labelTop + labelHeight);
    }
}
