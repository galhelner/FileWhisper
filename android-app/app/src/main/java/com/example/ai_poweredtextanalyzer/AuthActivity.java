package com.example.ai_poweredtextanalyzer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class AuthActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private Button authButton;
    private EditText fullNameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private TextView toggleLink;
    private enum Mode { LOGIN, REGISTER }
    private Mode mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth);

        // create shared preferences
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        // find all views references
        findViews();

        // set login mode
        mode = Mode.LOGIN;
        authButton.setText(R.string.login);
        toggleLink.setText(R.string.register_link);
        fullNameInput.setVisibility(View.GONE);

        // handle login button click event
        authButton.setOnClickListener(v -> {
            if (mode == Mode.LOGIN) {
                login();
            } else if (mode == Mode.REGISTER) {
                register();
            } else {
                Log.e("error", "App is in error mode!");
            }
        });

        // handle register link click event
        toggleLink.setOnClickListener(v -> {
            mode = Mode.REGISTER;
            authButton.setText(R.string.register);
            toggleLink.setText(R.string.login_link);
            fullNameInput.setVisibility(View.VISIBLE);
        });
    }

    /**
     * Find all views references
     */
    private void findViews() {
        authButton = findViewById(R.id.authButton);
        fullNameInput = findViewById(R.id.fullNameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        toggleLink = findViewById(R.id.toggleLink);
    }

    /**
     * Call the backend login API to login the system.
     */
    private void login() {
        String email = emailInput.getText().toString();
        String password = passwordInput.getText().toString();
        new Thread(() -> {
            try {
                // login to the backend server and get JWT access token
                String token = ApiClient.login(email, password);
                prefs.edit().putString("jwt_token", token).apply();
                Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                startActivity(intent);
            } catch (RuntimeException e) {
                if (e.getMessage() != null) {
                    Log.e("test", e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Call the backend register API to register as a new user.
     */
    private void register() {
        String fullName = fullNameInput.getText().toString();
        String email = emailInput.getText().toString();
        String password = passwordInput.getText().toString();
        new Thread(() -> {
            try {
                // register as new user at the backend server and get JWT access token
                String token = ApiClient.register(fullName, email, password);
                prefs.edit().putString("jwt_token", token).apply();
                Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                startActivity(intent);
            } catch (RuntimeException e) {
                if (e.getMessage() != null) {
                    Log.e("test", e.getMessage());
                }
            }
        }).start();
    }
}