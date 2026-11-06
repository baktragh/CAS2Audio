package com.baktra.cas2audio.features.goldies.presentation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.baktra.cas2audio.R;
import com.baktra.cas2audio.features.goldies.domain.TapeSystem;

import java.util.List;

public class GoldiesSystemAdapter extends BaseAdapter {

   private final Context context;
   private final int resourceId;

   private final List<TapeSystem> systems;

   public GoldiesSystemAdapter(Context context, int resourceId, List<TapeSystem> systems) {
       this.context=context;
       this.resourceId=resourceId;
       this.systems= systems;
   }


    @Override
    public int getCount() {
        return systems.size();
    }

    @Override
    public Object getItem(int position) {
        return systems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public int getPosition(TapeSystem tapeSystem) {
       return systems.indexOf(tapeSystem);
    }

    @Override
    public View getView (int position,
                         View convertView,
                         ViewGroup parent) {

        View listItemView = convertView;
        if (listItemView==null) {
            listItemView = LayoutInflater.from(context).inflate(resourceId,parent,false);
        }


        TapeSystem tapeSystem = (TapeSystem)getItem(position);

        TextView tv = listItemView.findViewById(R.id.tvSystemName);
        ImageView  iv = listItemView.findViewById(R.id.ivSystemCountry);

        if (tapeSystem==null) {
            tv.setText("");
            iv.setImageDrawable(null);
        }
        else {
            tv.setText(tapeSystem.getName());
            if (tapeSystem.getCountry().equals("CS") || tapeSystem.getCountry().equals("CZ")) {
                iv.setImageResource(R.drawable.flag_cs);
            }
            else if (tapeSystem.getCountry().equals("US")) {
                iv.setImageResource(R.drawable.flag_us);
            }
            else if (tapeSystem.getCountry().equals("PL")) {
                iv.setImageResource(R.drawable.flag_pl);
            }
            else if (tapeSystem.getCountry().equals("XX")) {
                iv.setImageResource(R.drawable.flag_xx);
            }
            else {
                iv.setImageDrawable(null);
            }
        }

        return listItemView;
    }
}
