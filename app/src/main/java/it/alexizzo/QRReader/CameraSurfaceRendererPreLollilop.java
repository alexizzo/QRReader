package it.alexizzo.QRReader;

import android.annotation.TargetApi;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import it.alexizzo.argonreader.R;

/**
 * Created by alessandro on 10/08/16.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraSurfaceRendererPreLollilop implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private static final String sTag ="CameraSurfaceRendererPL";
    private int[] mTextureIds;
    private FloatBuffer pVertex;
    private FloatBuffer pTexCoord;
    private int mProgramId;

    private Camera mCamera;
    private int mCameraId;
    private SurfaceTexture mSTexture;

    private volatile boolean mNewSTextureAvailable, mGLInit;
    private float[] mMvpMatrix;

    private WeakReference<CameraSurfaceView> mCameraSurfaceView;

    public CameraSurfaceRendererPreLollilop(CameraSurfaceView cameraSurfaceView) {
        mCameraSurfaceView = new WeakReference<CameraSurfaceView>(cameraSurfaceView);
        float[] vtmp = {1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f};
        float[] ttmp = {1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f};
        pVertex = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        pVertex.put(vtmp);
        pVertex.position(0);
        pTexCoord = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        pTexCoord.put(ttmp);
        pTexCoord.position(0);
        mCameraId=-1;
        mNewSTextureAvailable = mGLInit = false;
    }

    public void close() {
        mNewSTextureAvailable = false;
        mSTexture.release();
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        deleteTex();
    }

    public void onResume() {


    }

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        //String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
        //Log.i("mr", "Gl extensions: " + extensions);
        //Assert.assertTrue(extensions.contains("OES_EGL_image_external"));

        mGLInit=true;
        initTex();
        mSTexture = new SurfaceTexture(mTextureIds[0]);
        mSTexture.setOnFrameAvailableListener(this);

        mCameraId=findFrontFacingCameraID();
        if(mCameraId!=-1) {
            mCamera = Camera.open(mCameraId);
            setCameraDisplayOrientation();
            try {
                mCamera.setPreviewTexture(mSTexture);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            mCamera.startPreview();
        }
//        mCamera = Camera.open();


        mMvpMatrix = new float[16];
        Matrix.setIdentityM(mMvpMatrix, 0);
//        Matrix.rotateM(mMvpMatrix, 0, mMvpMatrix, 0, 90, 0, 0, -1);

        GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);

        mProgramId = Utils.compileAndLinkShaders(mCameraSurfaceView.get().getContext(), R.raw.vertex_shader, R.raw.fragment_shader);

        mGLInit=false;
    }


    @Override
    public void onDrawFrame(GL10 gl10) {
//        Log.d(sTag, "onDrawFrame");
        /**
         * Clear the rendering surface
         * GLES20.GL_COLOR_BUFFER_BIT -> cancella tutti i colori sulla surface e riempie con quello definito in glClearColor
         * GLES20.GL_DEPTH_BUFFER_BIT ->
         */

//        if (!mGLInit) {
//            Log.d(sTag, "onDraw blocked by mGLInit");
//            return;
//        }
//        if(mSTexture==null) initTextures();

//        Log.d(sTag, "onDraw blocked not by mGLInit");
//        try {
//            mDrawSemaphore.acquire();
//            Log.d(sTag, "onDraw semaphore acquired");
        if(mGLInit) return;
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);


//        synchronized (this) {
        if (mNewSTextureAvailable) {
            mNewSTextureAvailable = false;
            mSTexture.updateTexImage();
        }
//        }


        GLES20.glUseProgram(mProgramId);
//            drawWorld();

        int ph = GLES20.glGetAttribLocation(mProgramId, "a_position");
        int tch = GLES20.glGetAttribLocation(mProgramId, "a_textureCoord");
        int typeh = GLES20.glGetAttribLocation(mProgramId, "a_type");
        int foundh = GLES20.glGetAttribLocation(mProgramId, "a_found");
        int mvpch = GLES20.glGetUniformLocation(mProgramId, "mvp_matrix");

        GLES20.glUniformMatrix4fv(mvpch, 1, false, mMvpMatrix, 0);
        GLES20.glVertexAttribPointer(ph, 2, GLES20.GL_FLOAT, false, 4 * 2, pVertex);
        GLES20.glVertexAttribPointer(tch, 2, GLES20.GL_FLOAT, false, 4 * 2, pTexCoord);
        GLES20.glVertexAttrib1f(typeh, 1f);
//            GLES20.glVertexAttrib1f(foundh, mGreenLine == true ? 1 : 0);
        GLES20.glEnableVertexAttribArray(ph);
        GLES20.glEnableVertexAttribArray(tch);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureIds[0]);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mProgramId, "texture"), 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glFlush();


//            if (mTakePhoto && !mPhotoElaboration && !mQRCodeFounded) {

//            mDrawSemaphore.release();
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        Log.d(sTag, "onSurfaceChanged w: "+width+", h: "+height);
    }

    private void initTex() {
        mTextureIds = new int[1];
        GLES20.glGenTextures(1, mTextureIds, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureIds[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
    }

    private void deleteTex() {
        GLES20.glDeleteTextures(1, mTextureIds, 0);
    }

    public synchronized void onFrameAvailable(SurfaceTexture st) {
        mNewSTextureAvailable = true;
        mCameraSurfaceView.get().requestRender();
    }


    public void setCameraDisplayOrientation() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        int rotation = mCameraSurfaceView.get().getActivity().getWindowManager().getDefaultDisplay()
                .getRotation();

        float targetRatio = (float)120/(float)170;

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        Log.d(sTag, "info.orientation "+info.orientation+", "+degrees);
        int result;
//        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//            result = (info.orientation + degrees) % 360;
//            result = (360 - result) % 360;  // compensate the mirror
//        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
//        }

        mCamera.setDisplayOrientation(result);

        Camera.Parameters param = mCamera.getParameters();
        if (param.getSupportedPreviewSizes().size() > 0) {
            int mw, mh;
            mw = mh = 0;
            List<Camera.Size> psize = param.getSupportedPreviewSizes();
            for (int i = 0; i < psize.size(); i++) {
                if (psize.get(i).width > mw || psize.get(i).height > mh) {
                    mw = psize.get(i).width;
                    mh = psize.get(i).height;
                    break;
                }
            }
            Log.d(sTag, "info size, w: "+mw+", h: "+mh);
            param.setPreviewSize(mw, mh);//.get(i).width, psize.get(i).height);
//            param.setPictureSize(mw, mh);
            param.setPictureFormat(ImageFormat.JPEG);
            //Log.i("mr","ssize: "+psize.get(i).width+", "+psize.get(i).height);
        }
//        param.set("orientation", "landscape");
        mCamera.setParameters(param);
    }

    private int findFrontFacingCameraID() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                Log.d(sTag, "Camera found");
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    private void imageOrientation(String imageFilePath) {
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(imageFilePath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);
            switch (orientation) {
                case ExifInterface.ORIENTATION_NORMAL:
                    Log.d(sTag, "imageOrientation "+0);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    Log.d(sTag, "imageOrientation "+270);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    Log.d(sTag, "imageOrientation "+180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    Log.d(sTag, "imageOrientation "+90);
                    break;
                default:
                    Log.d(sTag, "imageOrientation unknown "+orientation);
            }
        } catch (IOException e) {
            Log.e(sTag, "imageOrientation ERROR ", e);
        }
    }
}
