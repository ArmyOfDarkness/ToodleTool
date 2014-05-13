package com.example.toodletool;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;


public class TaskAdapter extends CursorAdapter {
    private static final String KEY_TITLE = "title";
    private static final String KEY_STAR = "star";
    private static int TITLE_COLUMN;
    private static int STAR_COLUMN;

    private LayoutInflater inflater;
    private int resource;
    //private ArrayList<JSONObject> tasks;

    public TaskAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
        //super(context, resource, cursor);
        //this.resource = resource;
        //this.tasks = tasks;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        TITLE_COLUMN = cursor.getColumnIndex(KEY_TITLE);
        STAR_COLUMN = cursor.getColumnIndex(KEY_STAR);
    }
/*
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
*/
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View row = inflater.inflate(R.layout.task_row, parent, false);
        return row;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        CheckBox star = (CheckBox) view.findViewById(R.id.star);
        TextView title = (TextView) view.findViewById(R.id.title);
        if (cursor.getString(STAR_COLUMN).equals("1")) {
            star.setPressed(true);
        }
        title.setText(cursor.getString(TITLE_COLUMN));
    }
}
