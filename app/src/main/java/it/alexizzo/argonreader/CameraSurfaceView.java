package it.alexizzo.argonreader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.File;

import it.alexizzo.argonreader.activities.CameraActivity;

/**
 * Created by alessandro on 07/08/16.
 */
public class CameraSurfaceView extends GLSurfaceView {

    private static final String sTag = CameraSurfaceView.class.getSimpleName();


    private CameraSurfaceRenderer mRenderer;
    private ViewGroup mContentLayout;
    private CameraActivity mCameraActivity;
    public CameraSurfaceView(Context context) {
        this(context, null);

    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {

        super(context, attrs);
        setEGLConfigChooser(false);
        setEGLContextClientVersion(2);
//        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mCameraActivity = (CameraActivity) context;
//        getHolder().setFormat(PixelFormat.TRANSPARENT);
        mRenderer = new CameraSurfaceRenderer(this);
        setRenderer(mRenderer);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); //renderizza solo quando chiamo requestRender();
//        setPreserveEGLContextOnPause(true);
        //fondamentali questi 2 sotto così per far spuntare l'overlay sopra la camera
//        setZOrderOnTop(true);
        setZOrderMediaOverlay(true);

    }

    public CameraActivity getActivity(){ return mCameraActivity; }
    public void surfaceCreated ( SurfaceHolder holder ) {

        super.surfaceCreated ( holder );

    }

    public void surfaceDestroyed ( SurfaceHolder holder ) {
        mRenderer.close();
        super.surfaceDestroyed ( holder );
    }

    public void surfaceChanged (SurfaceHolder holder, int format, int w, int h ) {
        super.surfaceChanged ( holder, format, w, h );
    }

    @Override
    public void onResume() {
        Log.d(sTag, "onResume");
        super.onResume();
        mRenderer.onResume();
    }

    @Override
    public void onPause() {
        Log.d(sTag, "onPause");
        super.onPause();
        mRenderer.onPause();
    }

    public ViewGroup getContentLayout(){
        if ( mContentLayout == null ) setUI();
        return mContentLayout;
    }


    @SuppressLint({"ResourceType", "ClickableViewAccessibility"})
    private void setUI(){
        mContentLayout = new RelativeLayout(getContext());
        mContentLayout.setId(16);
        mContentLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mContentLayout.setBackgroundColor(Color.TRANSPARENT);
        RelativeLayout rl = new RelativeLayout(getContext());
        ViewGroup.LayoutParams rlParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        rl.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ImageView view = new ImageView(getContext());
        Drawable d = ContextCompat.getDrawable(getContext(), R.drawable.black);
        d.setColorFilter(0x77000000, PorterDuff.Mode.MULTIPLY);
        view.setBackground(d);
        view.setId(17);
        view.setVisibility(GONE);
        RelativeLayout.LayoutParams viewParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics()));
        viewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        rl.addView(view, viewParams);

        view = new ImageView(getContext());
        view.setBackground(d);
        view.setId(18);
        view.setVisibility(GONE);
        viewParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics()));
        viewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);

        rl.addView(view, viewParams);

        view = new ImageView(getContext());
        view.setBackground(d);
        view.setId(19);
        view.setVisibility(GONE);
        viewParams = new RelativeLayout.LayoutParams(
                (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics()),
                RelativeLayout.LayoutParams.MATCH_PARENT);
        viewParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        viewParams.addRule(RelativeLayout.BELOW, 18);
        viewParams.addRule(RelativeLayout.ABOVE, 17);

        rl.addView(view, viewParams);


        view = new ImageView(getContext());
        view.setBackground(d);
        view.setId(20);
        view.setVisibility(GONE);
        RelativeLayout.LayoutParams viewParams2 = new RelativeLayout.LayoutParams(
                (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics()),
                RelativeLayout.LayoutParams.MATCH_PARENT);
        viewParams2.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        viewParams2.addRule(RelativeLayout.BELOW, 18);
        viewParams2.addRule(RelativeLayout.ABOVE, 17);

        rl.addView(view, viewParams);

        Button b_submit = new Button(getContext());
        b_submit.setText("Scatta");

        b_submit.setId(21);
        RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);

        b_submit.setVisibility(View.GONE);

        b_submit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

//                mRenderer.takePhoto();
            }
        });

        buttonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        buttonParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        buttonParams.setMargins(3, 0, 3, 13);
        rl.addView(b_submit, buttonParams);
        mContentLayout.addView(rl, rlParams);
        mContentLayout.addView(this, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//        mContentLayout.addView(rl, rlParams);

    }

//    public void takePhoto(){
//        mRenderer.takePhoto();
//    }

    public void setDimensionForLine(float w1, float w2, float h1, float h2){
        mRenderer.setDimensionForLine(w1, w2, h1, h2);
    }
}

