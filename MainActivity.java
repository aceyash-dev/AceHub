package com.acehub.monitor;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnLaunchHud, btnSelectApp, btnPrivacy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        [span_3](start_span)// UI references from resources[span_3](end_span)
        btnLaunchHud = findViewById(R.id.btn_launch_game);
        btnSelectApp = findViewById(R.id.btn_select_app);
        btnPrivacy = findViewById(R.id.btn_privacy);

        btnLaunchHud.setOnClickListener(v -> checkOverlayPermission());
        
        btnSelectApp.setOnClickListener(v -> {
            startActivity(new Intent(this, PickerActivity.class));
        });

        btnPrivacy.setOnClickListener(v -> {
            [span_4](start_span)// Displays Privacy Policy updated Jan 20, 2026[span_4](end_span)
            startActivity(new Intent(this, OnboardingActivity.class));
        });
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 101);
            } else {
                startHudService();
            }
        } else {
            startHudService();
        }
    }

    private void startHudService() {
        Intent intent = new Intent(this, PerformanceService.class);
        startService(intent);
    }
}
