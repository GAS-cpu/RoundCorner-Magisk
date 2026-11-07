package com.shizuku.roundcorner;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;

/**
 * 圆角遮罩服务。
 * 优先使用 TYPE_SYSTEM_ERROR（常驻、不闪烁）；
 * 若权限不足则回退 TYPE_TOAST，用双缓冲交替刷新，避免出现直角闪烁。
 */
public class CornerService extends Service {
    private static final int CORNER_RADIUS_DP = 16;          // 圆角半径，单位 dp（越小圆角越小）
    private static final long TYPE_TOAST_REFRESH_MS = 1200;  // TYPE_TOAST 回退时的刷新间隔

    private WindowManager wm;
    private CornerOverlayView viewA;
    private CornerOverlayView viewB;
    private boolean showA = true;
    private float radius;
    private final Handler handler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        Notification n = new Notification.Builder(this)
                .setContentTitle("圆角屏幕")
                .setContentText("屏幕圆角已启用")
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .build();
        startForeground(1, n);

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        radius = CORNER_RADIUS_DP * getResources().getDisplayMetrics().density;

        viewA = new CornerOverlayView(this, radius);
        // 优先尝试常驻类型（需要 SYSTEM_ALERT_WINDOW 权限，系统 App 通常自动授予）
        if (add(viewA, WindowManager.LayoutParams.TYPE_SYSTEM_ERROR)) {
            // 常驻成功，低频兜底即可
            handler.postDelayed(checkRunnable, 15000);
        } else {
            // 权限不足，回退 TYPE_TOAST + 双缓冲刷新
            add(viewA, WindowManager.LayoutParams.TYPE_TOAST);
            handler.postDelayed(refreshRunnable, TYPE_TOAST_REFRESH_MS);
        }
    }

    private boolean add(CornerOverlayView v, int type) {
        try {
            WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            p.gravity = Gravity.TOP | Gravity.START;
            wm.addView(v, p);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void remove(CornerOverlayView v) {
        if (v == null) return;
        try {
            wm.removeView(v);
        } catch (Exception ignored) {
        }
    }

    /** 常驻模式：低频兜底，若被意外移除则重新添加 */
    private final Runnable checkRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewA != null && viewA.getParent() == null) {
                viewA = new CornerOverlayView(CornerService.this, radius);
                add(viewA, WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
            }
            handler.postDelayed(this, 15000);
        }
    };

    /** TYPE_TOAST 回退：双缓冲交替，先加新的再移除旧的，避免直角闪烁 */
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (showA) {
                if (viewB == null) viewB = new CornerOverlayView(CornerService.this, radius);
                add(viewB, WindowManager.LayoutParams.TYPE_TOAST);
                remove(viewA);
                showA = false;
            } else {
                if (viewA == null) viewA = new CornerOverlayView(CornerService.this, radius);
                add(viewA, WindowManager.LayoutParams.TYPE_TOAST);
                remove(viewB);
                showA = true;
            }
            handler.postDelayed(this, TYPE_TOAST_REFRESH_MS);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        remove(viewA);
        remove(viewB);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
