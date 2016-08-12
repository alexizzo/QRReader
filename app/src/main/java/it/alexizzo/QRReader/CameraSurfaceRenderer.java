package it.alexizzo.QRReader;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CameraProfile;
import android.media.ExifInterface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import it.alexizzo.argonreader.R;

/**
 * Created by alessandro on 07/08/16.
 */


/**
 * Ordine chiamate
 * 1) Constructor
 * 2)onResume
 * 3)onSurfaceCreated
 * Se si parte da onResume(nell'activity), no Constructor
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraSurfaceRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {


    private Semaphore mDrawSemaphore;
    private volatile boolean mPhotoElaboration, mNewSTextureAvailable, mGLInit, mHeightForLineSetted, mQRCodeFounded, mGreenLine;//, mTakePhoto;

    /**
     * OpenGL handles to our program uniforms, attributes and program.
     */
    private int mProgramId;//, mPositionAttributeId, mTextureCoordinateAttributeId, mMvpUniformId, mTextureUniformId;
    private int[] mTextureIds;

    private WeakReference<CameraSurfaceView> mCameraSurfaceView;
    private static final String sTag = CameraSurfaceRenderer.class.getSimpleName();
    private SurfaceTexture mSTexture;
    private String mCameraId;

    private float mHeightForLine1, mHeightForLine2, mWidthForLine1, mWidthForLine2, mCurrentWidthForLine;
    private boolean mCurrentWidthForLineVersus; //1=up 0=down
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);


    private Camera mCamera;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private int width, height;
    private FloatBuffer pVertex, pTexCoord, lineVertex, lineTexVertex;

    private long mThreadRecapthId;

    //da capire che fanno
    private Size mPreviewSize;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private Surface mSurface;
    private CameraDevice.StateCallback mStateCallback;


    private float[] mMvpMatrix;
    private ByteBuffer mPixelBuf;
    private long mLastTimeQRCodeFailed;


    protected CameraSurfaceRenderer(CameraSurfaceView cameraSurfaceView) {
        mCameraSurfaceView = new WeakReference<CameraSurfaceView>(cameraSurfaceView);
        mProgramId = -1;
        mTextureIds = null;
        mNewSTextureAvailable = mHeightForLineSetted = mCurrentWidthForLineVersus = mPhotoElaboration = mQRCodeFounded =
                mGreenLine = false;
        width = height = 0;
        mLastTimeQRCodeFailed = System.currentTimeMillis();
        mThreadRecapthId = -1;
//        Point ss = new Point();
        //cameraSurfaceView.getDisplay().getRealSize(ss);

//        cacPreviewSize(ss.x, ss.y);

        final DisplayMetrics displayMetrics = new DisplayMetrics();

        cameraSurfaceView.getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        mDrawSemaphore = new Semaphore(1, false);

//        mDensity = displayMetrics.density;
        width = displayMetrics.widthPixels;
        height = displayMetrics.heightPixels;
        Log.d(sTag, "1)change width, height: " + width + ", " + height);
        mPreviewSize = new Size(0, 0);
        float[] vtmp = {1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f};
        float[] ttmp = {1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f};
        pVertex = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        pVertex.put(vtmp);
        pVertex.position(0);
        pTexCoord = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        pTexCoord.put(ttmp);
        pTexCoord.position(0);

        float[] linetexturestmp = {0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f};
        lineTexVertex = ByteBuffer.allocateDirect(12 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        lineTexVertex.put(linetexturestmp);
        lineTexVertex.position(0);

//        startBackgroundThread();
//        instantiateCamera();
        instantiateStateCallback();

    }


    protected void onPause() {
        mGLInit = false;
        mNewSTextureAvailable = false;
        closeCamera();
        stopBackgroundThread();
    }

    protected void onResume() {
//        openCamera();
        startBackgroundThread();
//        instantiateCamera();
//        if(mStateCallback==null) instantiateStateCallback();

    }


    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        Log.d(sTag, "onSurfaceCreated");

        //solo listener interni, non quelli settati esternamente come mHeightForLineSetted R mCurrentWidthForLineVersus
        mNewSTextureAvailable = mPhotoElaboration =
                mGreenLine = mGLInit = false;
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f); //rgba
        GLES20.glClearDepthf(1f);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        initTextures();

        //calling this 2 functions after initTextures because set mSTexture
        instantiateCamera();
        openCamera();
//        GLES20.glDepthRangef(0.1f, 1f);     //il primo no 0, il secondo anke 1
//
//        GLES20.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);

        mProgramId = Utils.compileAndLinkShaders(mCameraSurfaceView.get().getContext(), R.raw.vertex_shader, R.raw.fragment_shader);

        mMvpMatrix = new float[16];
        Matrix.setIdentityM(mMvpMatrix, 0);
        Matrix.rotateM(mMvpMatrix, 0, mMvpMatrix, 0, 90, 0, 0, -1);
//        mTakePhoto = false;
        mGLInit = true;
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        try {
            mDrawSemaphore.acquire();
            Log.d(sTag, "onSurfaceChanged");
            GLES20.glViewport(0, 0, width, height);

            this.height = height;
            this.width = width;
//            mPreviewSize = new Size(width, height);

            while (mPhotoElaboration) ;
            mPixelBuf = null;
            mDrawSemaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onDrawFrame(GL10 gl10) {
//        Log.d(sTag, "onDrawFrame");
        /**
         * Clear the rendering surface
         * GLES20.GL_COLOR_BUFFER_BIT -> cancella tutti i colori sulla surface e riempie con quello definito in glClearColor
         * GLES20.GL_DEPTH_BUFFER_BIT ->
         */

        if (!mGLInit) {
            Log.d(sTag, "onDraw blocked by mGLInit");
            return;
        }
//        if(mSTexture==null) initTextures();

        Log.d(sTag, "onDraw blocked not by mGLInit");
        try {
            mDrawSemaphore.acquire();
            Log.d(sTag, "onDraw semaphore acquired");
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);


//        synchronized (this) {
            if (mNewSTextureAvailable) {
                mNewSTextureAvailable = false;
                mSTexture.updateTexImage();
            }
//        }


            GLES20.glUseProgram(mProgramId);
            drawWorld();

            int ph = GLES20.glGetAttribLocation(mProgramId, "a_position");
            int tch = GLES20.glGetAttribLocation(mProgramId, "a_textureCoord");
            int typeh = GLES20.glGetAttribLocation(mProgramId, "a_type");
            int foundh = GLES20.glGetAttribLocation(mProgramId, "a_found");
            int mvpch = GLES20.glGetUniformLocation(mProgramId, "mvp_matrix");

            GLES20.glUniformMatrix4fv(mvpch, 1, false, mMvpMatrix, 0);
            GLES20.glVertexAttribPointer(ph, 2, GLES20.GL_FLOAT, false, 4 * 2, pVertex);
            GLES20.glVertexAttribPointer(tch, 2, GLES20.GL_FLOAT, false, 4 * 2, pTexCoord);
            GLES20.glVertexAttrib1f(typeh, 1f);
            GLES20.glEnableVertexAttribArray(ph);
            GLES20.glEnableVertexAttribArray(tch);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureIds[0]);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(mProgramId, "texture"), 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);


            if (mHeightForLineSetted) {

                if (mCurrentWidthForLine == mHeightForLine1) {
                    mCurrentWidthForLine = -0.015f;
                    mCurrentWidthForLineVersus = false;
                } else if (mCurrentWidthForLine == mHeightForLine1) {
                    mCurrentWidthForLine = +0.015f;
                    mCurrentWidthForLineVersus = true;
                } else if (!mCurrentWidthForLineVersus) {    //sto scendendo
                    if (mCurrentWidthForLine < mHeightForLine1 && (mCurrentWidthForLine + 0.015f) < mHeightForLine1) {
                        mCurrentWidthForLine += 0.015;
                    } else {
                        mCurrentWidthForLineVersus = true;
                    }
                } else if (mCurrentWidthForLineVersus) {    //sto salendo
                    if (mCurrentWidthForLine > mHeightForLine2 && (mCurrentWidthForLine - 0.015f) > mHeightForLine2) {
                        mCurrentWidthForLine -= 0.015;
                    } else {
                        mCurrentWidthForLineVersus = false;
                    }
                }

                if (!mPhotoElaboration && !mQRCodeFounded) {
//            mTakePhoto = false;
                    if (mPixelBuf == null) {
                        mPixelBuf = ByteBuffer.allocateDirect(width * height * 4);
                        mPixelBuf.order(ByteOrder.LITTLE_ENDIAN);
                    }
                    mPixelBuf.rewind();
                    GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                            mPixelBuf);
                    saveFrame();
                } else if (!mQRCodeFounded && mThreadRecapthId == -1) {
                    //dopo 10s dal ritrovamento riabilito l'acquisizione di un nuovo QRCode

                    synchronized (this) {
                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while (true) {
                                    if (System.currentTimeMillis() - mLastTimeQRCodeFailed > 10000) {
//                                        mLastTimeQRCodeFailed = System.currentTimeMillis();
                                        mQRCodeFounded = false;
                                    }
                                }
                            }
                        });
                        t.start();
                        mThreadRecapthId = t.getId();
                    }
                }

//                Log.d(sTag, ""+mCurrentWidthForLine+", "+mWidthForLine1+", "+mWidthForLine2+
//                        ", "+(-mHeightForLine1)+", "+(-mHeightForLine2));
//              Prova linea con texture
//                float[] linetmp = {mCurrentWidthForLine, mWidthForLine1, 0f,
//                        mCurrentWidthForLine, mWidthForLine2, 0f,
//                        mCurrentWidthForLine + 0.15f, mWidthForLine1, 0f,
//                        mCurrentWidthForLine + 0.15f, mWidthForLine1, 0f,
//                        mCurrentWidthForLine + 0.15f, mWidthForLine2, 0f,
//                        mCurrentWidthForLine, mWidthForLine2, 0f,
//                };


//                float[] linetmp = {-mCurrentWidthForLine, mHeightForLine1, 0f,
//                        mCurrentWidthForLine, mHeightForLine1, 0f,
//                        mCurrentWidthForLine + 0.15f, mWidthForLine1, 0f,
//                        mCurrentWidthForLine + 0.15f, mWidthForLine1, 0f,
//                        mCurrentWidthForLine + 0.15f, mWidthForLine2, 0f,
//                        mCurrentWidthForLine, mWidthForLine2, 0f
//                };
//                lineVertex = ByteBuffer.allocateDirect(6 * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
//                lineVertex.put(linetmp);
//                lineVertex.position(0);
//
//
//                GLES20.glVertexAttribPointer(ph, 3, GLES20.GL_FLOAT, false, 3 * 2, lineVertex);
//                GLES20.glVertexAttribPointer(tch, 2, GLES20.GL_FLOAT, false, 2 * 2, lineTexVertex);
//                GLES20.glVertexAttrib1f(typeh, 1f);
//                GLES20.glVertexAttrib1f(foundh, mGreenLine == true ? 1 : 0);
//                GLES20.glEnableVertexAttribArray(ph);
//                GLES20.glEnableVertexAttribArray(tch);
//                GLES20.glActiveTexture(GLES20.GL_TEXTURE_2D);
//                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIds[1]);
//                GLES20.glUniform1i(GLES20.glGetUniformLocation(mProgramId, "texture"), 0);
//
//                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 2 * 3);

//                Per la linea sbloccare questo codice
                float[] pathCords = {
                        mCurrentWidthForLine, mWidthForLine1, 0.0f,
                        mCurrentWidthForLine, mWidthForLine2, 0.0f
                };
//                ByteBuffer dlb = ByteBuffer.allocateDirect(mPathLineDrawOrder.length * 2);
//                dlb.order(ByteOrder.nativeOrder());
//                GLES20.glVertexAttrib1f(typeh, 0f);
//                ShortBuffer drawListBuffer = dlb.asShortBuffer();
//                drawListBuffer.put(mPathLineDrawOrder);
//                drawListBuffer.position(0);

                ByteBuffer bb = ByteBuffer.allocateDirect(pathCords.length * 4);
                bb.order(ByteOrder.nativeOrder());
                FloatBuffer vertexBuffer = bb.asFloatBuffer();
                vertexBuffer.put(pathCords);
                GLES20.glVertexAttrib1f(typeh, 0f);
                GLES20.glVertexAttrib1f(foundh, mGreenLine == true ? 1f : 0f);
                vertexBuffer.position(0);
                GLES20.glEnableVertexAttribArray(ph);
                GLES20.glVertexAttribPointer(ph, 3, GLES20.GL_FLOAT, false,
                        3 * 4, vertexBuffer);
                GLES20.glLineWidth(4.5f);
                GLES20.glDrawArrays(GLES20.GL_LINES, 0, 3);
            }

            GLES20.glFlush();


//            if (mTakePhoto && !mPhotoElaboration && !mQRCodeFounded) {

            mDrawSemaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized void onFrameAvailable(SurfaceTexture st) {
//        Log.d(sTag, "onFrameAvailable");
        mNewSTextureAvailable = true;
        mCameraSurfaceView.get().requestRender();
    }

    private void openCamera() {

        try {
//            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            if (ActivityCompat.checkSelfPermission(mCameraSurfaceView.get().getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            try {
                mCameraManager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                return;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
//            stopBackgroundThread();
        } catch (InterruptedException e) {

        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private void instantiateStateCallback() {
        mStateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice cameraDevice) {
                Log.d(sTag, "openCamera onOpened");
                mCameraDevice = cameraDevice;
//            try {
//                mCamera.setPreviewTexture(mSTexture);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
                startBackgroundThread();
                createCameraPreviewSession();

            }

            @Override
            public void onDisconnected(CameraDevice cameraDevice) {
                Log.d(sTag, "openCamera onDisconnected");
                cameraDevice.close();
                if (null != mCameraDevice) {
                    mCameraDevice.close();
                    mCameraDevice = null;
                    if (mThreadRecapthId != -1) {
                        for (Thread t : Thread.getAllStackTraces().keySet()) {
                            if (t.getId() == mThreadRecapthId) {
                                try {
                                    t.join();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onError(CameraDevice cameraDevice, int i) {
                Log.e(sTag, "openCamera onError");
                cameraDevice.close();
                if (null != mCameraDevice) {
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
            }
        };
    }


    private void initTextures() {
        mTextureIds = new int[1];
        Log.d(sTag, "initTextures");
        GLES20.glGenTextures(1, mTextureIds, 0);
        if (mTextureIds[0] == 0) {
            GLES20.glDeleteTextures(3, mTextureIds, 0);
            throw new RuntimeException("Exception in initTextures, mTextureIds = 0 ");
        }
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureIds[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);


//        Bitmap bitmap = Utils.getBitmapFromResource(mCameraSurfaceView.get().getContext(), R.drawable.line_red);
//        if (bitmap != null) {
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIds[1]);
//            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
//            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
//        }
//
//        bitmap = Utils.getBitmapFromResource(mCameraSurfaceView.get().getContext(), R.drawable.line_green);
//        if (bitmap != null) {
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIds[2]);
//            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
//            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
//        }
        mSTexture = new SurfaceTexture(mTextureIds[0]);
        mSTexture.setOnFrameAvailableListener(this);
//        if (bitmap != null) bitmap.recycle();

    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressWarnings("deprecation")
    private void createCameraPreviewSession() {
        Log.d(sTag, "createCameraPreviewSession");
        try {
            mSTexture.setDefaultBufferSize(width, height);
            mSurface = new Surface(mSTexture);
            //Create a new camera capture session by providing the target output set of Surfaces to the camera device.
            //The active capture session determines the set of potential output Surfaces for the camera device for each capture request.
            mCameraDevice.createCaptureSession(Arrays.asList(mSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            if (null == mCameraDevice)
                                return;

                            mCaptureSession = cameraCaptureSession;
                            try {
                                mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                mPreviewRequestBuilder.addTarget(mSurface);
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
//                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
////                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_PORTRAIT);
//                                if (mCameraManager.getCameraCharacteristics(mCameraId).get(CameraCharacteristics.FLASH_INFO_AVAILABLE))
//                                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
//                                else
//                                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);

                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
//                                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);

                                int rotation = mCameraSurfaceView.get().getActivity().getWindowManager().getDefaultDisplay().getRotation();
                                mPreviewRequestBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) CameraProfile.getJpegEncodingQualityParameter(
                                        CameraProfile.QUALITY_HIGH));

                                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                Log.e(sTag, "CameraAccessException", e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            Log.e(sTag, "CameraCaptureSession onConfigureFailed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            Log.e(sTag, "CameraAccessException", e);
        }
    }


    private void instantiateCamera() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mCamera = Camera.open();
            try {
                mCamera.setPreviewTexture(mSTexture);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (ActivityCompat.checkSelfPermission(mCameraSurfaceView.get().getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e(sTag, "camera permissions not granted");
                return;
            }
            try {
                mCameraManager = (CameraManager) mCameraSurfaceView.get().getContext().getSystemService(Context.CAMERA_SERVICE);
            } catch (RuntimeException e) {
                Log.e(sTag, "cameraManager getSystemService", e);
                return;
            }
            if (mCameraManager == null) {
                Log.e(sTag, "cameraManager null");
                return;
            }
            try {
                if (mCameraManager.getCameraIdList() == null || mCameraManager.getCameraIdList().length == 0) {
                    Log.e(sTag, "cameraIdList length=0");
                    return;
                }
                for (final String cameraID : mCameraManager.getCameraIdList()) {
                    CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraID);
                    if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
                        continue;
                    mCameraId = cameraID;
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    for (Size psize : map.getOutputSizes(SurfaceTexture.class)) {
                        if (width > psize.getWidth() && height > psize.getHeight()) {
                            mPreviewSize = psize;
                            break;
                        }
                    }
                    break;
                }
            } catch (CameraAccessException e) {
                Log.e(sTag, "cameraIdList exc", e);
            } catch (IllegalArgumentException e) {
                Log.e("mr", "cacPreviewSize - Illegal Argument Exception");
            } catch (SecurityException e) {
                Log.e("mr", "cacPreviewSize - Security Exception");
            }
        }
        Log.d(sTag, ((mCameraId != null ? true : false) ? "cameraId found +" + mCameraId : "cameraId is null"));
    }


    private void drawWorld() {
        //setUniformAndAttributeLocations();
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e("mr", "stopBackgroundThread");
        }
    }

    synchronized private void saveFrame() {

        mPhotoElaboration = true;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedOutputStream bos = null;
                File file = null;
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.preRotate(180);  //rotating
                matrix.preScale(-0.3f, 0.3f); //mirroring
//            matrix.postScale(-1, 0);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                //bloccare l'acquisione della foto inonDrawFrame sul buffer mentre lo si sta elaborando
                mPixelBuf.rewind();
                bmp.copyPixelsFromBuffer(mPixelBuf);
//            Bitmap bmp=null;

                bmp = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);
                mPixelBuf.rewind();

                int[] intArray = new int[bmp.getWidth() * bmp.getHeight()];
                //copy pixel data from the Bitmap into the 'intArray' array
                bmp.getPixels(intArray, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());

                LuminanceSource source = new RGBLuminanceSource(bmp.getWidth(), bmp.getHeight(), intArray);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                Reader reader = new MultiFormatReader();// use this otherwise ChecksumException
                Result result = null;
                try {
                    result = reader.decode(bitmap);
                    mGreenLine = true;
                    mCameraSurfaceView.get().getActivity().showProgress();
                    Log.d(sTag, "BitmapBinary decode result: " + result.getText());
                    String filename = "photo_" + new Date().toString() + ".png";
                    mQRCodeFounded = true;
                    mCameraSurfaceView.get().getActivity().showProgress();
                    file = new File(ArgonApplication.getMediaCacheDir(), filename);
//                    Log.d(sTag, "file: " + file.getAbsolutePath());
                    bos = new BufferedOutputStream(new FileOutputStream(file));
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, bos);
                    //lo aggiorno per evitare tempi morti
                    mLastTimeQRCodeFailed = System.currentTimeMillis();
                    imageOrientation(file.getAbsolutePath());
                } catch (NotFoundException e) {
                    Log.d(sTag, "BitmapBinary NotFoundException");
                    e.printStackTrace();
                    if (System.currentTimeMillis() - mLastTimeQRCodeFailed > 10000) {
                        mLastTimeQRCodeFailed = System.currentTimeMillis();
                        mCameraSurfaceView.get().getActivity().showToast("Non è stato trovato nessun QRCode.");
                    }
                } catch (ChecksumException e) {
                    Log.d(sTag, "BitmapBinary ChecksumException");
                    mCameraSurfaceView.get().getActivity().showToast("Internal error, try again.");
                    e.printStackTrace();
                } catch (FormatException e) {
                    Log.d(sTag, "BitmapBinary FormatException");
                    mCameraSurfaceView.get().getActivity().showToast("Internal error, try again.");
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (bos != null)
                        try {
                            bos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    mCameraSurfaceView.get().getActivity().hideProgress();
                    if (file != null) {
                        mCameraSurfaceView.get().getActivity().showToast("Image saved as " + file.getName() + ((result != null && result.getText() != null) ? ", result " + result.getText() : ""));
                    }
                    if (bmp != null && !bmp.isRecycled()) bmp.recycle();

                    for (int i = 0; i < 5000; i++) ;
                    mCameraSurfaceView.get().getActivity().hideProgress();
                    mGreenLine = false;

                    mPhotoElaboration = false;
                }

//                    QRCodeReader reader = new QRCodeReader();
//                    BinaryBitmap bitmap = new BinaryBitmap();
//                    reader.decode(bmp.);

//                bmp.recycle();
            }
        });
        t.start();
    }

//    protected void takePhoto() {
//        mTakePhoto = true;
//    }

    protected void setDimensionForLine(float w1, float w2, float h1, float h2) {
        mWidthForLine1 = w1;
        mWidthForLine2 = w2;
        //y inverted
        mHeightForLine1 = h2;
        mHeightForLine2 = h1;
        mCurrentWidthForLine = w1;
        mHeightForLineSetted = true;
        Log.e(sTag, "setDimensionForLine: " + w1 + ", " + w2);
    }

    public void close() {
        Log.d(sTag, "close");
        mNewSTextureAvailable = false;
//        mSTexture.release();
//        mCamera.stopPreview();
//        mCamera.release();
//        mCamera = null;
//        deleteTex();
    }

    private void deleteTex() {
        Log.d(sTag, "deleteTex");
        GLES20.glDeleteTextures(1, mTextureIds, 0);
    }

    public void setCameraDisplayOrientation() {


        int rotation = mCameraSurfaceView.get().getActivity().getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        int result;
//        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//            result = (info.orientation + degrees) % 360;
//            result = (360 - result) % 360;  // compensate the mirror
//        } else {  // back-facing
//        result = (info.orientation - degrees + 360) % 360;
//        }

        try {
            Log.d(sTag, "orientation: "+mCameraManager.getCameraCharacteristics(mCameraId).get(CameraCharacteristics.SENSOR_ORIENTATION)+", "+degrees);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


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
