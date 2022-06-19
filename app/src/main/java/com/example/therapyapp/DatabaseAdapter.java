package com.example.therapyapp;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.IOException;


public class DatabaseAdapter {
    private final String TAG = "databaseAdapter";
    private final Context context;
    private SQLiteDatabase database;
    private DatabaseHelper databaseHelper;
    private String sql;

    public DatabaseAdapter(Context context) {
        this.context = context;
        databaseHelper = new DatabaseHelper(this.context);
    }

    public DatabaseAdapter createDataBase() throws SQLException {
        try {
            databaseHelper.createDataBase();
        } catch (IOException mIOException) {
            Log.e(TAG, mIOException.toString() + "  UnableToCreateDatabase");
            throw new Error("UnableToCreateDatabase");
        }
        return this;
    }

    public DatabaseAdapter openDataBase() throws SQLException {
        try {
            databaseHelper.openDataBase();
            databaseHelper.close();
            database = databaseHelper.getReadableDatabase();
        } catch (SQLException mSQLException) {
            Log.e(TAG, "open >>"+ mSQLException.toString());
            throw mSQLException;
        }
        return this;
    }

    public void close() {
        databaseHelper.close();
    }

    public Cursor getData(String sql) {
        this.sql = sql;
        try {
            Cursor mCur = database.rawQuery(sql, null);
            return mCur;
        } catch (SQLException mSQLException) {
            Log.e(TAG, "getData >>"+ mSQLException.toString());
            throw mSQLException;
        }
    }
}
