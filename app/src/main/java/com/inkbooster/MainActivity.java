package com.inkbooster;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final String ROBLOX_PACKAGE = "com.roblox.client";
    private static final int BG = Color.rgb(10, 13, 17);
    private static final int PANEL = Color.rgb(21, 27, 34);
    private static final int INK = Color.rgb(190, 255, 75);
    private static final int MUTED = Color.rgb(155, 166, 178);
    private TextView memoryValue, thermalValue, batteryValue, networkValue, statusValue;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildDashboard();
        refreshDeviceStatus();
    }

    @Override protected void onResume() {
        super.onResume();
        if (memoryValue != null) refreshDeviceStatus();
    }

    private void buildDashboard() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(28));
        scroll.addView(root);

        TextView eyebrow = text("ANDROID GAME COMPANION", 11, INK, true);
        eyebrow.setLetterSpacing(.14f);
        root.addView(eyebrow);
        TextView title = text("INK BOOSTER", 31, Color.WHITE, true);
        title.setPadding(0, dp(7), 0, 0);
        root.addView(title);
        TextView subtitle = text("A cleaner start. A steadier session.", 14, MUTED, false);
        subtitle.setPadding(0, dp(5), 0, dp(20));
        root.addView(subtitle);

        LinearLayout hero = panel();
        hero.addView(text("GAME SESSION", 11, INK, true));
        statusValue = text("Ready to check your phone", 18, Color.WHITE, true);
        statusValue.setPadding(0, dp(8), 0, dp(5));
        hero.addView(statusValue);
        hero.addView(text("Monitor your device and open Roblox. This app does not modify Roblox or promise extra FPS.", 13, MUTED, false));
        root.addView(hero, margin(0, 0, 0, 14));

        root.addView(sectionTitle("DEVICE STATUS"));
        LinearLayout stats = panel();
        memoryValue = addStat(stats, "AVAILABLE MEMORY", "Checking…");
        thermalValue = addStat(stats, "THERMAL STATUS", "Checking…");
        batteryValue = addStat(stats, "BATTERY TEMPERATURE", "Checking…");
        networkValue = addStat(stats, "NETWORK", "Checking…");
        root.addView(stats, margin(0, 0, 0, 14));

        root.addView(sectionTitle("QUICK ACTIONS"));
        Button launch = actionButton("LAUNCH ROBLOX", true);
        launch.setOnClickListener(v -> launchRoblox());
        root.addView(launch, margin(0, 0, 0, 9));
        Button refresh = actionButton("REFRESH DEVICE STATUS", false);
        refresh.setOnClickListener(v -> refreshDeviceStatus());
        root.addView(refresh, margin(0, 0, 0, 9));
        Button battery = actionButton("OPEN BATTERY SETTINGS", false);
        battery.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)); }
            catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        });
        root.addView(battery, margin(0, 0, 0, 16));

        root.addView(sectionTitle("THE FORGE — STARTING PRESET"));
        LinearLayout tips = panel();
        addTip(tips, "01", "Roblox graphics", "Set Graphics Mode to Manual, then start low (1–3 bars). Raise it only while the game stays smooth.");
        addTip(tips, "02", "Before playing", "Close apps you are not using, pause large downloads, and let the phone cool if it feels hot.");
        addTip(tips, "03", "During a session", "Keep brightness comfortable and avoid charging in direct sunlight.");
        addTip(tips, "04", "If lag continues", "Try stable Wi-Fi. High ping is a network issue; low FPS is usually device or game load.");
        root.addView(tips, margin(0, 0, 0, 14));

        root.addView(sectionTitle("PLAYER-FIRST VISUALS"));
        LinearLayout visuals = panel();
        addTip(visuals, "01", "Keep nearby players clear", "Use a low manual graphics level first. Roblox may shorten view distance and reduce scene detail, helping the phone focus on what is close.");
        addTip(visuals, "02", "Cut distant glow", "The normal Roblox graphics slider reduces visual workload broadly. It cannot selectively hide far-away pickaxe auras while keeping nearby effects.");
        addTip(visuals, "03", "Game-side optimization", "The Forge developer would need distance-based effect limits: show full effects nearby, simplify them farther away, and stop updating off-screen cosmetic effects.");
        addTip(visuals, "04", "Keep gameplay readable", "Do not use unofficial clients, injectors, or modified Roblox files. INK BOOSTER stays outside the game and uses supported settings only.");
        root.addView(visuals);

        TextView footer = text("NO ROOT  •  NO GAME INJECTION  •  NO FAKE RAM CLEANING", 10, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(18), 0, 0);
        root.addView(footer);
        setContentView(scroll);
    }

    private void refreshDeviceStatus() {
        if (memoryValue == null) return;
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        manager.getMemoryInfo(info);
        long freeMb = info.availMem / (1024L * 1024L);
        long totalMb = info.totalMem / (1024L * 1024L);
        memoryValue.setText(String.format(Locale.getDefault(), "%,d MB free / %,d MB total", freeMb, totalMb));
        thermalValue.setText(readThermalStatus());
        batteryValue.setText(readBatteryTemperature());
        networkValue.setText(readNetwork());
        statusValue.setText(info.lowMemory ? "Memory is running low" : "Your phone is ready to check");
    }

    private String readThermalStatus() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "Not available";
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        switch (power.getCurrentThermalStatus()) {
            case PowerManager.THERMAL_STATUS_NONE: return "Normal";
            case PowerManager.THERMAL_STATUS_LIGHT: return "Light";
            case PowerManager.THERMAL_STATUS_MODERATE: return "Moderate — take breaks";
            case PowerManager.THERMAL_STATUS_SEVERE: return "Severe — stop and cool down";
            case PowerManager.THERMAL_STATUS_CRITICAL: return "Critical — stop playing";
            case PowerManager.THERMAL_STATUS_EMERGENCY: return "Emergency";
            default: return "Unknown";
        }
    }

    private String readBatteryTemperature() {
        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) return "Unavailable";
        int value = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        return value < 0 ? "Unavailable" : String.format(Locale.getDefault(), "%.1f °C", value / 10.0);
    }

    private String readNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        Network active = cm.getActiveNetwork();
        if (active == null) return "No active connection";
        NetworkCapabilities caps = cm.getNetworkCapabilities(active);
        if (caps == null) return "Connection status unknown";
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "Wi-Fi";
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "Mobile data";
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "Ethernet";
        return "Connected";
    }

    private void launchRoblox() {
        PackageManager pm = getPackageManager();
        Intent launch = pm.getLaunchIntentForPackage(ROBLOX_PACKAGE);
        if (launch == null) {
            Toast.makeText(this, "Roblox is not installed. Install it from Google Play first.", Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(launch);
    }

    private TextView addStat(LinearLayout parent, String label, String value) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(0, dp(7), 0, dp(10));
        item.addView(text(label, 10, MUTED, true));
        TextView result = text(value, 16, Color.WHITE, true);
        result.setPadding(0, dp(3), 0, 0);
        item.addView(result);
        parent.addView(item);
        return result;
    }

    private void addTip(LinearLayout parent, String number, String heading, String body) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(0, dp(5), 0, dp(13));
        item.addView(text(number + "  /  " + heading, 14, INK, true));
        TextView detail = text(body, 13, MUTED, false);
        detail.setPadding(0, dp(4), 0, 0);
        item.addView(detail);
        parent.addView(item);
    }

    private TextView sectionTitle(String value) {
        TextView view = text(value, 11, MUTED, true);
        view.setLetterSpacing(.12f);
        view.setPadding(dp(2), dp(2), 0, dp(9));
        return view;
    }

    private LinearLayout panel() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(15), dp(16), dp(8));
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(PANEL);
        shape.setCornerRadius(dp(16));
        shape.setStroke(dp(1), Color.rgb(39, 49, 59));
        box.setBackground(shape);
        return box;
    }

    private Button actionButton(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(13);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setMinHeight(dp(54));
        button.setTextColor(primary ? BG : Color.WHITE);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(primary ? INK : PANEL);
        shape.setCornerRadius(dp(13));
        if (!primary) shape.setStroke(dp(1), Color.rgb(55, 66, 77));
        button.setBackground(shape);
        return button;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout.LayoutParams margin(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
