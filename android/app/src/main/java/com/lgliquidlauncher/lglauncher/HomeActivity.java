package com.lgliquidlauncher.lglauncher;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.PixelFormat;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends Activity {

    private static final String PREFS = "lg_launcher_prefs";
    private static final String KEY_FIRST_RUN = "first_run_done";
    private static final String KEY_HOME_OWNER_VERIFIED = "home_owner_verified";
    private static final long CLOCK_TICK_MS = 1000;

    private WallpaperManager wallpaperManager;
    private TextView tvClock;
    private TextView tvDate;
    private LinearLayout topBar;
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            clockHandler.postDelayed(this, CLOCK_TICK_MS);
        }
    };

    private LauncherAdapter adapter;
    private RecyclerView recycler;
    private ArrayList<AppShortcut> shortcuts = new ArrayList<>();
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setContentView(R.layout.activity_home);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        wallpaperManager = WallpaperManager.getInstance(this);

        tvClock = findViewById(R.id.tvClock);
        tvDate = findViewById(R.id.tvDate);
        topBar = findViewById(R.id.topBar);

        setupTopBarButtons();
        setupAppGrid();
        applyWallpaper("ocean");
        updateClock();
        clockHandler.post(clockTick);
        onHomeOwnershipChecked();
    }

    @Override
    protected void onResume() {
        super.onResume();
        onHomeOwnershipChecked();
        updateClock();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (clockHandler != null) {
            clockHandler.removeCallbacks(clockTick);
        }
    }

    private void onHomeOwnershipChecked() {
        if (isHomeMine()) {
            prefs.edit().putBoolean(KEY_HOME_OWNER_VERIFIED, true).apply();
            return;
        }
        if (!prefs.getBoolean(KEY_FIRST_RUN, false)) {
            prefs.edit().putBoolean(KEY_FIRST_RUN, true).apply();
            showFirstRunHomePrompt();
        }
    }

    private boolean isHomeMine() {
        String currentHome = currentHomeComponent();
        return currentHome != null
                && currentHome.startsWith("com.lgliquidlauncher.lglauncher");
    }

    private String currentHomeComponent() {
        try {
            PackageManager pm = getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo ri = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (ri != null) {
                return ri.activityInfo.packageName + "/" + ri.activityInfo.name;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void showFirstRunHomePrompt() {
        new FirstRunDialog().show(getSupportFragmentManager(), "first_run");
    }

    private void setupTopBarButtons() {
        findViewById(R.id.btnInputs).setOnClickListener(v -> showInputsPanel());
        findViewById(R.id.btnWifi).setOnClickListener(v -> showNetworkModal("wifi"));
        findViewById(R.id.btnNetwork).setOnClickListener(v -> showNetworkModal("network"));
        findViewById(R.id.btnWeather).setOnClickListener(v -> showWeatherPanel());
        findViewById(R.id.btnSettings).setOnClickListener(v -> showSettingsPanel());
    }

    private void setupAppGrid() {
        List<AppShortcut> apps = getInstalledApps();
        shortcuts.addAll(apps);
        shortcuts.sort((a, b) -> a.label.compareToIgnoreCase(b.label));

        int spanCount = 6;
        recycler = findViewById(R.id.appGrid);
        if (recycler != null) {
            recycler.setLayoutManager(new GridLayoutManager(this, spanCount));
            adapter = new LauncherAdapter(this, shortcuts);
            recycler.setAdapter(adapter);
        }

        findViewById(R.id.btnAllApps).setOnClickListener(v -> showAllAppsPanel());

        String customKey = "custom_wallpaper_path";
        String path = prefs.getString(customKey, null);
        if (path != null) {
            try {
                java.io.File f = new java.io.File(path);
                if (f.exists()) {
                    Bitmap b = BitmapFactory.decodeFile(path);
                    if (b != null) {
                        applyCustomWallpaper(b);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private List<AppShortcut> getInstalledApps() {
        List<AppShortcut> result = new ArrayList<>();
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        ResolveInfo[] ris = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo ri : ris) {
            AppShortcut s = new AppShortcut();
            String name = ri.loadLabel(pm).toString();
            if (name == null || name.isEmpty()) {
                name = ri.activityInfo.name;
            }
            s.label = name;
            s.packageName = ri.activityInfo.packageName;
            s.className = ri.activityInfo.name;
            DrawableCompat icon = new DrawableCompat();
            try {
                android.graphics.drawable.Drawable d = pm.getActivityIcon(ri.activityInfo);
                if (d != null) icon.drawable = d;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            result.add(s);
        }
        return result;
    }

    private void showAllAppsPanel() {
        Intent i = new Intent(this, AllAppsActivity.class);
        startActivity(i);
    }

    private void showInputsPanel() {
        Intent i = new Intent(this, InputsActivity.class);
        startActivity(i);
    }

    private void showNetworkModal(String mode) {
        Intent i = new Intent(this, NetworkActivity.class);
        i.putExtra("mode", mode);
        startActivity(i);
    }

    private void showWeatherPanel() {
        Intent i = new Intent(this, WeatherActivity.class);
        startActivity(i);
    }

    private void showSettingsPanel() {
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
    }

    private void applyWallpaper(String mode) {
        try {
            WallpaperManager wm = WallpaperManager.getInstance(this);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                wm.clear("");
            } else {
                wm.setBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8));
            }
            if (mode.equals("ocean")) {
                setWallpaperBitmap(createGradientBitmap(Color.parseColor("#002b5c"),
                        Color.parseColor("#9be7f0")), wm);
            } else if (mode.equals("aurora")) {
                setWallpaperBitmap(createGradientBitmap(Color.parseColor("#050a18"),
                        Color.parseColor("#c4b5fd")), wm);
            } else if (mode.equals("sunset")) {
                setWallpaperBitmap(createGradientBitmap(Color.parseColor("#1a0a14"),
                        Color.parseColor("#fef3c7")), wm);
            } else if (mode.equals("live")) {
                // Live clock wallpaper: keep system wallpaper clear so the app UI
                // sits on a dark base. The live-clock visual styling itself is
                // handled by the browser/demo launcher.
            }
        } catch (Exception ignored) {
        }
    }

    private void setWallpaperBitmap(Bitmap b, WallpaperManager wm) {
        try {
            wm.setBitmap(b, null, true);
        } catch (Exception ignored) {
        }
    }

    private void setWallpaperBitmap(Bitmap b) {
        try {
            wallpaperManager.setBitmap(b);
        } catch (Exception ignored) {
        }
    }

    /** Set a custom wallpaper bitmap from a user photo. */
    public void applyCustomWallpaper(Bitmap custom) {
        try {
            WallpaperManager wm = WallpaperManager.getInstance(this);
            setWallpaperBitmap(custom, wm);
        } catch (Exception ignored) {
        }
    }

    private Bitmap createGradientBitmap(int colorStart, int colorEnd) {
        Bitmap b = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        LinearGradient g = new LinearGradient(0, 0, 1920, 1080,
                colorStart, colorEnd, Shader.TileMode.CLAMP);
        Paint p = new Paint();
        p.setShader(g);
        c.drawRect(0, 0, 1920, 1080, p);
        return b;
    }

    private void updateClock() {
        long now = System.currentTimeMillis();
        CharSequence time = DateFormat.format("HH:mm", now);
        CharSequence date = DateFormat.format("EEEE, MMMM d", now);
        if (tvClock != null) tvClock.setText(time);
        if (tvDate != null) tvDate.setText(date);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode >= KeyEvent.KEYCODE_1 && keyCode <= KeyEvent.KEYCODE_9) {
            int idx = keyCode - KeyEvent.KEYCODE_1;
            if (idx < shortcuts.size()) {
                launch(shortcuts.get(idx));
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_STAR || keyCode == KeyEvent.KEYCODE_NUMPAD_STAR) {
            showAllAppsPanel();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            if (!isHomeMine()) {
                startActivity(getHomeIntent());
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private Intent getHomeIntent() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setPackage(getPackageName());
        return intent;
    }

    private void launch(AppShortcut shortcut) {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        i.setClassName(shortcut.packageName, shortcut.className);
        try {
            startActivity(i);
        } catch (Exception ignored) {
        }
    }

    private static class DrawableCompat {
        android.graphics.drawable.Drawable drawable;
    }

    public static class AppShortcut {
        String label;
        String packageName;
        String className;
        DrawableCompat icon = new DrawableCompat();

        public AppShortcut() {
        }

        static ArrayList<AppShortcut> from(Context context) {
            ArrayList<AppShortcut> apps = new ArrayList<>();
            PackageManager pm = context.getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            ResolveInfo[] ris = pm.queryIntentActivities(intent, 0);
            for (ResolveInfo ri : ris) {
                AppShortcut s = new AppShortcut();
                String name = ri.loadLabel(pm).toString();
                if (name == null || name.isEmpty()) {
                    name = ri.activityInfo.name;
                }
                s.label = name;
                s.packageName = ri.activityInfo.packageName;
                s.className = ri.activityInfo.name;
                DrawableCompat ic = new DrawableCompat();
                try {
                    android.graphics.drawable.Drawable d = pm.getActivityIcon(ri.activityInfo);
                    if (d != null) ic.drawable = d;
                } catch (PackageManager.NameNotFoundException ignored) {
                }
                s.icon = ic;
                apps.add(s);
            }

            return apps;
        }
    }
}