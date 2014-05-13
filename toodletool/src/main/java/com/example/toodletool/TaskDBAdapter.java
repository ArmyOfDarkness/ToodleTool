package com.example.toodletool;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.sql.SQLException;

/**
 * Created by brendan on 5/7/2014.
 */
public class TaskDBAdapter {

    private static final String TABLE_NAME = "Tasks";
    private static final int DB_VERSION = 2;
    private static final String TABLE_CREATE = "CREATE TABLE " + TABLE_NAME + " (_id integer primary key, serverId integer, title text, star integer, startDate integer, dueDate integer, priority integer, status integer, notes text);";
    private static final String KEY_SERVERID = "serverId";
    private static final String KEY_ROWID = "_id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_STARTDATE = "startDate";
    private static final String KEY_DUEDATE = "dueDate";
    private static final String KEY_PRIORITY = "priority";
    private static final String KEY_STAR = "star";
    private static final String KEY_NOTES = "notes";
    private static final String KEY_STATUS = "status";

    private Context context;
    private TaskDBHelper taskDBHelper;
    private SQLiteDatabase db;

    public class TaskDBHelper extends SQLiteOpenHelper {

        TaskDBHelper(Context context) {
            super(context, TABLE_NAME, null, DB_VERSION);
        }
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(TABLE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

    //Constructor
    public TaskDBAdapter(Context ctx) {
        context = ctx;
    }

    //Open database or try to create new one or throw exception
    public TaskDBAdapter open() throws SQLException {
        taskDBHelper = new TaskDBHelper(context);
        db = taskDBHelper.getWritableDatabase();
        return this;
    }

    public long createTask(String title, int star, long start, long due, int priority, int status, String notes) {
        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, title);
        values.put(KEY_SERVERID, 0);
        values.put(KEY_STAR, star);
        values.put(KEY_STARTDATE, start);
        values.put(KEY_DUEDATE, due);
        values.put(KEY_PRIORITY, priority);
        values.put(KEY_STATUS, status);
        values.put(KEY_NOTES, notes);
        return db.insert(TABLE_NAME, null, values);
    }

    public long editTask() {
        return 0;
    }

    public Cursor fetchTasks() {
        return db.query(TABLE_NAME, new String[] {KEY_ROWID, KEY_TITLE, KEY_PRIORITY, KEY_STAR, KEY_DUEDATE}, null, null, null, null, null);
    }

    public Cursor fetchNewTasks() {
        return db.query(TABLE_NAME, new String[] {KEY_ROWID, KEY_SERVERID, KEY_TITLE, KEY_STAR, KEY_STARTDATE, KEY_DUEDATE, KEY_PRIORITY, KEY_STATUS, KEY_NOTES}, KEY_SERVERID + "=0", null, null, null, null);
    }
}
