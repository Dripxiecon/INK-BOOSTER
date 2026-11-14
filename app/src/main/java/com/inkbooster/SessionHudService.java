package com.inkbooster;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

public final class SessionHudService extends Service {
    public static final String ACTION_STOP = "com.inkbooster.STOP_HUD";
    private static final String CHANNEL_ID = "ink_session_monitor";
    private static final int NOTIFICATION_ID = 41;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private View hud;
    private TextView stats;
    private boolean attached;
    private final Runnable refreshTask = new Runnable() {
        @Override public void run() {
            if (!attached) return;
            refreshHud();
            handler.postDelayed(this, 5000L);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startAsForeground();
        if (!android.provider.Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createHud();
        handler.post(refreshTask);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_NOT_STICKY;
    }

    private void startAsForeground() {
        Intent openApp = new Intent(this, MainActivity.class);
        PendingIntent openPending = PendingIntent.getActivity(this, 1, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, SessionHudService.class).setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 2, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        Notification notification = builder
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle("INK BOOSTER session monitor")
                .setContentText("Floating device status is active")
                .setContentIntent(openPending)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop HUD", stopPending)
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Gaming session monitor", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps the optional floating device monitor running.");
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);
        }
    }

    private void createHud() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(8), dp(8), dp(8));
        row.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(15, 20, 25));
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), Color.rgb(190, 255, 75));
        row.setBackground(bg);

        stats = new TextView(this);
        stats.setTextColor(Color.WHITE);
        stats.setTextSize(12);
        stats.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        stats.setText("INK  •  Checking…");
        row.addView(stats, new LinearLayout.LayoutParams(0, -2, 1f));

        Button close = new Button(this);
        close.setText("×");
        close.setTextSize(18);
        close.setTextColor(Color.rgb(190, 255, 75));
        close.setMinWidth(dp(40));
        close.setMinimumWidth(dp(40));
        close.setMinHeight(dp(40));
        close.setPadding(0, 0, 0, 0);
        close.setOnClickListener(v -> stopSelf());
        row.addView(close);
        row.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float downX, downY;
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = params.x;
                    startY = params.y;
                    downX = event.getRawX();
                    downY = event.getRawY();
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    params.x = startX + (int) (event.getRawX() - downX);
                    params.y = startY + (int) (event.getRawY() - downY);
                    if (attached) windowManager.updateViewLayout(hud, params);
                    return true;
                }
                return event.getAction() == MotionEvent.ACTION_UP;
            }
        });
        hud = row;
        windowManager.addView(hud, params);
        attached = true;
        refreshHud();
    }

    private final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            Build.VERSION.SDK_INT >= 26
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
    {
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = dp(90);
    }

    private void refreshHud() {
        if (stats == null) return;
        ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
        ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(memory);
        long free = memory.availMem / (1024L * 1024L);
        String heat = "Heat ?";
        if (Build.VERSION.SDK_INT >= 29) {
            int status = ((PowerManager) getSystemService(POWER_SERVICE)).getCurrentThermalStatus();
            heat = status >= PowerManager.THERMAL_STATUS_SEVERE ? "Heat HIGH"
                    : status >= PowerManager.THERMAL_STATUS_MODERATE ? "Heat WARM" : "Heat OK";
        }
        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        String temp = "--";
        if (battery != null) {
            int tenths = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            if (tenths >= 0) temp = String.format(Locale.getDefault(), "%.0f°C", tenths / 10.0);
        }
        stats.setText(String.format(Locale.getDefault(), "INK  •  RAM %dMB  •  %s  •  %s", free, heat, temp));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override public void onDestroy() {
        attached = false;
        handler.removeCallbacksAndMessages(null);
        if (windowManager != null && hud != null) {
            try { windowManager.removeView(hud); } catch (IllegalArgumentException ignored) { }
        }
        stopForeground(true);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
