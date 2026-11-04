package com.lgliquidlauncher.lglauncher;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends Activity {

    private LinearLayout container;
    private SharedPreferences prefs;
    private static final String PREFS = "lg_launcher_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        container = findViewById(R.id.settingsContainer);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildSettings();
    }

    private void buildSettings() {
        container.removeAllViews();

        addHeader("Settings");

        setupCustomWallpaperButton();
        addWallpaperSection();
        addBrightnessSection();
        addScreensaverSection();
        addHomeOwnershipSection();
        addAboutSection();

        Button close = new Button(this);
        close.setText("Close");
        close.setGravity(Gravity.CENTER);
        close.setBackgroundResource(R.drawable.bg_button);
        close.setTextColor(getColor(R.color.textPrimary));
        close.setOnClickListener(v -> finish());
        container.addView(close);
    }

    private void addHeader(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getColor(R.color.textPrimary));
        tv.setTextSize(20);
        tv.setGravity(Gravity.LEFT);
        tv.setPadding(0, 12, 0, 12);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        container.addView(tv);
    }

    private void setupCustomWallpaperButton() {
        Button custom = findViewById(R.id.btnCustomWallpaper);
        if (custom != null) {
            custom.setText(R.string.custom_wallpaper_title);
            custom.setOnClickListener(v -> {
                // On a real TV this would launch a photo / file picker.
                // Here we apply a placeholder custom wallpaper to show the path is wired.
                Bitmap placeholder = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(placeholder);
                Paint p = new Paint();
                p.setColor(Color.parseColor("#1a2030"));
                c.drawRect(0, 0, 1920, 1080, p);
                LinearGradient g = new LinearGradient(0, 0, 1920, 1080,
                        Color.parseColor("#5EEAD4"), Color.parseColor("#A78BFA"), Shader.TileMode.CLAMP);
                p.setShader(g);
                p.setStrokeWidth(40);
                p.setAntiAlias(true);
                c.drawLine(200, 500, 1720, 500, p);
                c.drawLine(200, 540, 1720, 540, p);
                c.drawLine(200, 580, 1720, 580, p);
                try {
                    WallpaperManager.getInstance(this).setBitmap(placeholder, null, true);
                    Toast.makeText(this, R.string.wallpaper_custom_applied, Toast.LENGTH_SHORT).show();
                } catch (Exception ignored) {
                    Toast.makeText(this, R.string.wallpaper_apply_failure, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void addWallpaperSection() {
        LinearLayout row = createRow("Wallpaper");
        String current = prefs.getString("wallpaper", "ocean");
        String[] modes = {"ocean", "aurora", "sunset"};
        for (String mode : modes) {
            Button b = new Button(this);
            b.setText(mode.substring(0, 1).toUpperCase() + mode.substring(1));
            b.setGravity(Gravity.CENTER);
            b.setBackgroundResource(mode.equals(current) ? R.drawable.bg_button_active : R.drawable.bg_button);
            b.setTextColor(getColor(R.color.textPrimary));
            b.setOnClickListener(v -> {
                prefs.edit().putString("wallpaper", mode).apply();
                setWallpaperBitmap(createGradientForMode(mode));
                rebuildRows();
            });
            row.addView(b);
        }
        container.addView(row);
    }

    private Bitmap createGradientForMode(String mode) {
        if (mode.equals("ocean")) {
            return createGradientBitmap(Color.parseColor("#002b5c"), Color.parseColor("#9be7f0"));
        } else if (mode.equals("aurora")) {
            return createGradientBitmap(Color.parseColor("#050a18"), Color.parseColor("#c4b5fd"));
        } else {
            return createGradientBitmap(Color.parseColor("#1a0a14"), Color.parseColor("#fef3c7"));
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

    private void setWallpaperBitmap(Bitmap b) {
        try {
            WallpaperManager.getInstance(this).setBitmap(b);
        } catch (Exception ignored) {
        }
    }

    private void addBrightnessSection() {
        LinearLayout row = createRow("Display Brightness");
        TextView val = new TextView(this);
        val.setText("Set by system");
        row.addView(val);
        container.addView(row);
    }

    private void addScreensaverSection() {
        LinearLayout row = createRow("Keep screen on");
        Switch sw = new Switch(this);
        sw.setText("Keep screen on");
        sw.setChecked(true);
        sw.setPadding(0, 0, 40, 0);
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getWindow().setFlags(
                    isChecked ? WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON : 0,
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        });
        row.addView(sw);
        container.addView(row);
    }

    private void addHomeOwnershipSection() {
        LinearLayout row = createRow("Home screen status");
        TextView status = new TextView(this);
        status.setTextColor(getColor(R.color.textPrimary));
        if (isHomeMine()) {
            status.setText("This app is your Home screen");
        } else {
            status.setText("Not set as Home screen yet");
        }
        row.addView(status);

        Button choose = new Button(this);
        choose.setText("Set as Home");
        choose.setGravity(Gravity.CENTER);
        choose.setBackgroundResource(R.drawable.bg_button);
        choose.setTextColor(getColor(R.color.textPrimary));
        choose.setOnClickListener(v -> launchHomeChooser());
        row.addView(choose);

        container.addView(row);
    }

    private boolean isHomeMine() {
        try {
            PackageManager pm = getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo ri = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (ri != null) {
                return ri.activityInfo.packageName.equals(getPackageName());
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void launchHomeChooser() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setPackage(getPackageName());
        try {
            startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    private void addAboutSection() {
        LinearLayout row = createRow("About");
        TextView tv = new TextView(this);
        tv.setText("Liquid Glass Launcher v1.0.0\n"
                + "Package: com.lgliquidlauncher.lglauncher");
        tv.setTextColor(getColor(R.color.textDim));
        tv.setGravity(Gravity.LEFT);
        tv.setPadding(0, 4, 0, 8);
        row.addView(tv);
        container.addView(row);
    }

    private LinearLayout createRow(String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 16);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(getColor(R.color.textPrimary));
        lbl.setTextSize(16);
        lbl.setTypeface(null, android.graphics.Typeface.BOLD);
        lbl.setGravity(Gravity.LEFT);
        lbl.setPadding(0, 0, 16, 0);
        row.addView(lbl);

        LinearLayout slot = new LinearLayout(this);
        slot.setOrientation(LinearLayout.VERTICAL);
        slot.setGravity(Gravity.CENTER);
        row.addView(slot);
        row.setWeightSum(1f);
        ViewGroup.LayoutParams lp = row.getLayoutParams();
        row.setLayoutParams(lp);
        return row;
    }

    private void rebuildRows() {
        buildSettings();
    }
}