package guri.br.selfiestudio;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CameraActivity extends Activity implements SurfaceHolder.Callback {

    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    boolean previewing = false;
    LayoutInflater controlInflater = null;
    public static Button buttonTakePicture;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_android_camera);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        getWindow().setFormat(PixelFormat.UNKNOWN);
        surfaceView = (SurfaceView)findViewById(R.id.camerapreview);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        controlInflater = LayoutInflater.from(getBaseContext());
        View viewControl = controlInflater.inflate(R.layout.control, null);
        LayoutParams layoutParamsControl
                = new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT);
        this.addContentView(viewControl, layoutParamsControl);

        buttonTakePicture = (Button)findViewById(R.id.takepicture);
        buttonTakePicture.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                Camera.Parameters p = camera.getParameters();
                p.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                camera.setParameters(p);
                camera.takePicture(myShutterCallback,
                        myPictureCallback_RAW, myPictureCallback_JPG);
            }});
    }

    /*protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    private void releaseCamera(){
        if (camera != null){
            camera.release();
            //camera = null;
        }
    }*/


    ShutterCallback myShutterCallback = new ShutterCallback(){

        @Override
        public void onShutter() {
            // TODO Auto-generated method stub

        }};

    PictureCallback myPictureCallback_RAW = new PictureCallback(){

        @Override
        public void onPictureTaken(byte[] arg0, Camera arg1) {
        }};

    PictureCallback myPictureCallback_JPG = new PictureCallback(){

        @Override
        public void onPictureTaken(byte[] arg0, Camera arg1) {
            if (StorageHelper.isExternalStorageWritable()) {
                File f = StorageHelper.getAlbumStorageDir("");
                File myExternalFile = new File(f, "teste3.jpg");
                try {
                    // salvando a foto no storage
                    FileOutputStream fos = new FileOutputStream(myExternalFile);
                    fos.write(arg0);
                    fos.close();

                    // abrindo o dialog pra selecionar o dispositivo para onde será enviada a foto
                    sendFileToDevice();

                } catch (IOException e) {
                    Log.e("ERRO AO SALVAR ARQUIVO", e.getMessage());
                }
            }
        }};

    /**
     * Abre um popup para selecionar o dispositivo ao qual será enviada a foto.
     */
    public void sendFileToDevice() {

        File f = StorageHelper.getAlbumStorageDir("");
        File myFile = new File(f, "teste3.jpg");

        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.setComponent(new ComponentName("com.android.bluetooth", "com.android.bluetooth.opp.BluetoothOppLauncherActivity"));

        //Log.d("DEVICEEEEEEE", MainActivity.mThreadComunicacao.getDevice().getName());
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(myFile));
        startActivity(intent);

        /**
         * Esse código pode ser substituido pelo de cima, e não vai abrir o popup para selecionar o dispositivo,
         * vai mandar diretamente.
         * FUNCIONAA somente para versões do android abaixo de 4.1  :(
        BluetoothDevice device = MainActivity.mThreadComunicacao.getDevice();
        ContentValues values = new ContentValues();
        String address = device.getAddress();
        values.put(BluetoothShare.URI, Uri.fromFile(myFile).toString());
        values.put(BluetoothShare.DESTINATION, address);
        values.put(BluetoothShare.DIRECTION, BluetoothShare.DIRECTION_OUTBOUND);
        Long ts = System.currentTimeMillis();
        values.put(BluetoothShare.TIMESTAMP, ts);
        Uri contentUri = getContentResolver().insert(BluetoothShare.CONTENT_URI, values);
         */
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        // TODO Auto-generated method stub
        if(previewing){
            camera.stopPreview();
            previewing = false;
        }

        if (camera != null){
            try {
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                previewing = true;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        camera = Camera.open();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        //if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        //}

        previewing = false;
    }
}