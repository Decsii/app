package com.example.balint.szakdolgozat.javaclasses;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.balint.szakdolgozat.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Balint on 2015.09.22..
 */
public class FriendListAdapter extends BaseAdapter {

    private LayoutInflater layoutInflater;
    private List<FriendListItem> fli;

    public FriendListAdapter(Activity activity) {
        layoutInflater = activity.getLayoutInflater();
        fli = new ArrayList<>();
    }

    public void addFriend(FriendListItem e) {
        fli.add(e);
        notifyDataSetChanged();
    }

    public void clearList() {
        fli.clear();
    }

    @Override
    public int getCount() {
        return fli.size();
    }

    @Override
    public Object getItem(int i) {
        return fli.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int i) {
        return fli.get(i).getType();
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.friend_list_view, viewGroup, false);
        }

        //String name = fli.get(i).getName();

        TextView txtMessage = (TextView) convertView.findViewById(R.id.nameText);
        txtMessage.setText(fli.get(i).getName());

        TextView txtMessage2 = (TextView) convertView.findViewById(R.id.lastMsgText);
        txtMessage2.setText("");

        TextView txtMessage3 = (TextView) convertView.findViewById(R.id.timeText);
        txtMessage3.setText("");

        return convertView;
    }
}

