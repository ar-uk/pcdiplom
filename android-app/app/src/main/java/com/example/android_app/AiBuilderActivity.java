package com.example.android_app;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

public class AiBuilderActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AuthGate.ensureSignedIn(this)) {
            return;
        }
        setContentView(R.layout.activity_ai_builder);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.nav_ai);
        }
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        TextInputEditText prompt = findViewById(R.id.ai_prompt);
        ChipGroup chipGroup = findViewById(R.id.ai_chips);
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setOnClickListener(v -> {
                    CharSequence t = chip.getText();
                    if (t == null || prompt.getText() == null) {
                        return;
                    }
                    String cur = prompt.getText().toString();
                    String add = t.toString();
                    if (cur.isEmpty()) {
                        prompt.setText(add);
                    } else {
                        prompt.setText(cur + (cur.endsWith(" ") ? "" : " ") + add);
                    }
                });
            }
        }

        MaterialButton gen = findViewById(R.id.ai_generate_btn);
        gen.setOnClickListener(v ->
                Toast.makeText(AiBuilderActivity.this, R.string.ai_generate, Toast.LENGTH_SHORT).show());

        BottomNavHelper.attach(this, R.id.nav_ai);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavHelper.syncSelection(this, R.id.nav_ai);
    }
}
