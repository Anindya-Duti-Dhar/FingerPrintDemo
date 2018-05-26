package com.example.duti.fingerprintdemo;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.Arrays;

import SecuGen.FDxSDKPro.JSGFPLib;
import SecuGen.FDxSDKPro.SGFDxDeviceName;
import SecuGen.FDxSDKPro.SGFDxErrorCode;
import SecuGen.FDxSDKPro.SGFDxTemplateFormat;

public class UserRegistrationActivity extends AppCompatActivity {

    private PendingIntent mPermissionIntent;
    private JSGFPLib sgfplib;
    private int mImageWidth;
    private int mImageHeight;
    private int[] mMaxTemplateSize;
    private byte[] fingerPrint;
    private int[] quality;


    private ImageView fingerprintImageView;
    private EditText UserId, Username, UserAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_registration);

        fingerprintImageView = (ImageView) findViewById(R.id.fingerprintImageView);
        UserId = (EditText) findViewById(R.id.UserId);
        Username = (EditText) findViewById(R.id.Username);
        UserAddress = (EditText) findViewById(R.id.UserAddress);

        mMaxTemplateSize = new int[1];
        quality = new int[1];

        //USB Permissions
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
        sgfplib = new JSGFPLib((UsbManager) getSystemService(Context.USB_SERVICE));
        Log.i("duti", "jnisgfplib version: " + sgfplib.Version() + "\n");
    }

    public void createFingerPrint(View view) {
        byte[] imageBuffer = new byte[mImageWidth * mImageHeight];
        long result = sgfplib.GetImageEx(imageBuffer, 10000, 80);
        sgfplib.SetLedOn(true);
        if (result == SGFDxErrorCode.SGFDX_ERROR_NONE) {
            // Display image
            fingerPrint = Arrays.copyOf(imageBuffer, imageBuffer.length);
            sgfplib.GetImageQuality(mImageWidth, mImageHeight, imageBuffer, quality);
            Toast.makeText(this, "Image quality: " + quality[0], Toast.LENGTH_SHORT).show();
            fingerprintImageView.setImageBitmap(this.toGrayScale(imageBuffer));
            sgfplib.SetLedOn(false);
        }
    }

    //Converts image to gray scale
    public Bitmap toGrayScale(byte[] mImageBuffer) {
        byte[] Bits = new byte[mImageBuffer.length * 4];
        for (int i = 0; i < mImageBuffer.length; i++) {
            Bits[i * 4] = Bits[i * 4 + 1] = Bits[i * 4 + 2] = mImageBuffer[i];
            Bits[i * 4 + 3] = -1;
        }
        Bitmap bmpGrayScale = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888);
        bmpGrayScale.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));
        return bmpGrayScale;
    }

    public void addUser(View view) {
        if(hasAllData()){
            if (fingerPrint != null) insertIntoDb(fingerPrint);
            else makeToast("Please Capture Finger print properly");
        } else  makeToast("Please Enter All Data");
    }

    public boolean hasAllData() {
        if ((!TextUtils.isEmpty(UserId.getText().toString().trim())) || (!TextUtils.isEmpty(Username.getText().toString().trim()))
                || (!TextUtils.isEmpty(UserAddress.getText().toString().trim()))) return true;
        else return false;
    }

    public void insertIntoDb(byte[] fingerPrint) {
        User user = new User();
        user.setUserId(Integer.parseInt(UserId.getText().toString()));
        user.setUsername(Username.getText().toString());
        user.setUserAddress(UserAddress.getText().toString());
        user.setFingerPrint(fingerPrint);
        Repository<User> repo = new Repository<User>(this, new User());
        //repo.add(user);
        repo.insertOldStyle(Integer.parseInt(UserId.getText().toString()), Username.getText().toString(),
                UserAddress.getText().toString(), fingerPrint);
        makeToast("User Registration Success!");
        goNext();

    }

    public void goNext(){
        Intent intent = new Intent(UserRegistrationActivity.this,
                HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    // method to make toast
    public void makeToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed(){
        goNext();
    }

    @Override
    public void onResume() {
        super.onResume();
        long error = sgfplib.Init(SGFDxDeviceName.SG_DEV_AUTO);
        if (error != SGFDxErrorCode.SGFDX_ERROR_NONE) {
            AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
            if (error == SGFDxErrorCode.SGFDX_ERROR_DEVICE_NOT_FOUND)
                dlgAlert.setMessage("The attached fingerprint device is not supported on Android");
            else
                dlgAlert.setMessage("Fingerprint device initialization failed!");
            dlgAlert.setTitle("SecuGen Fingerprint SDK");
            dlgAlert.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                            return;
                        }
                    }
            );
            dlgAlert.setCancelable(false);
            dlgAlert.create().show();
        } else {
            UsbDevice usbDevice = sgfplib.GetUsbDevice();
            if (usbDevice == null) {
                AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
                dlgAlert.setMessage("SDU04P or SDU03P fingerprint sensor not found!");
                dlgAlert.setTitle("SecuGen Fingerprint SDK");
                dlgAlert.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                                return;
                            }
                        }
                );
                dlgAlert.setCancelable(false);
                dlgAlert.create().show();
            } else {
                sgfplib.GetUsbManager().requestPermission(usbDevice, mPermissionIntent);
                error = sgfplib.OpenDevice(0);
                Log.i("duti", "OpenDevice() ret: " + error + "\n");
                SecuGen.FDxSDKPro.SGDeviceInfoParam deviceInfo = new SecuGen.FDxSDKPro.SGDeviceInfoParam();
                error = sgfplib.GetDeviceInfo(deviceInfo);
                Log.i("duti", "GetDeviceInfo() ret: " + error + "\n");
                mImageWidth = deviceInfo.imageWidth;
                mImageHeight = deviceInfo.imageHeight;
                sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_SG400);
                sgfplib.GetMaxTemplateSize(mMaxTemplateSize);
                Log.i("duti", "TEMPLATE_FORMAT_SG400 SIZE: " + mMaxTemplateSize[0] + "\n");
                sgfplib.SetLedOn(true);
                sgfplib.writeData((byte) 5, (byte) 1);
            }
        }
    }

    @Override
    public void onPause() {
        sgfplib.SetLedOn(false);
        sgfplib.CloseDevice();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        sgfplib.CloseDevice();
        sgfplib.Close();
        super.onDestroy();
    }

    //This broadcast receiver is necessary to get user permissions to access the attached USB device
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("duti", "Enter mUsbReceiver.onReceive()");
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            Log.i("duti", "USB BroadcastReceiver VID : " + device.getVendorId() + "\n");
                            Log.i("duti", "USB BroadcastReceiver PID: " + device.getProductId() + "\n");
                        } else
                            Log.i("duti", "mUsbReceiver.onReceive() Device is null");
                    } else
                        Log.i("duti", "mUsbReceiver.onReceive() permission denied for device " + device);
                }
            }
        }
    };
}
