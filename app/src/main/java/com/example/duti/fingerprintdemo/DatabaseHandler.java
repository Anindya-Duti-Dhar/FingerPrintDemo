package com.example.duti.fingerprintdemo;

import android.content.Context;

import static com.example.duti.fingerprintdemo.Constant.mRecordId;


public class DatabaseHandler {

    Context mContext;

    public DatabaseHandler(Context context) {
        mContext = context;
        createTable();
    }

    public void createTable() {
        new Repository(mContext, new User()).create(mRecordId, true);
    }

}
