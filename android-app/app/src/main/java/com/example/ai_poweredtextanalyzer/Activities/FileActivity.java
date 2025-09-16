package com.example.ai_poweredtextanalyzer.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.example.ai_poweredtextanalyzer.Adapters.FilePagerAdapter;
import com.example.ai_poweredtextanalyzer.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class FileActivity extends AppCompatActivity {
    ImageButton backButton;
    TextView textFilename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);

        // get the file id and name from the intent
        String fileID = getIntent().getStringExtra("file_id");
        String filename = getIntent().getStringExtra("filename");

        // set the file name
        textFilename = findViewById(R.id.textFilename);
        textFilename.setText(filename);

        TabLayout tabLayout = findViewById(R.id.fileTabLayout);
        ViewPager2 viewPager = findViewById(R.id.fileViewPager);

        FilePagerAdapter adapter = new FilePagerAdapter(this, getBaseContext(), fileID);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    if (position == 0) tab.setText("Summary");
                    else tab.setText("Chat");
                }).attach();

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });
    }
}