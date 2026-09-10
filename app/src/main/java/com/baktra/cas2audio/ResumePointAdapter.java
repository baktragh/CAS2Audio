package com.baktra.cas2audio;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class ResumePointAdapter extends BaseAdapter {

    private ArrayList<ResumePoint> resumePoints;
    private int currentResumePointIndex;
    private int currentSelectedIndex;

    private ListView parentView;

    private Activity currentContext;

    private int selectionBackground;
    private int normalBackground;

    public ResumePointAdapter(Activity parentActivity,ListView parentView,ArrayList<ResumePoint> resumePoints,int resumePointIndex) {
        this.currentContext=parentActivity;
        this.parentView=parentView;
        this.resumePoints=resumePoints;
        this.currentResumePointIndex=resumePointIndex;
        this.currentSelectedIndex=resumePointIndex;
        retrieveColors();
    }

    @Override
    public int getCount() {
        return resumePoints.size();
    }

    @Override
    public Object getItem(int i) {
        return resumePoints.get(i);
    }

    @Override
    public long getItemId(int i) {
        return resumePoints.get(i).index;
    }

    private final void retrieveColors() {
        Resources.Theme t = currentContext.getTheme();

        TypedValue tvSelBackground = new TypedValue();
        TypedValue tvNormBackground = new TypedValue();

        /*Get selection background*/
        if (t.resolveAttribute(android.R.attr.colorAccent,tvSelBackground,true)) {
            selectionBackground=tvSelBackground.data;
        }
        else {
            selectionBackground=Color.BLACK;
        }
        /*Get normal background*/
        if (t.resolveAttribute(android.R.attr.colorBackground,tvNormBackground,true)) {
            normalBackground=tvNormBackground.data;
        }
        else {
            normalBackground=Color.TRANSPARENT;
        }

    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        TextView tv;
        if (view!=null) {
            tv=(TextView)view;
        }
        else {
            tv = new TextView(currentContext);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PT,10);
            tv.setPadding(0,2,0,2);
        }


        if (i==currentSelectedIndex) {
            tv.setBackgroundColor(selectionBackground);
        }
        else {
            tv.setBackgroundColor(normalBackground);
        }

        String resumePointText = resumePoints.get(i).toString();

        if (i==currentResumePointIndex) {
            resumePointText += "<--";
            tv.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        }
        else {
            tv.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        }

        tv.setText(resumePointText);

        return tv;

    }
    public void setSelectedIndex(int i) {
        this.currentSelectedIndex=i;
        notifyDataSetChanged();
    }

    public Object getSelectedItem() {
        if (currentSelectedIndex>=0 && currentSelectedIndex<resumePoints.size()) {
            return resumePoints.get(currentSelectedIndex);
        }
        else {
            return null;
        }
    }

    public void setResumePoint(int ip) {

        /*Fallback value*/
        currentResumePointIndex=0;

        /*Find the closest resume point*/
        for (int i=resumePoints.size()-1;i>=0;i--) {
            ResumePoint p = resumePoints.get(i);
            if (p.resumeIp<ip) {
                currentResumePointIndex=i;
                break;
            }
        }

        /*Notify for the change*/
        currentSelectedIndex=currentResumePointIndex;
        notifyDataSetChanged();

        /*Programmatic selection, scroll to two items above, when possible*/
        int selectionIndex = currentSelectedIndex-2;
        if (selectionIndex<0) selectionIndex=0;
        parentView.setSelection(selectionIndex);

    }
}
