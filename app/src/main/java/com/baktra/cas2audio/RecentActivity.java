package com.baktra.cas2audio;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

public class RecentActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private TapeImageRecents localRecents;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent);
    }

    protected void onResume() {
        super.onResume();
        localRecents = new TapeImageRecents();
        localRecents.parsePersistenceString((String) getIntent().getSerializableExtra("recent_items"));
        setUI();
    }

    protected void onPause() {
        closeOptionsMenu();
        super.onPause();
    }

    protected void onDestroy() {
        closeOptionsMenu();
        super.onDestroy();
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.recent_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK, createResultIntent(null));
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setUI() {
        ListView lv = findViewById(R.id.lvRecentItems);
        lv.setAdapter(new ArrayAdapter<RecentItem>(this, R.layout.recent_item, localRecents.getAsArray()));
        lv.setOnItemClickListener(this);
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        super.onStop();
        RecentItem item = (RecentItem) adapterView.getItemAtPosition(i);
        Uri selectedUri = item.uri;
        setResult(RESULT_OK, createResultIntent(selectedUri));
        finish();
    }

    public void onClearRecents(MenuItem menuItem) {
        localRecents.clear();
        setUI();
    }

    private Intent createResultIntent(Uri selectedUri) {
        Intent i = new Intent();
        i.setData(selectedUri);
        i.putExtra("recents", localRecents.createPersistenceString());
        return i;
    }

}