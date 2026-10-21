package com.baktra.cas2audio.goldies;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.baktra.cas2audio.Cas2AudioApp;
import com.baktra.cas2audio.R;

import java.util.List;

public class GoldiesActivity extends AppCompatActivity {

    private GoldiesViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goldies);

        Cas2AudioApp c2aApp = (Cas2AudioApp)getApplication();
        GoldiesViewModelFactory factory = new GoldiesViewModelFactory(c2aApp.getGoldiesRepository());


        viewModel = new ViewModelProvider(this,factory).get(GoldiesViewModel.class);
        setupModelObservers();

        viewModel.reDisplay();

    }


    private void setupModelObservers() {
        /*Chunk list visibility*/
        viewModel.getUIState().observe(this, new Observer<GoldiesViewModel.UIState>() {
            @Override
            public void onChanged(GoldiesViewModel.UIState newState) {
                ListView lv = (ListView)findViewById(R.id.lvGoldies);

                /*Redisplay the items, if any*/
                List<String> currentItems = newState.getCurrentItems();

                System.out.println(currentItems);


                if (currentItems != null) {
                    String[] arr = new String[0];
                    arr = newState.getCurrentItems().toArray(arr);
                    lv.setAdapter(new ArrayAdapter<String>(GoldiesActivity.this,R.layout.goldies_item,arr));
                }
                else {
                    String[] arr = new String[0];
                    lv.setAdapter(new ArrayAdapter<String>(GoldiesActivity.this,R.layout.goldies_item,arr));
                }

            }
        });
    }


}
