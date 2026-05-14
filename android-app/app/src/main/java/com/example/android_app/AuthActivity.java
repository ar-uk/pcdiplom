package com.example.android_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class AuthActivity extends AppCompatActivity {

    private boolean registerMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        View navSlot = findViewById(R.id.bottom_nav_slot);
        if (navSlot != null) {
            navSlot.setVisibility(View.GONE);
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.brand_title);
        }
        if (isTaskRoot()) {
            toolbar.setNavigationIcon(null);
            toolbar.setNavigationOnClickListener(null);
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        TextView headline = findViewById(R.id.auth_headline);
        TextView sub = findViewById(R.id.auth_subtitle_dynamic);
        TextInputLayout usernameLayout = findViewById(R.id.username_layout);
        TextInputEditText username = findViewById(R.id.username);
        TextInputEditText email = findViewById(R.id.email);
        TextInputEditText password = findViewById(R.id.password);
        MaterialButton submit = findViewById(R.id.auth_submit);
        MaterialButtonToggleGroup modeToggle = findViewById(R.id.auth_mode_toggle);

        Runnable updateUi = () -> {
            registerMode = modeToggle.getCheckedButtonId() == R.id.mode_register;
            usernameLayout.setVisibility(registerMode ? View.VISIBLE : View.GONE);
            headline.setText(registerMode ? R.string.auth_headline_register : R.string.login);
            sub.setText(registerMode ? R.string.auth_subtitle_register : R.string.auth_subtitle);
            submit.setText(registerMode ? R.string.register_button : R.string.login_button);
        };

        modeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            updateUi.run();
        });
        updateUi.run();

        submit.setOnClickListener(v -> {
            String u = username.getText() != null ? username.getText().toString().trim() : "";
            String e = email.getText() != null ? email.getText().toString().trim() : "";
            String p = password.getText() != null ? password.getText().toString() : "";
            if (registerMode) {
                if (u.length() < 3) {
                    Toast.makeText(AuthActivity.this, R.string.auth_username_short, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            if (e.isEmpty() || p.isEmpty()) {
                Toast.makeText(AuthActivity.this, R.string.auth_email_password_required, Toast.LENGTH_SHORT).show();
                return;
            }
            if (p.length() < 6) {
                Toast.makeText(AuthActivity.this, R.string.auth_password_short, Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                String apiBase = getString(R.string.web_start_url).replace("5173/", "8080");
                final AuthManager mgr = new AuthManager(AuthActivity.this, apiBase);
                submit.setEnabled(false);
                if (registerMode) {
                    mgr.register(u, e, p, new AuthManager.Callback() {
                        @Override
                        public void onSuccess(String token) {
                            runOnUiThread(() -> goHomeAfterAuth(submit));
                        }

                        @Override
                        public void onError(String message) {
                            runOnUiThread(() -> {
                                submit.setEnabled(true);
                                Toast.makeText(AuthActivity.this, message, Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                } else {
                    mgr.login(e, p, new AuthManager.Callback() {
                        @Override
                        public void onSuccess(String token) {
                            runOnUiThread(() -> goHomeAfterAuth(submit));
                        }

                        @Override
                        public void onError(String message) {
                            runOnUiThread(() -> {
                                submit.setEnabled(true);
                                Toast.makeText(AuthActivity.this, message, Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                }
            } catch (Exception ex) {
                Toast.makeText(AuthActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                submit.setEnabled(true);
            }
        });
    }

    private void goHomeAfterAuth(MaterialButton submit) {
        Toast.makeText(AuthActivity.this, R.string.auth_signed_in, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        Intent i = new Intent(AuthActivity.this, HomeActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        submit.setEnabled(true);
        finish();
    }
}
