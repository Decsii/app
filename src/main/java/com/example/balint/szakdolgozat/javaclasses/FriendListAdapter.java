package com.example.balint.szakdolgozat.javaclasses;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.balint.szakdolgozat.R;
import com.example.balint.szakdolgozat.activities.FriendListActivity;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by Balint on 2015.09.22..
 */
public class FriendListAdapter extends BaseAdapter {

    private LayoutInflater layoutInflater;
    private List<FriendListItem> fli;
    Activity context;

    public FriendListAdapter(Activity activity) {
        context = activity;
        layoutInflater = activity.getLayoutInflater();
        fli = new ArrayList<>();
    }

    public void addFriend(FriendListItem e) {
        fli.add(e);
        notifyDataSetChanged();
    }

    public void refreshFriend(Pair<Integer,FriendListItem> pair){
        fli.set(pair.first,pair.second);
        notifyDataSetChanged();
    };

    public long getLastActive(int i){
        return fli.get(i).getLast_login();
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
        txtMessage.setText( fli.get(i).getName() );
        if( fli.get(i).isLoggedin() ) {
            txtMessage.setTypeface(null, Typeface.BOLD);
        }
        TextView txtMessage2 = (TextView) convertView.findViewById(R.id.lastMsgText);
        if ( fli.get(i).getLstMsg() == "" ){
            txtMessage2.setText("Nincs üzenet");
        }else{
            txtMessage2.setText(fli.get(i).getLstMsg());
        }

        TextView txtMessage3 = (TextView) convertView.findViewById(R.id.timeText);
        txtMessage3.setText(fli.get(i).getTime());

        //if( fli.get(i).getUserid() == 1055 ){
        //    RoundedImageView p = (RoundedImageView) convertView.findViewById(R.id.imgView);
        //    Drawable d =  ContextCompat.getDrawable(context, R.drawable.pet);
        //    p.setImageDrawable(d);
        //}

        return convertView;
    }
}

