package it.alexizzo.QRReader;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.opengl.GLES20;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by alessandro on 07/08/16.
 */
public class Utils {


/**************************************************************************

 FUNZIONI PER LEGGERE E CARICARE GLI SHADERS

 **************************************************************************/


    /**
     * Compile the shader
     * @param sourceCode
     * @param type
     * @return The shaderId to handle it
     */
    private static int compileShader(String sourceCode, int type) {

        final int shaderId= GLES20.glCreateShader(type);

        if (shaderId==0) throw new RuntimeException("Exception in compileShader, shaderId = 0");

        GLES20.glShaderSource(shaderId, sourceCode);

        GLES20.glCompileShader(shaderId);

        final int[] compileStatus = new int[1];

        GLES20.glGetShaderiv(shaderId, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        if (compileStatus[0]==0) {
            String err=GLES20.glGetShaderInfoLog(shaderId);
            GLES20.glDeleteShader(shaderId);
            throw new RuntimeException("compileshader "+type+" "+err);
        }

        return shaderId;
    }

    /**
     * Read the source code saved on raw resources
     * @param context
     * @param vertexShaderResourceId The resource id of the vertex shader used
     * @param fragmentShaderResourceId The resource id of the fragment shader used
     * @return The id of the Program to handle it in the GPU
     */
    public static int compileAndLinkShaders(Context context, int vertexShaderResourceId, int fragmentShaderResourceId){

        int vertexShaderId = compileShader(readShaderSourceCodeFromRaw(context, vertexShaderResourceId)
                , GLES20.GL_VERTEX_SHADER);

        int fragmentShaderId = compileShader(readShaderSourceCodeFromRaw(context, fragmentShaderResourceId)
                , GLES20.GL_FRAGMENT_SHADER);

        return linkShader(vertexShaderId, fragmentShaderId);

    }
    private static String readShaderSourceCodeFromRaw( Context context, int shaderId ){

        StringBuilder body = new StringBuilder();

        try {

            InputStream inputStream = context.getResources().openRawResource(shaderId);

            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String nextLine;

            while ((nextLine = bufferedReader.readLine()) != null) {

                body.append(nextLine);
                body.append('\n');

            }
        }
        catch (IOException e) {

            throw new RuntimeException("Could not open resource: " + shaderId, e);
        }
        catch (Resources.NotFoundException nfe) {

            throw new RuntimeException("Resource not found: " + shaderId, nfe);
        }
        return body.toString();
    }


    /**
     * Link the two shader into a program
     * @param vertexId
     * @param fragmentId
     * @return The program Id that links the vertex and fragment shader passed
     */
    private static int linkShader(int vertexId, int fragmentId) {

        final int programId= GLES20.glCreateProgram();

        if (programId==0) throw new RuntimeException("Exception in linkShader, programId = 0");

        GLES20.glAttachShader(programId, vertexId);

        GLES20.glAttachShader(programId, fragmentId);

        GLES20.glLinkProgram(programId);

        final int[] linkStatus = new int[1];

        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0);

        if (linkStatus[0]==0) {
            String err= GLES20.glGetProgramInfoLog(programId);
            GLES20.glDeleteProgram(programId);
            throw new RuntimeException("linkShader"+err);
        }

        return programId;
    }


    public static Bitmap getBitmapFromResource(Context context, int resourceId) {

        BitmapFactory.Options options = new BitmapFactory.Options();

        if( resourceId >= 0 ){

            options.inScaled = false;
            options.inSampleSize = 1; //100%
            return BitmapFactory.decodeResource(context.getResources(), resourceId, options);
        }
        return null;
    }




    public static String copyToExternalStorage(String filepath){

        //if(context.getFilesDir() == null) return null;
        String result = null;

        if(!isExternalStorageWritable()) return null;

        File file = new File(ArgonApplication.getMediaCacheDir(), filepath);
        if (!file.exists()) return null;

        BufferedInputStream fIn = null;
        BufferedOutputStream fOut = null;
        File fileOut = null;
        try {
            fileOut = new File(Environment.getExternalStorageDirectory(), filepath);
            if (!fileOut.exists()) file.createNewFile();
            fIn  = new BufferedInputStream(new FileInputStream(file));
            fOut = new BufferedOutputStream(new FileOutputStream(fileOut));
            byte[] buffer = new byte[1024];
            int read;
            while ((read = fIn.read(buffer)) != -1) {
                fOut.write(buffer, 0, read);
            }
            result = fileOut.getAbsolutePath();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            result = null;
            if(fileOut != null && fileOut.exists()) fileOut.delete();
        } catch (IOException e) {
            e.printStackTrace();
            result = null;
            if(fileOut != null && fileOut.exists()) fileOut.delete();
        }  catch (Exception e) {
            e.printStackTrace();
            result = null;
            if(fileOut != null && fileOut.exists()) fileOut.delete();
        } finally{
            if (fIn != null)
                try {
                    fIn.close();
                } catch (IOException e1) {
                }
            if (fOut != null)
                try {
                    fOut.flush();
                    fOut.close();
                } catch (IOException e1) {
                }

        }

        return result;
    }

    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }


    private static void checkCameraRotation(File file) {
        int rotation = 0;
        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }
}
