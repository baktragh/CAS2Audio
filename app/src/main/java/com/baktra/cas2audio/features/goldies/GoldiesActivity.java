package com.baktra.cas2audio.features.goldies;

import android.content.Intent;
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
import com.baktra.cas2audio.core.util.Utils;

import java.util.List;

public class GoldiesActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener,AdapterView.OnItemClickListener {

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
                setResult(RESULT_CANCELED, createResultIntent(null,null));
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
                else if (newState.getMainState() == GoldiesViewModel.MainState.ZIP_DOWNLOAD_FAILED || newState.getMainState() == GoldiesViewModel.MainState.ZIP_NOT_PRESENT_OR_CORRUPT) {
                    String[] itemsArr = new String[2];
                    TapeSystem[] systemsArr = new TapeSystem[0];

                    if (newState.getMainState()== GoldiesViewModel.MainState.ZIP_DOWNLOAD_FAILED) {
                        itemsArr[0] = getString(R.string.goldies_zip_download_failed);
                        itemsArr[1] = Utils.getExceptionMessage(newState.getFailureException());
                    }
                    else {
                        itemsArr[0] = getString(R.string.goldies_zip_absent_or_corrupt);
                        itemsArr[1] = Utils.getExceptionMessage(newState.getFailureException());
                    }

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
                lv.setOnItemClickListener(GoldiesActivity.this);
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

    private Intent createResultIntent(TapeSystem system,String tapeImageName) {
        Intent i = new Intent();
        if (system==null || tapeImageName==null) return i;
        int[] tapeImageData = viewModel.getTapeImageData(system,tapeImageName);
        if (tapeImageData==null) return i;

        i.putExtra("tape_image_data",tapeImageData);
        i.putExtra("tape_image_name",tapeImageName);
        return i;
    }

    public void onRefresh(View v) {
        viewModel.initiateRefresh();
    }



    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        //System.out.println("onItemSelected()");

        if (parent==findViewById(R.id.spSystems)) {
            viewModel.updateCurrentTapeSystem((TapeSystem)parent.getSelectedItem());
        }


    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        //System.out.println("onItemClick()");

        if (parent==findViewById(R.id.lvGoldies)) {

            ListView lv = (ListView)findViewById(R.id.lvGoldies);
            Spinner sp = (Spinner)findViewById(R.id.spSystems);

            Intent i = createResultIntent((TapeSystem)sp.getSelectedItem(),(String)lv.getItemAtPosition(position));
            setResult(RESULT_OK,i);
            finish();
        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
