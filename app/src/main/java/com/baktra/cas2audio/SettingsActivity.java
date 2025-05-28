package com.baktra.cas2audio;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class SettingsActivity extends AppCompatActivity {

    public SettingsActivity() {
        super();
    }

    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }

    protected final void onResume() {
    }

    protected final void onStop() {
        super.onStop();
    }

    protected final void onDestroy() {
        super.onDestroy();
    }

    private final void setUI() {

    }

    private final void flushUIToSettings() {

    }


}
