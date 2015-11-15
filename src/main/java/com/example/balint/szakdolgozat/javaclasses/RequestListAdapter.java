package com.example.balint.szakdolgozat.javaclasses;

import android.app.Activity;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.example.balint.szakdolgozat.R;
import com.example.balint.szakdolgozat.activities.TCPService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Balint on 2015.11.08..
 */
public class RequestListAdapter extends BaseAdapter {

    private LayoutInflater layoutInflater;
    private List<FriendListItem> fli;
    Activity context;
    TCPService tcps;

    public RequestListAdapter(Activity activity, TCPService tcps) {
        this.context = activity;
        layoutInflater = activity.getLayoutInflater();
        fli = new ArrayList<>();
        this.tcps = tcps;
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
        return 6;
    }

    @Override
    public int getItemViewType(int i) {
        return fli.get(i).getType();
    }

    private View.OnClickListener racceptA;
    private View.OnClickListener rdeclineA;
    private View.OnClickListener rundorA;

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        TextView txtMessage;
        switch (fli.get(i).getType()) {
            case 1:
                //request
                if (convertView == null) {
                    convertView = layoutInflater.inflate(R.layout.request_list_item, viewGroup, false);
                }
                txtMessage = (TextView) convertView.findViewById(R.id.requestText);
                txtMessage.setText(fli.get(i).getName());

                Button btn = (Button) convertView.findViewById(R.id.racceptB);
                racceptA = new MyOnClickListener(fli.get(i).getUserid(),fli.get(i).getName()) {
                    public void onClick(View v) {
                        tcps.acceptRequest(this.getUsername());
                    }
                };
                btn.setOnClickListener(racceptA);

                Button btn2 = (Button) convertView.findViewById(R.id.rdeclineB);
                rdeclineA = new MyOnClickListener(fli.get(i).getUserid(),fli.get(i).getName()) {
                    public void onClick(View v) {
                        tcps.declineRequest(this.getUsername());
                    }
                };
                btn2.setOnClickListener(rdeclineA);
                break;
            case 2:
                //outgoing
                if (convertView == null) {
                    convertView = layoutInflater.inflate(R.layout.outgoing_list_item, viewGroup, false);
                }
                txtMessage = (TextView) convertView.findViewById(R.id.nameText);
                txtMessage.setText(fli.get(i).getName());

                Button btn3 = (Button) convertView.findViewById(R.id.rundoB);
                rundorA = new MyOnClickListener(fli.get(i).getUserid(),fli.get(i).getName()) {
                    public void onClick(View v) {
                        tcps.undoRequest(this.getUsername());
                    }
                };
                btn3.setOnClickListener(rundorA);

                break;
            case 3:
                //requestsep
                if (convertView == null) {
                    convertView = layoutInflater.inflate(R.layout.request_separator, viewGroup, false);
                }
                txtMessage = (TextView) convertView.findViewById(R.id.sepText);
                txtMessage.setText("Bejövő felkérések");
                return convertView;
            case 4:
                //outgoinfsep
                if (convertView == null) {
                    convertView = layoutInflater.inflate(R.layout.request_separator, viewGroup, false);
                }
                txtMessage = (TextView) convertView.findViewById(R.id.sepText);
                txtMessage.setText("Kimenő felkérések");
                return convertView;
        }
        return convertView;
    }
}
