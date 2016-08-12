package it.alexizzo.QRReader;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;

/**
 * Created by alessandro on 09/08/16.
 */
public class ArgonApplication extends Application {
    private static final String sTag = ArgonApplication.class.getSimpleName();


    private static File sMediaCacheDir;
    private static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;
        setMediaCacheDir();
    }

    static public File getMediaCacheDir() {
        return sMediaCacheDir;
    }

    public static void setMediaCacheDir(File mediaCacheDir) {
        sMediaCacheDir = mediaCacheDir;
    }

    public static void setMediaCacheDir() {
        if(sMediaCacheDir==null) initializeMediaCacheDir();
    }

    private static void initializeMediaCacheDir() {

        if (sContext != null &&
                ContextCompat.checkSelfPermission(sContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            try {
                if (sContext.getExternalFilesDir(null) != null) {
                    sMediaCacheDir = sContext.getExternalFilesDir(null);
                } else if (sContext.getExternalCacheDir() != null) {
                    sMediaCacheDir = sContext.getExternalCacheDir();
                } else if (sContext.getCacheDir() != null) {
                    sMediaCacheDir = sContext.getCacheDir();
                } else if (sContext.getFilesDir() != null) {
                    sMediaCacheDir = sContext.getFilesDir();
                } else sMediaCacheDir = null;

            } catch (Exception e) {
                Log.e(sTag, "Cache check failed:" + e);
                sMediaCacheDir = null;
            }
        }
        Log.d(sTag, "mediaCacheDir: "+((sMediaCacheDir!=null)?sMediaCacheDir.getAbsolutePath():"null"));
    }

}
