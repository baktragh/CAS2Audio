package com.baktra.cas2audio;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private CasTask casTask;

    public MainActivity() {
        super();
        casTask=null;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    protected void onResume() {

        super.onResume();
        Intent intent = getIntent();
        Uri u = intent.getData();

        if (u!=null) {
            String filename = new File(u.getPath()).getName();
            ((TextView) findViewById(R.id.textView)).setText(filename);
        }
        else {
            ((TextView) findViewById(R.id.textView)).setText("CAS2Audio");
        }
    }

    protected void onStop() {
        super.onStop();

    }

    public void onPlay(View v) {

        try {
            casTask = new CasTask(getContentResolver().openInputStream(getIntent().getData()),findViewById(R.id.btnPlay),findViewById(R.id.btnStop));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        casTask.execute();

    }

    public void onStopPlaying(View v) {
        if (casTask!=null) {
            casTask.cancel(true);
        }
    }



}
