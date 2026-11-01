package com.zenith.launcher.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.FrameLayout;

import com.zenith.launcher.util.GlobalSidebarOverlayManager;

/**
 * Root FrameLayout for Global Quick Settings Overlay.
 * Intercepts BACK, SETTINGS (176), and MENU keys globally to dismiss the overlay.
 */
public class GlobalSidebarRootLayout extends FrameLayout {

    public GlobalSidebarRootLayout(Context context) {
        super(context);
    }

    public GlobalSidebarRootLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GlobalSidebarRootLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_BACK
                    || keyCode == KeyEvent.KEYCODE_SETTINGS
                    || keyCode == 176
                    || keyCode == KeyEvent.KEYCODE_NOTIFICATION
                    || keyCode == 83
                    || keyCode == 227
                    || keyCode == KeyEvent.KEYCODE_MENU
                    || keyCode == 82) {
                GlobalSidebarOverlayManager manager = GlobalSidebarOverlayManager.getInstance(getContext());
                if (manager != null && manager.isShowing()) {
                    manager.closeSidebar();
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }
}

