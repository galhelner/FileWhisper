package com.example.ai_poweredtextanalyzer.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import com.example.ai_poweredtextanalyzer.R;
import com.example.ai_poweredtextanalyzer.Utils.ApiClient;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class FileSummaryFragment  extends Fragment {
    FragmentActivity activity;
    Context context;
    String fileID;
    Spinner spinnerStyle;
    Spinner spinnerLength;
    Button summaryButton;
    TextView summaryText;
    CircularProgressIndicator summaryLoading;

    public FileSummaryFragment(FragmentActivity activity, Context context, String fileID) {
        this.activity = activity;
        this.context = context;
        this.fileID = fileID;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.file_summary_fragment_page, container, false);

        findViews(view);

        loadSpinnersData();

        // handle summary button click event
        summaryButton.setOnClickListener(v -> summary());

        return view;
    }

    /**
     * Find all views references
     */
    private void findViews(View view) {
        spinnerStyle = view.findViewById(R.id.spinnerStyle);
        spinnerLength = view.findViewById(R.id.spinnerLength);
        summaryButton = view.findViewById(R.id.summaryButton);
        summaryText = view.findViewById(R.id.summaryText);
        summaryLoading = view.findViewById(R.id.summaryLoading);
    }

    /**
     * Load the style and length spinners option strings.
     */
    private void loadSpinnersData() {
        ArrayAdapter<CharSequence> stylesAdapter = ArrayAdapter.createFromResource(
                context,
                R.array.styles_array,
                android.R.layout.simple_spinner_item
        );

        ArrayAdapter<CharSequence> lengthAdapter = ArrayAdapter.createFromResource(
                context,
                R.array.length_array,
                android.R.layout.simple_spinner_item
        );

        stylesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lengthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerStyle.setAdapter(stylesAdapter);
        spinnerLength.setAdapter(lengthAdapter);
    }

    /**
     * Call the backend summarizeFile API to get a summary for the file content.
     */
    private void summary() {
        summaryText.setVisibility(View.GONE);
        summaryLoading.setVisibility(View.VISIBLE);
        String style = spinnerStyle.getSelectedItem().toString().toLowerCase();
        String length = spinnerLength.getSelectedItem().toString().toLowerCase();
        new Thread(() -> {
            try {
                String summary = ApiClient.summarizeFile(context, fileID, style, length);
                activity.runOnUiThread(() -> {
                    summaryLoading.setVisibility(View.GONE);
                    summaryText.setText(summary);
                    summaryText.setVisibility(View.VISIBLE);
                });
            } catch (Exception e) {
                if (e.getMessage() != null) {
                    Log.e("error", e.getMessage());
                }
                activity.runOnUiThread(() -> Toast.makeText(context, "Summary failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                activity.runOnUiThread(() -> summaryLoading.setVisibility(View.GONE));
            }
        }).start();
    }
}
