package com.example.ai_poweredtextanalyzer.Adapters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.ai_poweredtextanalyzer.Fragments.FileChatFragment;
import com.example.ai_poweredtextanalyzer.Fragments.FileSummaryFragment;

public class FilePagerAdapter extends FragmentStateAdapter {
    FragmentActivity activity;
    Context context;
    String fileID;

    public FilePagerAdapter(@NonNull FragmentActivity fa, Context context, String fileID) {
        super(fa);
        this.activity = fa;
        this.context = context;
        this.fileID = fileID;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) return new FileSummaryFragment(activity, context, fileID);
        else return new FileChatFragment(activity, context, fileID);
    }

    @Override
    public int getItemCount() {
        return 2; // number of tabs
    }
}
