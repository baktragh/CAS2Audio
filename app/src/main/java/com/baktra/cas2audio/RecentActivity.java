package com.baktra.cas2audio;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class RecentActivity extends Activity {


    private ArrayList<RecentItem> recentItems = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent);
    }

    protected final void onResume() {
        super.onResume();
        recentItems = new ArrayList<>();
        RecentItem.parsePersistenceString((String)getIntent().getSerializableExtra("recent_items"),recentItems);
        setUI();
    }

    private void setUI() {
        ListView lv = findViewById(R.id.lvRecentItems);
        lv.setAdapter(new ArrayAdapter<RecentItem>(this, R.layout.recent_item, recentItems));
    }

}