package com.example.ai_poweredtextanalyzer.Activities;

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
import android.util.Patterns;
import android.widget.Toast;

import com.example.ai_poweredtextanalyzer.Utils.ApiClient;
import com.example.ai_poweredtextanalyzer.R;

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
            if (mode == Mode.LOGIN) {
                mode = Mode.REGISTER;
                authButton.setText(R.string.register);
                toggleLink.setText(R.string.login_link);
                fullNameInput.setVisibility(View.VISIBLE);
            } else {
                mode = Mode.LOGIN;
                authButton.setText(R.string.login);
                toggleLink.setText(R.string.register_link);
                fullNameInput.setVisibility(View.GONE);
            }
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

        // validate inputs
        if (!validateInputs(email, password)) {
            return;
        }

        new Thread(() -> {
            try {
                // login to the backend server and get JWT access token
                String token = ApiClient.login(email, password);
                prefs.edit().putString("jwt_token", token).apply();
                Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                startActivity(intent);
            } catch (RuntimeException e) {
                if (e.getMessage() != null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("auth", e.getMessage());
                    });
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

        // validate inputs
        if (fullName.isEmpty()) {
            Toast.makeText(this, "Please enter full name!", Toast.LENGTH_LONG).show();
            return;
        }
        if (!validateInputs(email, password)) {
            return;
        }

        new Thread(() -> {
            try {
                // register as new user at the backend server and get JWT access token
                String token = ApiClient.register(fullName, email, password);
                prefs.edit().putString("jwt_token", token).apply();
                Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                startActivity(intent);
            } catch (RuntimeException e) {
                if (e.getMessage() != null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("auth", e.getMessage());
                    });
                }
            }
        }).start();
    }

    private boolean validateInputs(String email, String password) {
        // check for empty values
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password!", Toast.LENGTH_LONG).show();
            return false;
        }

        // validate email address
        boolean isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches();
        if (!isEmailValid) {
            Toast.makeText(this, "Email address is invalid!", Toast.LENGTH_LONG).show();
            return false;
        }

        // validate password
        boolean isValidPassword = password.length() >= 6;
        if (!isValidPassword) {
            Toast.makeText(this, "Password must be at least 6 characters long!", Toast.LENGTH_LONG).show();
            return false;
        }

        // everything is ok (=
        return true;
    }
}