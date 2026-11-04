package com.lgliquidlauncher.lglauncher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AllAppsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_apps);

        List<HomeActivity.AppShortcut> apps = HomeActivity.AppShortcut.from(this);
        RecyclerView recycler = findViewById(R.id.allAppsGrid);
        recycler.setLayoutManager(new GridLayoutManager(this, 6));
        recycler.setAdapter(new LauncherAdapter(this, apps));
    }
}