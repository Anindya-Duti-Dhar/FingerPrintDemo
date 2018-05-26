package com.example.duti.fingerprintdemo;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.amitshekhar.DebugDB;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        new DatabaseHandler(this);

        // get db log browser address
        Log.i("duti", "DB Browser: " + DebugDB.getAddressLog());

    }


    public void addUser(View view) {
        Intent intent = new Intent(HomeActivity.this,
                UserRegistrationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    public void verifyUser(View view) {
        Intent intent = new Intent(HomeActivity.this,
                UserVerificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
