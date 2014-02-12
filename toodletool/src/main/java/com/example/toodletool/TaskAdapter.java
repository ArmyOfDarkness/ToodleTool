package com.example.toodletool;

import android.content.Context;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by brendan on 2/8/14.
 */
public class TaskAdapter extends ArrayAdapter<JSONObject> {
    private LayoutInflater inflater;
    private int resource;
    private ArrayList<JSONObject> tasks;

    public TaskAdapter(Context context, int resource, ArrayList<JSONObject> tasks) {
        super(context, resource, tasks);
        this.resource = resource;
        this.tasks = tasks;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View taskView = convertView;
        String title;
        if (taskView == null) {
            taskView = inflater.inflate(resource, parent, false);
        }
        TextView taskName = (TextView) taskView.findViewById(R.id.textView);
        try {
            title = tasks.get(position).getString("title");
            taskName.setText(title);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return taskView;
    }
}
