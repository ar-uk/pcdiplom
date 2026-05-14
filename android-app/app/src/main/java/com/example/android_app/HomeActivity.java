package com.example.android_app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AuthGate.ensureSignedIn(this)) {
            return;
        }

        setContentView(R.layout.activity_home);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.brand_title);
        }
        toolbar.setLogo(R.drawable.ic_memory_24);
        toolbar.inflateMenu(R.menu.home_toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_account) {
                openAccount();
                return true;
            }
            return false;
        });

        findViewById(R.id.card_manual_build).setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, BuilderActivity.class)));

        findViewById(R.id.card_ai_build).setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, AiBuilderActivity.class)));

        findViewById(R.id.view_all_community).setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, DiscoveryActivity.class)));

        BottomNavHelper.attach(this, R.id.nav_home);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavHelper.syncSelection(this, R.id.nav_home);
    }

    private void openAccount() {
        startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
    }
}
