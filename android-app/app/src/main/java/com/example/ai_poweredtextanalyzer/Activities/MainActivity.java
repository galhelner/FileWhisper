package com.example.ai_poweredtextanalyzer.Activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ai_poweredtextanalyzer.Utils.ApiClient;
import com.example.ai_poweredtextanalyzer.Objects.File;
import com.example.ai_poweredtextanalyzer.Adapters.FileAdapter;
import com.example.ai_poweredtextanalyzer.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private ImageButton profileButton;
    private GridView myFilesGrid;
    private FileAdapter adapter;
    private FloatingActionButton uploadFileButton;
    private ActivityResultLauncher<Intent> filePickerLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // check if user authenticated
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        checkAuthentication();

        // find all views references
        findViews();

        // load my files from backend server
        loadMyFiles();

        // initialize the file picker
        initFilePicker();

        // handle profile button click event
        profileButton.setOnClickListener(this::openProfilePopup);

        // handle upload new file button
        uploadFileButton.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.upload_file)
                .setMessage(R.string.upload_file_message)
                .setPositiveButton(R.string.choose_file, (dialog, which) -> openFilePicker())
                .show());
    }

    private void openProfilePopup(View view) {
        // Inflate popup layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        ViewGroup root = findViewById(R.id.main);
        View popupView = inflater.inflate(R.layout.profile_popup, root, false);
        Button logoutButton = popupView.findViewById(R.id.logoutButton);

        // Create PopupWindow
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // Add shadow/elevation
        popupWindow.setElevation(20);

        // Show popup anchored to the clicked view
        popupWindow.showAsDropDown(view, 0, 10);

        // handle logout button click event
        logoutButton.setOnClickListener(v -> {
            // remove access token from shared preferences
            prefs.edit().remove("jwt_token").apply();

            // redirect to auth activity
            Intent intent = new Intent(MainActivity.this, AuthActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Check if the user is already authenticated.
     */
    private void checkAuthentication() {
        String savedToken = prefs.getString("jwt_token", null);
        if (savedToken == null) {
            // user isn't authenticated, redirect to auth activity
            Intent intent = new Intent(MainActivity.this, AuthActivity.class);
            startActivity(intent);
        }
    }

    /**
     * Find all views references.
     */
    private void findViews() {
        profileButton = findViewById(R.id.profileButton);
        myFilesGrid = findViewById(R.id.myFilesGrid);
        uploadFileButton = findViewById(R.id.uploadFileButton);
    }

    /**
     * Call the backend getMyFiles API to get all the user's uploaded files.
     */
    private void loadMyFiles() {
        new Thread(() -> {
            try {
                ArrayList<File> myFiles = ApiClient.getMyFiles(getApplicationContext());
                adapter = new FileAdapter(this, myFiles);

                runOnUiThread(() -> myFilesGrid.setAdapter(adapter));

            } catch (RuntimeException e) {
                if (e.getMessage() != null) {
                    Log.e("error", e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Initialize the file picker.
     */
    private void initFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri fileUri = data.getData();
                            String fileName = getFileName(fileUri);
                            Toast.makeText(this, "Selected: " + fileName, Toast.LENGTH_SHORT).show();

                            // upload the file to the backend server
                            uploadFile(fileUri, fileName);
                        }
                    }
                }
        );
    }

    /**
     * Opens the file picker to allow the user pick a file to upload.
     */
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // any type, filter with MIME types
        String[] mimeTypes = {"text/plain", "application/pdf"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        filePickerLauncher.launch(Intent.createChooser(intent, "Select File"));
    }

    /**
     * Helper function to extract only the uploaded file name.
     * @param uri - uri of the uploaded file.
     * @return The name of the uploaded file.
     */
    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        String result = null;
        if (Objects.equals(uri.getScheme(), "content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    /**
     * Call the backend uploadFile API to upload a new file.
     * @param fileUri - uri of the file to upload.
     * @param fileName - name of the file to upload.
     */
    private void uploadFile(Uri fileUri, String fileName) {
        new Thread(() -> {
            try {
                ApiClient.uploadFile(getApplicationContext(), fileUri, fileName);
                runOnUiThread(this::loadMyFiles);
            } catch (Exception e) {
                if (e.getMessage() != null) {
                    Log.e("error", e.getMessage());
                }
                runOnUiThread(() -> Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }


}