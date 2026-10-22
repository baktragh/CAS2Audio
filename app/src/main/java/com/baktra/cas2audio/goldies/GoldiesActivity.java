package com.baktra.cas2audio.goldies;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.baktra.cas2audio.Cas2AudioApp;
import com.baktra.cas2audio.R;

import java.util.List;

public class GoldiesActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private GoldiesViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goldies);

        Cas2AudioApp c2aApp = (Cas2AudioApp)getApplication();
        GoldiesViewModelFactory factory = new GoldiesViewModelFactory(c2aApp.getGoldiesRepository(), c2aApp.getSettingsRepository());

        setTitle(getString(R.string.goldies));


        viewModel = new ViewModelProvider(this,factory).get(GoldiesViewModel.class);
        setupModelObservers();

        viewModel.reDisplay();

    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK, createResultIntent());
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupModelObservers() {
        /*Chunk list visibility*/
        viewModel.getUIState().observe(this, new Observer<GoldiesViewModel.UIState>() {
            @Override
            public void onChanged(GoldiesViewModel.UIState newState) {

                ListView lv = (ListView) findViewById(R.id.lvGoldies);
                Spinner spSystems = (Spinner) findViewById(R.id.spSystems);

                /*For an empty state, display just everything empty*/
                if (newState.getMainState() == GoldiesViewModel.MainState.EMPTY) {
                    String[] itemsArr = new String[0];
                    TapeSystem[] systemsArr = new TapeSystem[0];

                    lv.setAdapter(new ArrayAdapter<String>(GoldiesActivity.this, R.layout.goldies_item, itemsArr));
                    spSystems.setAdapter(new ArrayAdapter<TapeSystem>(GoldiesActivity.this, R.layout.goldies_item, systemsArr));

                    lv.setEnabled(false);
                    spSystems.setEnabled(false);

                    return;
                }
                /*For an error state, just display one item holding the error message*/
                else if (newState.getMainState() == GoldiesViewModel.MainState.ZIP_DOWNLOAD_FAILED || newState.getMainState() == GoldiesViewModel.MainState.ZIP_NOT_PRESENT) {
                    String[] itemsArr = new String[1];
                    TapeSystem[] systemsArr = new TapeSystem[0];
                    itemsArr[0] = "Goldies archive not present. Select refresh to re-download";

                    lv.setAdapter(new ArrayAdapter<String>(GoldiesActivity.this, R.layout.goldies_item, itemsArr));
                    spSystems.setAdapter(new ArrayAdapter<TapeSystem>(GoldiesActivity.this, R.layout.goldies_item, systemsArr));

                    lv.setEnabled(false);
                    spSystems.setEnabled(false);

                    return;
                }

                /*Redisplay the Goldies, if any*/
                String[] itemsArr = new String[0];
                itemsArr = newState.getCurrentItems().toArray(itemsArr);
                lv.setAdapter(new ArrayAdapter<String>(GoldiesActivity.this, R.layout.goldies_item, itemsArr));
                lv.setEnabled(true);

                /*Redisplay the Systems, but only when the list has changed*/
                if (!isSystemListSame(spSystems,newState.getTapeSystems())) {
                    TapeSystem[] systemsArr = new TapeSystem[0];
                    systemsArr = newState.getTapeSystems().toArray(systemsArr);

                    ArrayAdapter<TapeSystem> systemAdapter = new ArrayAdapter<TapeSystem>(GoldiesActivity.this, R.layout.goldies_item, systemsArr);
                    spSystems.setAdapter(systemAdapter);
                    int selPosition = systemAdapter.getPosition(newState.getCurrentTapeSystem());
                    spSystems.setSelection(selPosition);
                }
                spSystems.setEnabled(true);
                spSystems.setOnItemSelectedListener(GoldiesActivity.this);
            }
        });
    }

    private boolean isSystemListSame(Spinner s,List<TapeSystem> newList) {

        if (s.getAdapter()==null) return false;

        int newLen = newList.size();
        int curLen = s.getAdapter().getCount();

        if (newLen!=curLen) return false;

        for(int i=0;i<curLen;i++) {
            TapeSystem tLeft = (TapeSystem)s.getAdapter().getItem(i);
            TapeSystem tRight = newList.get(i);
            if (!tLeft.equals(tRight)) return false;
        }

        return true;

    }


    private Intent createResultIntent() {
        Intent i = new Intent();
        i.setData(null);
        return i;
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent==findViewById(R.id.spSystems)) {
            viewModel.updateCurrentTapeSystem((TapeSystem)parent.getSelectedItem());
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
