package com.baktra.cas2audio.features.goldies;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewTreeLifecycleOwner;

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

                ImageButton btnRefresh = (ImageButton)findViewById(R.id.btnRefresh);

                /*For an empty state, display just everything empty.
                * The user can select the refresh button to download the archive*/
                if (newState.getMainState() == GoldiesViewModel.MainState.EMPTY) {

                    String[] itemsArr = new String[0];
                    TapeSystem[] systemsArr = new TapeSystem[0];

                    lv.setAdapter(new ArrayAdapter<String>(GoldiesActivity.this, R.layout.goldies_item, itemsArr));
                    spSystems.setAdapter(new ArrayAdapter<TapeSystem>(GoldiesActivity.this, R.layout.goldies_item, systemsArr));

                    lv.setEnabled(false);
                    spSystems.setEnabled(false);
                    btnRefresh.setEnabled(true);

                    setMessageBarVisible(false,false,null);

                    return;
                }

                /*Downloading ZIP. Transitional state to display something when the file is being
                  downloaded. The user controls are not active, because there is not much to do
                  during the download anyway.
                 */
                else if (newState.getMainState() == GoldiesViewModel.MainState.ZIP_DOWNLOADING) {

                    String[] itemsArr = new String[0];
                    TapeSystem[] systemsArr = new TapeSystem[0];

                    lv.setAdapter(new ArrayAdapter<String>(GoldiesActivity.this, R.layout.goldies_item, itemsArr));
                    spSystems.setAdapter(new ArrayAdapter<TapeSystem>(GoldiesActivity.this, R.layout.goldies_item, systemsArr));

                    lv.setEnabled(false);
                    spSystems.setEnabled(false);
                    btnRefresh.setActivated(false);

                    setMessageBarVisible(true,false,getString(R.string.goldies_downloading));

                    return;
                }

                /*If the download failed, display a message with a "Dismiss" button. Clicking the dismiss
                * button will acknowledge the message and force re-read of the existing archive*/
                else if (newState.getMainState() == GoldiesViewModel.MainState.ZIP_DOWNLOAD_FAILED) {

                    String[] itemsArr = new String[0];
                    TapeSystem[] systemsArr = new TapeSystem[0];

                    String msg = getString(R.string.goldies_zip_download_failed) +
                            "\n" +
                            Utils.getExceptionMessage(newState.getFailureException());

                    lv.setAdapter(new ArrayAdapter<String>(GoldiesActivity.this, R.layout.goldies_item, itemsArr));
                    spSystems.setAdapter(new ArrayAdapter<TapeSystem>(GoldiesActivity.this, R.layout.goldies_item, systemsArr));

                    lv.setEnabled(false);
                    spSystems.setEnabled(false);
                    btnRefresh.setEnabled(false);
                    setMessageBarVisible(true,true,msg);

                    return;
                }

                /*If the ZIP file is not present, or corrupted, just display an error message. The
                * refresh button is enabled, so that the ZIP can be re-downloaded*/
                else if (newState.getMainState() == GoldiesViewModel.MainState.ZIP_NOT_PRESENT_OR_CORRUPT) {
                    String[] itemsArr = new String[0];
                    TapeSystem[] systemsArr = new TapeSystem[0];


                    String msg = getString(R.string.goldies_zip_absent_or_corrupt) +
                                "\n"+
                                Utils.getExceptionMessage(newState.getFailureException());

                    lv.setAdapter(new ArrayAdapter<String>(GoldiesActivity.this, R.layout.goldies_item, itemsArr));
                    spSystems.setAdapter(new ArrayAdapter<TapeSystem>(GoldiesActivity.this, R.layout.goldies_item, systemsArr));

                    lv.setEnabled(false);
                    spSystems.setEnabled(false);
                    btnRefresh.setEnabled(true);
                    setMessageBarVisible(true,false,msg);

                    return;
                }

                /*Most common state, we have the archive, which we have successfully read,
                now we display the list of tape images available for the selected system
                 */
                else if (newState.getMainState()== GoldiesViewModel.MainState.LIST_POPULATED) {

                    /*The state is */
                    String[] itemsArr = new String[0];
                    itemsArr = newState.getCurrentItems().toArray(itemsArr);
                    lv.setAdapter(new ArrayAdapter<String>(GoldiesActivity.this, R.layout.goldies_item, itemsArr));
                    lv.setEnabled(true);

                    /*Redisplay the Systems, but only when the list has changed*/
                    if (!isSystemListSame(spSystems, newState.getTapeSystems())) {
                        TapeSystem[] systemsArr = new TapeSystem[0];
                        systemsArr = newState.getTapeSystems().toArray(systemsArr);

                        ArrayAdapter<TapeSystem> systemAdapter = new ArrayAdapter<TapeSystem>(GoldiesActivity.this, R.layout.goldies_item, systemsArr);
                        spSystems.setAdapter(systemAdapter);
                        int selPosition = systemAdapter.getPosition(newState.getCurrentTapeSystem());
                        spSystems.setSelection(selPosition);
                    }

                    spSystems.setEnabled(true);
                    btnRefresh.setEnabled(true);

                    spSystems.setOnItemSelectedListener(GoldiesActivity.this);
                    lv.setOnItemClickListener(GoldiesActivity.this);


                    setMessageBarVisible(false, false, null);
                }

                /*Unknown state (should never happen)*/
                else {
                    String[] itemsArr = new String[0];
                    TapeSystem[] systemsArr = new TapeSystem[0];

                    lv.setAdapter(new ArrayAdapter<String>(GoldiesActivity.this, R.layout.goldies_item, itemsArr));
                    spSystems.setAdapter(new ArrayAdapter<TapeSystem>(GoldiesActivity.this, R.layout.goldies_item, systemsArr));

                    lv.setEnabled(false);
                    spSystems.setEnabled(false);
                    btnRefresh.setEnabled(false);

                    setMessageBarVisible(false,false,"Internal error: Unknown state");
                }
            }
        });
    }

    private void setMessageBarVisible(boolean visible,boolean dismissButtonVisible,String message) {

        TextView tvMessage = (TextView)findViewById(R.id.tvMessage);
        LinearLayout llMessage = (LinearLayout)findViewById(R.id.llMessage);
        Button btnDismiss = (Button)findViewById(R.id.btnDismiss);

        if (!visible) {
            tvMessage.setVisibility(View.GONE);
            btnDismiss.setVisibility(View.GONE);
            llMessage.setVisibility(View.GONE);
            tvMessage.setText("");
        }
        else {
            tvMessage.setVisibility(View.VISIBLE);
            llMessage.setVisibility(View.VISIBLE);
            tvMessage.setText(message);
            if (dismissButtonVisible) {
                btnDismiss.setVisibility(View.VISIBLE);
            }
            else {
                btnDismiss.setVisibility(View.GONE);
            }

        }



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

    public void onMessageDismiss(View v) {
        viewModel.reDisplay();
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
