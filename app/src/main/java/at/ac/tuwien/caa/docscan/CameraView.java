package at.ac.tuwien.caa.docscan;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.List;

import at.ac.tuwien.caa.docscan.cv.Patch;

/**
 * Created by fabian on 16.06.2016.
 */

public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback
{


    class CameraFrameThread extends Thread {

        private CameraView mCameraView;
        private boolean mIsRunning;
        private int mSurfaceWidth,mSurfaceHeight;

        public CameraFrameThread(CameraView cameraView) {

            mCameraView = cameraView;

        }

        public void setRunning(boolean running) {

            mIsRunning = running;

        }

        public void setSurfaceSize(int width, int height) {

            mSurfaceWidth = width;
            mSurfaceHeight = height;

        }

        @Override
        public void run() {

            byte[] frame;


            while (mIsRunning) {
                // TODO: I am not sure if this access should be synchronized, but I guess:
//                synchronized (mCameraView) {
                frame = mCameraView.getFrame();
                if (frame != null) {
                    Mat mat = new Mat(mSurfaceWidth, mSurfaceHeight, CvType.CV_8UC3);
                    mat.put(0, 0, frame);

                    int angle = MainActivity.getCameraOrientation(mCamera);

                    Mat tmp;

                    if (angle == 0) {
                        tmp = mat.t();
                        Core.flip(tmp, mat, 1);
                        tmp.release();
                    }

//                    NativeWrapper.processFrame(mat);
//                    Patch p = NativeWrapper.processFrameTest(mat);
                    Patch[] patches = NativeWrapper.getPatches(mat);
                    mCVCallback.onFocusMeasured(patches);

                }

//                }

            }
        }

    }

    private Camera mCamera = null;
    private int previewSizeWidth;
    private int previewSizeHeight;
    private SurfaceHolder holder;
    private byte[] mFrame;
    private CameraFrameThread mCameraFrameThread;
    private NativeWrapper.CVCallback mCVCallback;

    private static String TAG = "CameraView";

    public CameraView(Context context, AttributeSet attrs) {


        super(context, attrs);

        mCVCallback = (NativeWrapper.CVCallback) context;

        holder = getHolder();
        holder.addCallback(this);

        mCameraFrameThread = null;

    }

    @Override
    public void onPreviewFrame(byte[] pixels, Camera arg1)
    {
        mFrame = pixels;

    }

    public void onPause()
    {

        mCamera.stopPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
//        Parameters parameters;

//        parameters = mCamera.getParameters();
//        // Set the camera preview size
//        parameters.setPreviewSize(PreviewSizeWidth, PreviewSizeHeight);

        holder = holder;
//        mHandler = new Handler(Looper.getMainLooper());

        mCameraFrameThread.setSurfaceSize(width, height);
        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();

//        mFrameWidth = width;
//        mFrameHeight = height;

        // selecting optimal camera preview size

        int minDiff = Integer.MAX_VALUE;
        for (Camera.Size size : sizes) {
            if (Math.abs(size.height - height) < minDiff) {
                previewSizeWidth = size.width;
                previewSizeHeight = size.height;
                minDiff = Math.abs(size.height - height);
            }
        }

        // Use autofocus if available:
        if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

        params.setPreviewSize(previewSizeWidth, previewSizeHeight);


        mCamera.setParameters(params);

        MainActivity.setCameraDisplayOrientation(mCamera);
        mCamera.startPreview();
    }

    @Override
    public void surfaceCreated(SurfaceHolder arg0)
    {
        mCamera = Camera.open();
        try
        {
            // If did not set the SurfaceHolder, the preview area will be black.
            mCamera.setPreviewDisplay(arg0);
            mCamera.setPreviewCallback(this);
        }
        catch (IOException e)
        {
            mCamera.release();
            mCamera = null;
        }

        mCameraFrameThread = new CameraFrameThread(this);

        mCameraFrameThread.setRunning(true);
        Thread.State state = mCameraFrameThread.getState();
        // TODO: check why the thread is already started - if the app is restarted. The thread should be dead!
        if (mCameraFrameThread.getState() == Thread.State.NEW)
            mCameraFrameThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0)
    {

        boolean retry = true;
        mCameraFrameThread.setRunning(false);
        while (retry) {
            try {
                mCameraFrameThread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }

        Thread.State state = mCameraFrameThread.getState();

        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    protected byte[] getFrame() {

        return mFrame;

    }



}