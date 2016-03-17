package com.x.otg_ddmo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Leon on 2015/8/4 0004.
 */
public class BaseStringAdapter extends BaseAdapter {
    private Context context;
    private List<String> list;
    public BaseStringAdapter(Context context){
        this.context = context;
        list = new ArrayList<String>();
    }

    public void add(String info){
        list.add(0,info);
        if(list.size()>20){
            list.remove(20);
        }
        notifyDataSetChanged();
    }

    public void clear(){
        list.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public String getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (convertView == null) {
            view =  LayoutInflater.from(context).inflate(
                    R.layout.layout, null);
        } else {
            view =  LayoutInflater.from(context).inflate(
                    R.layout.layout, null);
        }
        TextView textView1 = (TextView) view.findViewById(R.id.textView3);
        textView1.setText(getItem(position));
        return view;
    }
}
