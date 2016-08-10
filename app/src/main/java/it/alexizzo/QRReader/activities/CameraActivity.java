package it.alexizzo.QRReader.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.rahatarmanahmed.cpv.CircularProgressView;

import it.alexizzo.QRReader.CameraSurfaceView;
import it.alexizzo.argonreader.R;

/**
 * Created by alessandro on 07/08/16.
 */
public class CameraActivity extends Activity {

    private static final String sTag = CameraActivity.class.getSimpleName();

    private CameraManager mCameraManager;
    private Camera mCamera;
    private CameraSurfaceView mCameraSurfaceView;
    private CircularProgressView mCpv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        if(instantiateCamera()){
//
//        } else showAlert();
        mCameraSurfaceView = new CameraSurfaceView(this);
        ((FrameLayout) findViewById(R.id.cameraSurfaceView)).addView(mCameraSurfaceView.getContentLayout());
        mCpv = (CircularProgressView) findViewById(R.id.cpv);
        ((ImageView) findViewById(R.id.top)).setColorFilter(0x77000000, PorterDuff.Mode.MULTIPLY);//OVERLAY
        ((ImageView) findViewById(R.id.left)).setColorFilter(0x77000000, PorterDuff.Mode.MULTIPLY);
        ((ImageView) findViewById(R.id.right)).setColorFilter(0x77000000, PorterDuff.Mode.MULTIPLY);
        ((ImageView) findViewById(R.id.bottom)).setColorFilter(0x77000000, PorterDuff.Mode.MULTIPLY);
        ((ImageView) findViewById(R.id.top)).invalidate();
        ((ImageView) findViewById(R.id.left)).invalidate();
        ((ImageView) findViewById(R.id.right)).invalidate();
        ((ImageView) findViewById(R.id.bottom)).invalidate();
        findViewById(R.id.rl).getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Log.e(sTag, "onGlobalLayout: "+(findViewById(R.id.left).getMeasuredWidth()*1f)+", "+(findViewById(R.id.rl).getMeasuredWidth()*1f));
                float w1 = ((((findViewById(R.id.left).getMeasuredWidth()*1f)/(findViewById(R.id.rl).getMeasuredWidth()*1f)))* 2f)-1f;
                float w2 = ((((findViewById(R.id.rl).getMeasuredWidth()*1f)-(findViewById(R.id.left).getMeasuredWidth()*1f)*1f)/(findViewById(R.id.rl).getMeasuredWidth()*1f))* 2f)-1f;
                float h1 = (((findViewById(R.id.top).getMeasuredHeight()*1f)/(findViewById(R.id.rl).getMeasuredHeight()*1f))* 2f)-1f;
                float h2 = (((findViewById(R.id.rl).getMeasuredHeight()*1f)-(findViewById(R.id.bottom).getMeasuredHeight()*1f)*1f)/(findViewById(R.id.rl).getMeasuredHeight()*1f)* 2f)-1f;
                mCameraSurfaceView.setDimensionForLine(w1, w2, h1, h2);

                findViewById(R.id.rl).getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
//        ((Button) findViewById(R.id.b_scatta)).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                mCameraSurfaceView.takePhoto();
//            }
//        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.d(sTag, "onPause");
        mCameraSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(sTag, "onResume");
        mCameraSurfaceView.onResume();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressWarnings("deprecation")
    private boolean instantiateCamera(){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            mCamera = Camera.open();
        }
        else{
            try {
                mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            }
            catch(RuntimeException e){
                Log.e(sTag, "cameraManager getSystemService", e);
                return false;
            }
            if(mCameraManager == null){
                Log.e(sTag, "cameraManager null");
                return false;
            }
            String[] cameraIdList;
            try {
                cameraIdList = mCameraManager.getCameraIdList();
            } catch (CameraAccessException e) {
                Log.e(sTag, "cameraIdList exc", e);
                return false;
            }
            if(cameraIdList.length == 0){
                Log.e(sTag, "cameraIdList length=0");
                return false;
            }
        }
        return true;
    }

    private void showAlert(){

    }

    public void showProgress(){
        if(Looper.myLooper() == Looper.getMainLooper())
            mCpv.setVisibility(View.VISIBLE);
        else runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCpv.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideProgress(){
        if(Looper.myLooper() == Looper.getMainLooper())
            mCpv.setVisibility(View.GONE);
        else runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCpv.setVisibility(View.GONE);
            }
        });
    }

    public void showToast(final String text){
        if(Looper.myLooper() == Looper.getMainLooper())
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        else runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CameraActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
