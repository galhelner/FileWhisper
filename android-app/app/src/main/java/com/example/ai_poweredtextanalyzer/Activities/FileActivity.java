package com.example.ai_poweredtextanalyzer.Activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.ai_poweredtextanalyzer.Utils.ApiClient;
import com.example.ai_poweredtextanalyzer.Objects.ChatMessage;
import com.example.ai_poweredtextanalyzer.R;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.util.ArrayList;

public class FileActivity extends AppCompatActivity {
    private String fileId;
    private TextView textFilename;
    private ImageButton backButton;
    private Spinner summaryStyleSpinner;
    private Spinner summaryLengthSpinner;
    private Button summaryButton;
    private ImageButton sendButton;
    private TextView summaryText;
    private TextView chatText;
    private CircularProgressIndicator summaryLoading;
    private CircularProgressIndicator chatLoading;
    private EditText chatQuestionInput;
    private ScrollView chatScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_file);

        // get the file id and name from the intent
        fileId = getIntent().getStringExtra("file_id");
        String filename = getIntent().getStringExtra("filename");

        // find views references
        findViews();

        // load style and length spinners dataset
        loadSpinnersData();

        // load chat history
        loadChatHistory();

        // set the file name title
        textFilename.setText(filename);

        // handle back button click event
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(FileActivity.this, MainActivity.class);
            startActivity(intent);
        });

        // handle summary button click event
        summaryButton.setOnClickListener(v -> summary());

        // handle chat send button click event
        sendButton.setOnClickListener(v -> askModel());
    }

    /**
     * Find all views references
     */
    private void findViews() {
        textFilename = findViewById(R.id.textFilename);
        backButton = findViewById(R.id.backButton);
        summaryStyleSpinner = findViewById(R.id.summaryStyleSpinner);
        summaryLengthSpinner = findViewById(R.id.summaryLengthSpinner);
        summaryButton = findViewById(R.id.summaryButton);
        sendButton = findViewById(R.id.sendButton);
        summaryText = findViewById(R.id.summaryText);
        chatText = findViewById(R.id.chatText);
        summaryLoading = findViewById(R.id.summaryLoading);
        chatLoading = findViewById(R.id.chatLoading);
        chatQuestionInput = findViewById(R.id.chatQuestionInput);
        chatScrollView = findViewById(R.id.chatScrollView);
    }

    /**
     * Load the style and length spinners option strings.
     */
    private void loadSpinnersData() {
        ArrayAdapter<CharSequence> stylesAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.styles_array,
                android.R.layout.simple_spinner_item
        );

        ArrayAdapter<CharSequence> lengthAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.length_array,
                android.R.layout.simple_spinner_item
        );

        stylesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lengthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        summaryStyleSpinner.setAdapter(stylesAdapter);
        summaryLengthSpinner.setAdapter(lengthAdapter);
    }

    /**
     * Call the backend getChatHistory API to get the chat history.
     */
    private void loadChatHistory() {
        new Thread(() -> {
            try {
                ArrayList<ChatMessage> chatMessages = ApiClient.getChatHistory(this, fileId);
                runOnUiThread(() -> {
                    for (ChatMessage message : chatMessages) {
                        appendMessage(message.getSender(), message.getText());
                    }
                });
            } catch (Exception e) {
                if (e.getMessage() != null) {
                    Log.e("error", e.getMessage());
                }
                runOnUiThread(() -> Toast.makeText(this, "Summary failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /**
     * Call the backend summarizeFile API to get a summary for the file content.
     */
    private void summary() {
        summaryText.setVisibility(View.GONE);
        summaryLoading.setVisibility(View.VISIBLE);
        String style = summaryStyleSpinner.getSelectedItem().toString().toLowerCase();
        String length = summaryLengthSpinner.getSelectedItem().toString().toLowerCase();
        new Thread(() -> {
            try {
                String summary = ApiClient.summarizeFile(this, fileId, style, length);
                runOnUiThread(() -> {
                    summaryLoading.setVisibility(View.GONE);
                    summaryText.setText(summary);
                    summaryText.setVisibility(View.VISIBLE);
                });
            } catch (Exception e) {
                if (e.getMessage() != null) {
                    Log.e("error", e.getMessage());
                }
                runOnUiThread(() -> Toast.makeText(this, "Summary failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /**
     * Append a single message to the chat display.
     * @param sender - message's sender
     * @param message - message's content
     */
    private void appendMessage(String sender, String message) {
        int color = ContextCompat.getColor(this, R.color.primaryColorDark);
        SpannableString spannableString = new SpannableString(sender + ": " + message + "\n\n");
        spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, sender.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(color), 0, sender.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        chatText.append(spannableString);
    }

    /**
     * Call the backend askModel API to ask the AI Model a question about the file.
     */
    private void askModel() {
        String question = chatQuestionInput.getText().toString();
        appendMessage("Me", question);
        chatQuestionInput.setText("");

        chatLoading.setVisibility(View.VISIBLE);
        chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));

        new Thread(() -> {
            try {
                String answer = ApiClient.askModel(this, fileId, question);
                runOnUiThread(() -> {
                    appendMessage("AI Model", answer);
                    chatLoading.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                if (e.getMessage() != null) {
                    Log.e("error", e.getMessage());
                }
                runOnUiThread(() -> Toast.makeText(this, "Summary failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}