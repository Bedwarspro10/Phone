package org.fossify.phone.hyperos;

import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;

/**
 * Compatibility port of the accessibility delegate installed by Xiaomi's
 * com.android.contacts.dialer.view.DialPadDigitsKeyContainer on each dialer key.
 *
 * The original delegate exposes the key as a Button, marks it clickable, and adds the
 * long-click accessibility action. This helper keeps those semantics without requiring
 * MIUIX's private Button class.
 */
public final class HyperOSDialpadAccessibility {
    private HyperOSDialpadAccessibility() { }

    public static void install(View key) {
        if (key == null) return;
        key.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(Button.class.getName());
                info.setClickable(true);
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK);
            }
        });
    }
}
