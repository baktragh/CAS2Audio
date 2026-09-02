package com.baktra.cas2audio;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class RecentActivity extends Activity implements AdapterView.OnItemClickListener {


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

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setUI() {
        ListView lv = findViewById(R.id.lvRecentItems);
        lv.setAdapter(new ArrayAdapter<RecentItem>(this, R.layout.recent_item, recentItems));
        lv.setOnItemClickListener(this);
    }

    public void onItemClick(View view) {
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        super.onStop();
        RecentItem item = (RecentItem) adapterView.getItemAtPosition(i);
        Uri selectedUri = item.uri;
        setResult(RESULT_OK, new Intent().setData(selectedUri));
        finish();
    }
}