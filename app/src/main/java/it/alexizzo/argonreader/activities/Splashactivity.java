package it.alexizzo.argonreader.activities;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import it.alexizzo.argonreader.ArgonApplication;
import it.alexizzo.argonreader.R;

/**
 * Created by alessandro on 07/08/16.
 */
public class Splashactivity extends Activity {

    private static final String sTag = Splashactivity.class.getSimpleName();
    private static final int INTERNET_PERMISSION_REQUEST_CODE = 101;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 102;
    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 103;
    private volatile boolean isGranted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        if(savedInstanceState != null && savedInstanceState.containsKey("isGranted"))
            isGranted = savedInstanceState.getBoolean("isGranted");
        else isGranted = false;
        checkPermissions();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                while(!isGranted);
                startMenuActivity();
                finish();
            }
        }, 3000);
    }



    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkPermission(android.Manifest.permission.INTERNET, INTERNET_PERMISSION_REQUEST_CODE)) {
                Log.d(sTag, "Permissions - INTERNET permission NOT GRANTED");
                return;
            } else Log.d(sTag, "Permissions - INTERNET permission GRANTED");
            if (!checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_REQUEST_CODE)) {
                Log.d(sTag, "Permissions - CAMERA permission NOT GRANTED");
                return;
            } else Log.d(sTag, "Permissions - CAMERA permission GRANTED");
            if (!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE)) {
                Log.d(sTag, "Permissions - WRITE_EXTERNAL_STORAGE permission NOT GRANTED");
                return;
            } else Log.d(sTag, "Permissions - WRITE_EXTERNAL_STORAGE permission GRANTED");
        }
        continueOnCreate();
    }

    private void continueOnCreate() {
        isGranted = true;
    }

    private boolean checkPermission(final String permission, final int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    permission)) {
//                AlertDialog.Builder builder = new AlertDialog.Builder(this);
//                builder.setMessage(getResources().getString(R.string.warningPermission));
//                builder.setCancelable(false);
//                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        ActivityCompat.requestPermissions(Splashactivity.this,
//                                new String[]{permission},
//                                requestCode);
//                    }
//                });
//                AlertDialog alert = builder.create();
//                alert.show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{permission},
                        requestCode);
            }
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getResources().getString(R.string.warningPermission));
            builder.setCancelable(false);
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        } else {
            if(permissions[0] == Manifest.permission.WRITE_EXTERNAL_STORAGE) ArgonApplication.setMediaCacheDir();
            checkPermissions();
        }
    }


    private void startMenuActivity(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Splashactivity.this, MenuActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.in_to_up, R.anim.null_anim);
            }
        });
    }
}
