package com.example.ai_poweredtextanalyzer.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.ai_poweredtextanalyzer.Activities.FileActivity;
import com.example.ai_poweredtextanalyzer.Objects.File;
import com.example.ai_poweredtextanalyzer.R;

import java.util.ArrayList;

public class FileAdapter extends BaseAdapter {
    private final Context context;
    private final ArrayList<File> files;

    public FileAdapter(Context context, ArrayList<File> files) {
        this.context = context;
        this.files = files;
    }

    @Override
    public int getCount() {
        return files.size();
    }

    @Override
    public Object getItem(int i) {
        return files.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.file_grid_item, parent, false);
        }

        File file = files.get(position);

        TextView textFilename = convertView.findViewById(R.id.textFilename);
        TextView textUploadedAt = convertView.findViewById(R.id.textUploadedAt);

        textFilename.setText(file.getFilename());
        textUploadedAt.setText(file.getUploadedAt());

        // handle grid item click event
        convertView.setOnClickListener(v -> {
            Intent intent = new Intent(context, FileActivity.class);
            intent.putExtra("file_id", file.getId());
            intent.putExtra("filename", file.getFilename());
            context.startActivity(intent);
        });

        return convertView;
    }
}
