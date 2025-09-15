package com.example.ai_poweredtextanalyzer.Fragments;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.ai_poweredtextanalyzer.Objects.ChatMessage;
import com.example.ai_poweredtextanalyzer.R;
import com.example.ai_poweredtextanalyzer.Utils.ApiClient;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;

public class FileChatFragment extends Fragment {
    FragmentActivity activity;
    Context context;
    String fileID;
    Button sendButton;
    EditText chatQuestionInput;
    TextView chatText;
    ScrollView chatScrollView;
    CircularProgressIndicator chatLoading;

    public FileChatFragment(FragmentActivity activity, Context context, String fileID) {
        this.activity = activity;
        this.context = context;
        this.fileID = fileID;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.file_chat_fragment_page, container, false);

        // find all views references
        findViews(view);

        // load chat history
        loadChatHistory();

        // handle send button click event
        sendButton.setOnClickListener(v -> askModel());

        return view;
    }

    /**
     * Find all views references
     */
    private void findViews(View view) {
        sendButton = view.findViewById(R.id.sendButton);
        chatQuestionInput = view.findViewById(R.id.chatQuestionInput);
        chatText = view.findViewById(R.id.chatText);
        chatScrollView = view.findViewById(R.id.chatScrollView);
        chatLoading = view.findViewById(R.id.chatLoading);
    }

    /**
     * Append a single message to the chat display.
     * @param sender - message's sender
     * @param message - message's content
     */
    private void appendMessage(String sender, String message) {
        int color = ContextCompat.getColor(context, R.color.primaryColorDark);
        SpannableString spannableString = new SpannableString(sender + ": " + message + "\n\n");
        spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, sender.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(color), 0, sender.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        chatText.append(spannableString);
    }

    /**
     * Call the backend getChatHistory API to get the chat history.
     */
    private void loadChatHistory() {
        new Thread(() -> {
            try {
                ArrayList<ChatMessage> chatMessages = ApiClient.getChatHistory(context, fileID);
                activity.runOnUiThread(() -> {
                    for (ChatMessage message : chatMessages) {
                        appendMessage(message.getSender(), message.getText());
                    }
                });
            } catch (Exception e) {
                if (e.getMessage() != null) {
                    Log.e("error", e.getMessage());
                }
                activity.runOnUiThread(() -> Toast.makeText(context, "Summary failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
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
                String answer = ApiClient.askModel(context, fileID, question);
                activity.runOnUiThread(() -> appendMessage("AI Model", answer));
            } catch (Exception e) {
                if (e.getMessage() != null) {
                    Log.e("error", e.getMessage());
                }
                activity.runOnUiThread(() -> Toast.makeText(context, "Summary failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                activity.runOnUiThread(() -> chatLoading.setVisibility(View.GONE));
            }
        }).start();
    }
}
