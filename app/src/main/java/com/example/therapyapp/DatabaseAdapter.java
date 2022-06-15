package com.example.therapyapp;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.IOException;


public class DatabaseAdapter {
    private final String TAG = "databaseAdapter";
    private final Context mContext;
    private SQLiteDatabase mDb;
    private DatabaseHelper mDatabaseHelper;
    private String sql;

    public DatabaseAdapter(Context context) {
        this.mContext = context;
        mDatabaseHelper = new DatabaseHelper(mContext);
    }

    public DatabaseAdapter createDataBase() throws SQLException {
        try {
            mDatabaseHelper.createDataBase();
        } catch (IOException mIOException) {
            Log.e(TAG, mIOException.toString() + "  UnableToCreateDatabase");
            throw new Error("UnableToCreateDatabase");
        }
        return this;
    }

    public DatabaseAdapter openDataBase() throws SQLException {
        try {
            mDatabaseHelper.openDataBase();
            mDatabaseHelper.close();
            mDb = mDatabaseHelper.getReadableDatabase();
        } catch (SQLException mSQLException) {
            Log.e(TAG, "open >>"+ mSQLException.toString());
            throw mSQLException;
        }
        return this;
    }

    public void close() {
        mDatabaseHelper.close();
    }

    public Cursor getData(String sql) {
        this.sql = sql;
        try {
            Cursor mCur = mDb.rawQuery(sql, null);
            return mCur;
        } catch (SQLException mSQLException) {
            Log.e(TAG, "getData >>"+ mSQLException.toString());
            throw mSQLException;
        }
    }

    public void executeSql(String sql) {
        this.sql = sql;
        mDb.execSQL(sql);
    }
}
