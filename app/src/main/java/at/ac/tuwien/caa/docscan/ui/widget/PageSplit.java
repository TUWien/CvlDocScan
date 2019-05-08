package at.ac.tuwien.caa.docscan.ui.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuffXfermode;
import android.support.media.ExifInterface;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.camera.cv.NativeWrapper;
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.PageDetector;
import at.ac.tuwien.caa.docscan.logic.Helper;

import static org.opencv.android.Utils.bitmapToMat;
import static org.opencv.android.Utils.matToBitmap;
import static org.opencv.core.CvType.CV_32FC1;
import static org.opencv.core.CvType.CV_32FC3;
import static org.opencv.core.CvType.CV_32SC3;
import static org.opencv.core.CvType.CV_64FC1;
import static org.opencv.core.CvType.CV_8UC1;

/**
 *   Created by Matthias Wödlinger on 14.02.2019
 */

/**
 *  This class can be used to apply the page-split trained version of dhSegment (https://arxiv.org/abs/1804.10371)
 *  to an image file. The constructor will look for a model named MODEL_NAME.tflite in the assets folder.
 *  After initialization the model can be applied to a bitmap with {@link #applyPageSplit(Uri, Context)}.
 */
public class PageSplit {

    public static final String TAG = "PageSplit";

    /** Name of the model file stored in Assets. */
    private static final String MODEL_NAME = "1557128784_q_from_resized_images_output_160";
    private static final String MODEL_PATH = MODEL_NAME + ".tflite";

    /** Output folder */
    private static final String FOLDER_NAME = "PageSplit";
    private static int book_c;//TODO: delete

    /** TfLite model only works for fixed input dimension.
     *  The input will be resized internally. */
    private static final int DIM_IMG_SIZE_X = 160;
    private static final int DIM_IMG_SIZE_Y = 160;

    /** An instance of the TfLite Interpreter */
    private Interpreter mTflite;

    private static PageSplit pageSplitInstance = null;




    public PageSplit(Context mContext) throws IOException {

        long startTime = SystemClock.uptimeMillis();
        mTflite = new Interpreter(loadModelFile(mContext));
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Created a Tensorflow Lite model in " + Long.toString((endTime - startTime)) + "ms");
    }

    /**
     *  Initializes the tensorflow-lite model and return the PageSplit object.
     *
     *  @param  mContext ... the current context.
     *  @throws IOException if the model can not be opened.
     *          It will look for a model named MODEL_NAME.tflite in the assets folder.
     */
    public static PageSplit getInstance(Context mContext) throws IOException {
        if (pageSplitInstance == null) {
            pageSplitInstance = new PageSplit(mContext);
        }

        return pageSplitInstance;
    }

    /**
     * Applies the page split model to a saved bitmap.
     * The model produces three bitmaps:    - The output mask of the page split model
     *                                      - The input bitmap
     *                                      - An overlay of the output mask and the input bitmap.
     * The bitmaps will be saved in FOLDER_NAME and are visible in the gallery.
     *
     * @param   uri ... the Uri of the bitmap file.
     * @param   mContext ... the current context. Will be used to access the assets folder.
     * @return  returns 1 if the tflite model has not been initialized yet.
     *          Returns 0 after successful run.
     * @throws  IOException if there is a problem loading the bitmap.
     */
    public int applyPageSplit(Uri uri, Context mContext) throws IOException {

        if (mTflite == null) {
            Log.e(TAG, "PageSplit has not been initialized; Skipped.");
            return 1;
        }

        int[] id = {R.drawable.book_1,
                R.drawable.book_2,
                R.drawable.book_3,
                R.drawable.book_4,
                R.drawable.book_5,
                R.drawable.book_6,
                R.drawable.book_7,
                R.drawable.book_8,
                R.drawable.book_9,
                R.drawable.book_10,
                R.drawable.book_11,
                R.drawable.book_12};

        for (int book = 0; book < 12; ++book) {
            book_c = book;
            // Load input bitmap
//        Bitmap inBitmap = Bitmap.createScaledBitmap(bmp, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, true);


//        Bitmap bmp = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), uri);
            long timeBegin = SystemClock.uptimeMillis();
//        Bitmap inBitmap = decodeSampledBitmapFromUri(uri, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y);
//        inBitmap = Bitmap.createScaledBitmap(inBitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, true);


            Bitmap bmp = decodeSampledBitmapFromResource(mContext.getResources(),
                    id[book], DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y);
            Bitmap inBitmap = Bitmap.createScaledBitmap(
                    decodeSampledBitmapFromResource(mContext.getResources(), id[book], DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y),
                    DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, true);


            // Rotate the Bitmap
            final int rotationInDegrees = getExifAngle(uri);
            Matrix inMatrix = new Matrix();
            inMatrix.postRotate(rotationInDegrees + 90); // The model needs the book rotated by 90°; TODO: find out why
            inBitmap = Bitmap.createBitmap(inBitmap, 0, 0, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, inMatrix, true);


            // The TfLite model needs a float array as input and output
            // For alternatives see: https://www.tensorflow.org/lite/apis#running_a_model_2
            float[][][][] outFloat = new float[1][DIM_IMG_SIZE_X][DIM_IMG_SIZE_Y][3];
            float[][][][] inFloat = new float[1][DIM_IMG_SIZE_X][DIM_IMG_SIZE_Y][3];
            bitmapToFloatArray(inBitmap, inFloat);

            // Running the model
            long startTime = SystemClock.uptimeMillis();
            mTflite.run(inFloat, outFloat);
            long endTime = SystemClock.uptimeMillis();
            Log.d(TAG, "tflite.run took " + Long.toString((endTime - startTime)) + "ms");

            // Transform the output to two Mats with the red and green channels as page and seperator
            Bitmap outBitmap = Bitmap.createBitmap(DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, inBitmap.getConfig());
            List<Mat> outChannels = floatArrayToMats(outFloat, outBitmap); //TODO: outBitmap as input is not necessary
            Mat pages = outChannels.get(1);
            Mat split = outChannels.get(2);
            outChannels.clear();


            List<Point> pCorners = new ArrayList<>();
            if (getCorners(pages, pCorners, 50) == 1) {
                Mat pMask = applyThreshold(pages, -1);
                pMask = bwClean(pMask, 3);//5
                final List<MatOfPoint> pContours = findPolygonalRegions(pMask, 0.01,
                        1, true);
                if (pContours.isEmpty()) {
                    Log.e(TAG, "No page found!");
                    return 1;
                }
                pCorners = getCorners2(pContours);
            }

            pages.release();
            // Get page seperator contours
            Mat sMask = applyThreshold(split, 100);
            split.release();
            sMask = bwClean(sMask, 3);//3
            int cPoints =  countConfidentPoints(sMask,100);
            Log.d(TAG,"COUNTER: " + Integer.toString(cPoints));

            Point sTop = new Point();
            Point sBottom = new Point();
            if (cPoints < 10) {//countConfidentPoints(sMask,240) < 50) {
                sTop.x = pCorners.get(0).x;
                sTop.y = pCorners.get(0).y;
                sBottom.x = pCorners.get(3).x;
                sBottom.y = pCorners.get(3).y;
            } else {
                final List<MatOfPoint> sContours = findPolygonalRegions(sMask, 0.001,
                        1, false);
                if (sContours.isEmpty()) {
                    Log.e(TAG, "no contours for split found");
                    continue;//TODO: change
//                return 1;
                }

                // Fit a line to the seperator
                Core.rotate(sMask, sMask, Core.ROTATE_90_CLOCKWISE);
                interpolateSeperator(sContours, sMask, pCorners, sTop, sBottom);
                checkIfSinglePage(pCorners, sTop, sBottom, 0.3);
            }


            long timeEnd = SystemClock.uptimeMillis();
            Log.d(TAG, "Total time cost: " + Long.toString((timeEnd - timeBegin)) + "ms");

            rescaleCorners(bmp, uri, pCorners, sTop, sBottom);
            helperDrawTrapezoid(bmp, mContext, uri, pCorners, sTop, sBottom, 10, "Bounding_trapezoid_" + Integer.toString(book));
            saveBitmap(outBitmap, "pagesplit_" + Integer.toString(book) + MODEL_NAME, mContext);

        }
        return 0;
    }


    private int countConfidentPoints(Mat binaryMask, int threshold) {
        Mat mask = binaryMask.clone();
        mask.convertTo(mask, CV_32FC1);
        final float[] pixels = new float[mask.width()*mask.height()];
        mask.get(0,0,pixels);
        int counter = 0;

        for (int x = 0; x < mask.width(); ++x) {
            for (int y = 0; y < mask.height(); ++y) {
                final float value = pixels[y + mask.width()*x];
                if (value > threshold) {
                    ++counter;
                }
            }
        }
        mask.release();

        return counter;
    }

    private int getCorners(Mat pages, List<Point> corners, int threshold) {
        Mat mg = applyThreshold(pages, threshold);
        Imgproc.cvtColor(mg, mg, Imgproc.COLOR_GRAY2RGB);

//        Resize the image:
        NativeWrapper.resize(mg);

        DkPolyRect[] polyRects = NativeWrapper.getPageSegmentation(mg);
        if (polyRects.length == 0) {
            Log.e(TAG, "no rectangle found");
            return 1;
        }
        List<Point> result = new ArrayList<>(4);

        result.add(new Point(polyRects[0].getX3(), polyRects[0].getY3()));
        result.add(new Point(polyRects[0].getX4(), polyRects[0].getY4()));
        result.add(new Point(polyRects[0].getX1(), polyRects[0].getY1()));
        result.add(new Point(polyRects[0].getX2(), polyRects[0].getY2()));

        final Point c1 = new Point(0,0);
        final Point c2 = new Point(DIM_IMG_SIZE_X,0);
        final Point c3 = new Point(DIM_IMG_SIZE_X,DIM_IMG_SIZE_Y);
        final Point c4 = new Point(0,DIM_IMG_SIZE_Y);
        final List<Point> imageCorners = new ArrayList<>(Arrays.asList(c1,c2,c3,c4));


        for (Point c: imageCorners) {
            boolean first = true;
            double distOld = DIM_IMG_SIZE_X+DIM_IMG_SIZE_Y;
            for (Point cornerCandidate: result) {
                double distNew = Math.sqrt(Math.pow(c.x-cornerCandidate.x,2) + Math.pow(c.y-cornerCandidate.y,2));
                if (first) {
                    corners.add(new Point(cornerCandidate.x, cornerCandidate.y));
                    distOld = distNew;
                    first = false;
                } else if (distNew < distOld) {
                    corners.remove(corners.size()-1);
                    corners.add(new Point(cornerCandidate.x, cornerCandidate.y));
                    distOld = distNew;
                }
            }
        }

        return 0;
    }
    // x1 and y1 should be left of x2 and y2
    private double getAngle(Point a1, Point a2, Point b1, Point b2) {
        final Point v1 = new Point(a1.x - b1.x, a1.y - b1.y);
        final Point v2 = new Point(a2.x - b2.x, a2.y - b2.y);
        final double l1 = Math.sqrt(Math.pow(a1.x - b1.x,2) + Math.pow(a1.y - b1.y,2));
        final double l2 = Math.sqrt(Math.pow(a2.x - b2.x,2) + Math.pow(a2.y - b2.y,2));

        return Math.acos((v1.x*v2.x + v1.y*v2.y)/(l1*l2));
    }

    private void bitmapToFloatArray(Bitmap inBitmap, float[][][][] inFloat) {
        int[] inPixels = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];
        inBitmap.getPixels(inPixels, 0, DIM_IMG_SIZE_X, 0, 0, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y);

        for (int x = 0; x < DIM_IMG_SIZE_X; ++x) {
            for (int y = 0; y < DIM_IMG_SIZE_Y; ++y) {
                inFloat[0][x][y][0] = Color.red(inPixels[x + DIM_IMG_SIZE_Y*y]);
                inFloat[0][x][y][1] = Color.green(inPixels[x + DIM_IMG_SIZE_Y*y]);
                inFloat[0][x][y][2] = Color.blue(inPixels[x + DIM_IMG_SIZE_Y*y]);
            }
        }
    }

    private List<Mat> floatArrayToMats(float[][][][] outFloat, Bitmap bmp) {
        int[] outPixels = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];
        for (int x = 0; x < DIM_IMG_SIZE_X; ++x) {
            for (int y = 0; y < DIM_IMG_SIZE_Y; ++y) {
                final int r = (int) (255.0 * outFloat[0][x][y][0]);
                final int g = (int) (255.0 * outFloat[0][x][y][1]);
                final int b = (int) (255.0 * outFloat[0][x][y][2]);
                outPixels[x + y*DIM_IMG_SIZE_X] = Color.argb(255, r, Math.max(0,g-b), b);
            }
        }

        // Create outBitmap and feed it with the pixels
        bmp.setPixels(outPixels,0, DIM_IMG_SIZE_X, 0,0,DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y);
        Matrix tmpRotMatrix = new Matrix();
        tmpRotMatrix.postRotate(-90); // The model needs the book rotated by 90°; TODO: find out why
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), tmpRotMatrix, true);

        Mat outMat = new Mat();
        bitmapToMat(bmp, outMat);
//        outMat = outMat.t();    //TODO: why?
//        Core.flip(outMat, outMat,0);    //TODO: why?

        List<Mat> outChannels = new ArrayList<Mat>(3);
        Core.split(outMat, outChannels);
        outMat.release();

        return outChannels;
    }

    private MatOfPoint getPointsFromMask(Mat mask) {
        mask.convertTo(mask, CV_32FC1);
        final float[] pixels = new float[mask.width()*mask.height()];
        mask.get(0,0,pixels);
        List<Point> points = new ArrayList<>();

        for (int x = 0; x < mask.width(); ++x) {
            for (int y = 0; y < mask.height(); ++y) {
                final float value = pixels[y + mask.width()*x];
                if (value > 100) {
                    points.add(new Point(x,y));
                }
            }
        }
        final MatOfPoint matOfPoints = new MatOfPoint();
        matOfPoints.fromList(points);

        return matOfPoints;
    }

    private int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private Bitmap decodeSampledBitmapFromUri(Uri uri, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(uri.getPath(), options);


        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight); //TODO: find out why factor 4 would be needed

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(uri.getPath(), options);
    }

    private Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight); //TODO: find out why factor 4 would be needed

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    private void checkIfSinglePage(List<Point> pCorners, Point sTop, Point sBottom, double threshold) {
        final double diffLeft = Math.abs(pCorners.get(0).x - sTop.x) + Math.abs(pCorners.get(3).x - sBottom.x);
        final double diffRight = Math.abs(pCorners.get(1).x - sTop.x) + Math.abs(pCorners.get(2).x - sBottom.x);
        final double diffTot = Math.abs(pCorners.get(0).x - pCorners.get(1).x) + Math.abs(pCorners.get(2).x - pCorners.get(3).x);

        if (diffLeft < threshold*diffTot) {
            pCorners.get(0).x = sTop.x;
            pCorners.get(0).y = sTop.y;
            pCorners.get(3).x = sBottom.x;
            pCorners.get(3).y = sBottom.y;
        } else if (diffRight < threshold*diffTot) {
            pCorners.get(1).x = sTop.x;
            pCorners.get(1).y = sTop.y;
            pCorners.get(2).x = sBottom.x;
            pCorners.get(2).y = sBottom.y;
        } else if (pCorners.get(0).x < sTop.x && pCorners.get(1).x < sTop.x) {
            pCorners.get(1).x = sTop.x;
            pCorners.get(1).y = sTop.y;
            pCorners.get(2).x = sBottom.x;
            pCorners.get(2).y = sBottom.y;
        } else if (pCorners.get(0).x > sTop.x && pCorners.get(1).x > sTop.x) {
            pCorners.get(0).x = sTop.x;
            pCorners.get(0).y = sTop.y;
            pCorners.get(3).x = sBottom.x;
            pCorners.get(3).y = sBottom.y;
        }

    }

    private void interpolateSeperator(List<MatOfPoint> sContours, Mat sMask, List<Point> pCorners,
                                      Point sTop, Point sBottom) {
        Mat sepLine = new Mat();
        Imgproc.fitLine(sContours.get(0), sepLine, Imgproc.CV_DIST_L2, 0,0.01,0.01); //TODO: check what is better
        MatOfPoint sPoints = getPointsFromMask(sMask);
//        Imgproc.fitLine(sPoints, sepLine, Imgproc.CV_DIST_L2, 0,0.01,0.01);

        double x1 = sepLine.get(2,0)[0];
        double y1 = sepLine.get(3,0)[0];
        double x2 = x1 + 100*sepLine.get(0,0)[0];
        double y2 = y1 + 100*sepLine.get(1,0)[0];
        double x3 = pCorners.get(0).x;
        double y3 = pCorners.get(0).y;
        double x4 = pCorners.get(1).x;
        double y4 = pCorners.get(1).y;

//        Formula taken from: https://de.wikipedia.org/wiki/Schnittpunkt#Schnittpunkt_zweier_Geraden
        double xs = ((x4 - x3) * (x2 * y1 - x1 * y2) - (x2 - x1) * (x4 * y3 - x3 * y4)) /
                ((y4 - y3) * (x2 - x1) - (y2 - y1) * (x4 - x3));

        double ys = ((y1 - y2) * (x4 * y3 - x3 * y4) - (y3 - y4) * (x2 * y1 - x1 * y2)) /
                ((y4 - y3) * (x2 - x1) - (y2 - y1) * (x4 - x3));

        sTop.x = xs;
        sTop.y = ys;

        x3 = pCorners.get(2).x;
        y3 = pCorners.get(2).y;
        x4 = pCorners.get(3).x;
        y4 = pCorners.get(3).y;

        xs = ((x4 - x3) * (x2 * y1 - x1 * y2) - (x2 - x1) * (x4 * y3 - x3 * y4)) /
                ((y4 - y3) * (x2 - x1) - (y2 - y1) * (x4 - x3));

        ys = ((y1 - y2) * (x4 * y3 - x3 * y4) - (y3 - y4) * (x2 * y1 - x1 * y2)) /
                ((y4 - y3) * (x2 - x1) - (y2 - y1) * (x4 - x3));

        sBottom.x = xs;
        sBottom.y = ys;
    }

    private void rescaleCorners(Bitmap bmp, Uri uri,
                                List<Point> pCorners,
                                Point sTop, Point sBottom) throws IOException {
        final double heightRatio;
        final double widthRatio;
        final int rotationInDegrees = getExifAngle(uri);

        if (rotationInDegrees == 90 || rotationInDegrees == 270) {
            heightRatio = ((double) bmp.getWidth())/DIM_IMG_SIZE_X;
            widthRatio = ((double) bmp.getHeight())/DIM_IMG_SIZE_Y;
        } else {
            heightRatio = ((double) bmp.getHeight())/DIM_IMG_SIZE_Y;
            widthRatio = ((double) bmp.getWidth())/DIM_IMG_SIZE_X;
        }

        for (Point c: pCorners) {
            c.x = c.x*widthRatio + widthRatio/2.0;
            c.y = c.y*heightRatio + heightRatio/2.0;
        }

        sTop.x = sTop.x*widthRatio + widthRatio/2.0;
        sTop.y = sTop.y*heightRatio + heightRatio/2.0;
        sBottom.x = sBottom.x*widthRatio + widthRatio/2.0;
        sBottom.y = sBottom.y*heightRatio + heightRatio/2.0;
    }

    /**
     * Draws a trapezoid around each page
     * @param bmp The input bitmap (full size)
     * @param mContext The current context
     * @param uri Uri from input file
     * @param pCorners The four corner points of the pages
     * @param sTop The top corner point of the seperator
     * @param sBottom The bottom corner point of the seperator
     * @throws IOException
     */
    private void helperDrawTrapezoid(Bitmap bmp, Context mContext, Uri uri,
                                     List<Point> pCorners,
                                     Point sTop, Point sBottom, int thickness,
                                     String filename) throws IOException {

        Mat tmpOriginMat = new Mat();
        final int rotationInDegrees = getExifAngle(uri);
        Matrix originalMatrix = new Matrix();
        originalMatrix.postRotate(rotationInDegrees);
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), originalMatrix, true);

        bitmapToMat(bmp, tmpOriginMat);

        Imgproc.line(tmpOriginMat, pCorners.get(0), sTop, new Scalar(0, 255, 0), thickness);
        Imgproc.line(tmpOriginMat, sBottom, pCorners.get(3), new Scalar(0, 255, 0), thickness);
        Imgproc.line(tmpOriginMat, pCorners.get(0), pCorners.get(3), new Scalar(0, 255, 0), thickness);
        Imgproc.line(tmpOriginMat, sTop, pCorners.get(1), new Scalar(0, 255, 0), thickness);
        Imgproc.line(tmpOriginMat, pCorners.get(1), pCorners.get(2), new Scalar(0, 255, 0), thickness);
        Imgproc.line(tmpOriginMat, sBottom, pCorners.get(2), new Scalar(0, 255, 0), thickness);
        Imgproc.line(tmpOriginMat, sTop, sBottom, new Scalar(0, 0, 255), thickness);
        matToBitmap(tmpOriginMat, bmp);

        saveBitmap(bmp, filename, mContext);
    }

    private List<Point> getCorners2(List<MatOfPoint> contours) {

        final double angleThreshold = Math.PI; // 180 degrees
        final List<Point> pPolygonList = contours.get(0).toList();
        List<Point> cornerCandidates = new ArrayList<>();

        if (pPolygonList.size() > 2) {
            Point p1 = pPolygonList.get(pPolygonList.size()-1);
            Point p2 = pPolygonList.get(0);
            Point p3;

            for (int i = 0; i < pPolygonList.size(); ++i) {
                if (i == pPolygonList.size()-1) {
                    p3 = pPolygonList.get(0);
                } else {
                    p3 = pPolygonList.get(i+1);
                }

                //Calculate angle: compute p1-p2 and p3-p2 to obtain vector to the points p1,p3 from p2.
                //then calculate the dot product and compute the angle with the arccos function.
                //cos(alpha) = dot(v1,v3)/(length(v1)*length(v3))

                final Point v1 = new Point(p1.x-p2.x, p1.y-p2.y);
                final Point v3 = new Point(p3.x-p2.x, p3.y-p2.y);
                final double l1 = Math.sqrt(Math.pow(v1.x,2) + Math.pow(v1.y,2));
                final double l3 = Math.sqrt(Math.pow(v3.x,2) + Math.pow(v3.y,2));

                final double alpha = Math.acos((v1.x*v3.x + v1.y*v3.y)/(l1*l3));

                if (Math.abs(alpha) < angleThreshold) {
                    cornerCandidates.add(p2);
//                    Log.d(TAG, "x = "+Double.toString(p2.x)+"; y = "+Double.toString(p2.y)+"; angle = "+Double.toString(alpha));
                }

                p1 = p2;
                p2 = p3;
            }
        } else if (pPolygonList.size() == 2){
            cornerCandidates.add(pPolygonList.get(0));
            cornerCandidates.add(pPolygonList.get(1));
        } else {
            cornerCandidates.add(pPolygonList.get(0));
        }

        final Point c1 = new Point(0,0);
        final Point c2 = new Point(DIM_IMG_SIZE_X,0);
        final Point c3 = new Point(DIM_IMG_SIZE_X,DIM_IMG_SIZE_Y);
        final Point c4 = new Point(0,DIM_IMG_SIZE_Y);
        final List<Point> imageCorners = new ArrayList<>(Arrays.asList(c1,c2,c3,c4));

        List<Point> corners = new ArrayList<>();

        for (Point c: imageCorners) {
            boolean first = true;
            double distOld = DIM_IMG_SIZE_X+DIM_IMG_SIZE_Y;
            for (Point cornerCandidate: cornerCandidates) {
                double distNew = Math.sqrt(Math.pow(c.x-cornerCandidate.x,2) + Math.pow(c.y-cornerCandidate.y,2));
                if (first) {
                    corners.add(new Point(cornerCandidate.x, cornerCandidate.y));
                    distOld = distNew;
                    first = false;
                } else if (distNew < distOld) {
                    corners.remove(corners.size()-1);
                    corners.add(new Point(cornerCandidate.x, cornerCandidate.y));
                    distOld = distNew;
                }
            }
        }


//        Log.d(TAG, "corners found");
//        for (Point c: corners) {
//            Log.d(TAG, "x:"+Double.toString(c.x)+"  y:"+Double.toString(c.y));
//        }
        return corners;
    }

    private List<MatOfPoint> findPolygonalRegions(Mat mask, double minArea, int epsilon, boolean polygon) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE );

        // Remove small contours
        final double minA = minArea*DIM_IMG_SIZE_X*DIM_IMG_SIZE_Y;
        for (int i = contours.size(); i > 0; --i) {
            if (Imgproc.contourArea(contours.get(i-1)) < minA) {
                contours.remove(contours.get(i-1));
            }
        }

        //sort by area: https://stackoverflow.com/questions/18939856/using-comparator-without-adding-class
        Collections.sort(contours, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint c1, MatOfPoint c2) {
                return (int) (Imgproc.contourArea(c2) - Imgproc.contourArea(c1));
            }
        });

        /* Combine the two largest contours if they are of similar size
           (This is two make sure that we find both pages together and because the page split
            is often seperated in two parts, top and bottom. */
        if (contours.size() > 1) {
            if (Imgproc.contourArea(contours.get(1))/Imgproc.contourArea(contours.get(0)) > 0.2) {
                List<Point> cList = new ArrayList<>();
                cList.addAll(contours.get(0).toList());
                cList.addAll(contours.get(1).toList());

                MatOfPoint cMat = new MatOfPoint();
                cMat.fromList(cList);

                List<MatOfPoint> cMatList = new ArrayList<>();
                cMatList.add(cMat);

                contours = cMatList;
            }
        }

        // For the pages we want to approximate the contours with a polygon:
        if (polygon == true) {
            // Compute the convex hulls
            List<MatOfPoint> hulls = new ArrayList<>(contours.size());
            for (MatOfPoint c: contours) {
                MatOfInt h = new MatOfInt();
                Imgproc.convexHull(c, h);

                MatOfPoint mopOut = new MatOfPoint();
                mopOut.create((int)h.size().height,1, CvType.CV_32SC2);

                for(int i = 0; i < h.size().height ; ++i)
                {
                    int index = (int)h.get(i, 0)[0];
                    double[] point = new double[] {
                            c.get(index, 0)[0], c.get(index, 0)[1]
                    };
                    mopOut.put(i, 0, point);
                }
                hulls.add(mopOut);
            }

            // Approximate the hulls with a polygon
            List<MatOfPoint> pApproxPolys = new ArrayList<>();
            for (MatOfPoint c : hulls) {
                MatOfPoint2f pApproxPoly2f = new MatOfPoint2f();
                Imgproc.approxPolyDP(new MatOfPoint2f(c.toArray()), pApproxPoly2f, epsilon, true);
                MatOfPoint pApproxPoly = new MatOfPoint(pApproxPoly2f.toArray());
                pApproxPolys.add(pApproxPoly);
            }

            return pApproxPolys;
        } else {
            return contours;
        }

    }

    private Mat bwClean(Mat mask, int size) {
        final Mat kernel = new Mat(new Size(size,size), CV_8UC1, new Scalar(255));
        Mat tmp = new Mat();
        Mat out = new Mat();

        Imgproc.morphologyEx(mask, tmp, Imgproc.MORPH_OPEN, kernel);
        Imgproc.morphologyEx(tmp, out, Imgproc.MORPH_CLOSE, kernel);

        return out;
    }

    private Mat applyThreshold(Mat img, int threshold) {
        Mat mask = new Mat();
        if (threshold < 0) {
            Imgproc.threshold(img, mask, 0, 255, Imgproc.THRESH_BINARY+ Imgproc.THRESH_OTSU);
        } else {
            Imgproc.threshold(img, mask, threshold, 255, Imgproc.THRESH_TOZERO);
        }

        return mask;
    }

    /** Save Bitmap in seperate folder named FOLDER_NAME shown in gallery */
    private void saveBitmap(final Bitmap bitmap, final String name, Context mContext) {
        final String filename = name+".jpg";
        File mediaDir = Helper.getMediaStorageDir(FOLDER_NAME);//mContext.getString(R.string.app_name));

        final File file = new File(mediaDir, filename);
        if (file.exists()) {
            file.delete();
//            Log.d(TAG, "Existing File overwritten");
        }
        try {
            final FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (final Exception e) {
            Log.e(TAG, "Failed to save the file");
        }
        mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
        Log.d(TAG, "PageSplit output image saved under " + filename);
    }

    /** Memory-map the model file in Assets. */
    private static MappedByteBuffer loadModelFile(Context mContext) throws IOException {
        AssetFileDescriptor fileDescriptor = mContext.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private int getExifAngle(Uri uri) throws IOException {
        final ExifInterface exif = new ExifInterface(uri.getPath());
        final int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        int rotation = 0;

        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            rotation = 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            rotation =  180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            rotation =  270;
        }

//        return rotation;
        if (book_c < 3 || book_c == 6 || book_c == 10 || book_c == 11) {//TODO: change back to the line above
            return 180;
        } else {
            return 90;
        }
    }

    /**
     * Closes tflite to release resources.
     */
    public void close() {
        mTflite.close();
        mTflite = null;
    }
}

