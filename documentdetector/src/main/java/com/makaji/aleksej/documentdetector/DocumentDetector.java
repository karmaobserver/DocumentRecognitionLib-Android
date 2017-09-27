package com.makaji.aleksej.documentdetector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TimingLogger;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.android.LoaderCallbackInterface.SUCCESS;
import static org.opencv.core.Core.countNonZero;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;

/**
 * Created by Aleksej on 9/21/2017.
 */

@EBean
public class DocumentDetector {

    private static final int PREFERRED_SIZE = 800;

    private static final String TAG = "DocumentDetector";

    @RootContext
    Context context;

    /**
     * Detect document, if it is a document, prepare it for OCR otherwise make smooth Image.
     *
     * @param originalMat Mat which should be processed
     * @param tolerance   Tolerance which will be used as threshold
     * @param percentage  Percentage which defines how many white pixels needs to be contained in document to be valid document
     * @param regions     Regions number which defines how many regions needs to be contained in document to be valid document
     * @return Mat which is prepared for OCR in case Mat is a document, otherwise return MAT as smooth Image.
     */
    public Mat detectAndPrepareDocument(Mat originalMat, Integer tolerance, Float percentage, Integer regions) {

        Mat resultMat;

        //Check if image is document
        if (detectDocument(originalMat, tolerance, percentage, regions)) {

            //If image is document, prepare document for OCR
            resultMat = prepareDocumentForOCR(originalMat);
        } else {

            //If image is not document, prepare smooth image
            resultMat = prepareSmoothImage(originalMat);
        }

        return resultMat;
    }

    private Mat prepareSmoothImage(Mat originalMat) {
        //TODO: make alogrithm for making smooth image if it is not a document
        return originalMat;
    }

    /**
     * Detect if Image is an document.
     *
     * @param originalMat Mat which should be processed
     * @return True if it is document, else false
     */
    public boolean detectDocument(Mat originalMat, Integer tolerance, Float percentage, Integer regions) {

        boolean isWhite;

        // Convert to gray
        Mat grayMat = new Mat(originalMat.cols(), originalMat.rows(), CvType.CV_8U, new Scalar(1));
        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_RGB2GRAY, 1);

        //checks image size and scale it if necessary
        grayMat = checkImageSize(grayMat);

        //Apply threshold to convert to binary image.
        Mat thresholdMat = new Mat(originalMat.cols(), originalMat.rows(), CvType.CV_8U, new Scalar(1));
        Imgproc.threshold(grayMat, thresholdMat, tolerance, 255, Imgproc.THRESH_BINARY);

        int whitePixels = countNonZero(thresholdMat);
        int blackPixels = thresholdMat.cols() * thresholdMat.rows() - whitePixels;

        //check if white pixels are more then 40%
        int pixels = blackPixels + whitePixels;
        int pixelsPercentage = (int) (pixels * (percentage / 100.0f));

        if (pixelsPercentage < whitePixels) {
            isWhite = true;
            Log.d(TAG, "There are more then " + percentage + "% white pixels");
        } else {
            Log.d(TAG, "There are less then " + percentage + "% white pixels");
            return false;
        }

        //Apply Morphological Gradient.
        Mat morphMat = new Mat(originalMat.cols(), originalMat.rows(), CvType.CV_8U, new Scalar(1));
        Mat morphStructure = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        Imgproc.morphologyEx(grayMat, morphMat, Imgproc.MORPH_GRADIENT, morphStructure);

        // Apply threshold to convert to binary image.
        // Using Otsu algorithm to choose the optimal threshold value to convert the processed image to binary image.
        Mat thresholdWithMorphMat = new Mat(thresholdMat.cols(), thresholdMat.rows(), CvType.CV_8U, new Scalar(1));
        Imgproc.threshold(morphMat, thresholdWithMorphMat, 0.0, 255.0, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        //Apply Closing Morphological Transformation
        Mat morphClosingMat = new Mat(thresholdMat.cols(), thresholdMat.rows(), CvType.CV_8U, new Scalar(1));
        morphStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(15, 1));
        Imgproc.morphologyEx(thresholdWithMorphMat, morphClosingMat, Imgproc.MORPH_CLOSE, morphStructure);

        List<MatOfPoint> contours = new ArrayList<>();

        Mat hierarchy = new Mat();

        //Find contours
        Imgproc.findContours(morphClosingMat, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        Log.d(TAG, "Number of Regions: " + contours.size());

        //If we want to draw rectangles on image. (If we scaling picture, make sure you draw on correct image)
        /*if (drawRegions) {

            Mat mask = Mat.zeros(thresholdWithMorphMat.size(), CvType.CV_8UC1);

            for (int idx = 0; idx < contours.size(); idx++) {

                Rect rect = Imgproc.boundingRect(contours.get(idx));

                Mat maskROI = new Mat(mask, rect);

                maskROI.setTo(new Scalar(0, 0, 0));

                //takes 1-2 ms per contour
                Imgproc.drawContours(mask, contours, idx, new Scalar(255, 255, 255), Core.FILLED);

                double r = (double) Core.countNonZero(maskROI) / (rect.width * rect.height);

                if (r > .45 && (rect.height > 8 && rect.width > 8)) {
                    Imgproc.rectangle(originalMat, rect.br(), new Point(rect.br().x - rect.width, rect.br().y - rect.height), new Scalar(20, 4, 201));
                }
            }
        }*/

        //If there are more white pixels as defined and if there are more contours as defined, return true as valid document
        if (isWhite && contours.size() > regions) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if the picture should be scaled
     *
     * @param imageMat Mat which will be scaled
     */
    private Mat checkImageSize(Mat imageMat) {

        Mat retVal;

        if (imageMat.height() > PREFERRED_SIZE || imageMat.width() > PREFERRED_SIZE) {

            //it takes less than 20 milliseconds for scaling
            retVal = scalePicture(imageMat);

        } else {
            retVal = imageMat;
        }

        return retVal;
    }

    /**
     * Picture scaling - if at least one picture's dimension is bigger than PREFERRED_SIZE,
     * the picture will be scaled
     *
     * @param imageMat Mat which will be scaled
     */
    private Mat scalePicture(Mat imageMat) {

        int pictureWidth = imageMat.width();
        int pictureHeight = imageMat.height();
        double scale;

        if (pictureWidth >= pictureHeight) {
            scale = PREFERRED_SIZE * 1.0 / pictureWidth;
        } else {
            scale = PREFERRED_SIZE * 1.0 / pictureHeight;
        }

        Size szResized = new Size(imageMat.cols() * scale, imageMat.rows() * scale);

        Mat destination = new Mat();

        Imgproc.resize(imageMat, destination, szResized, 0, 0, Imgproc.INTER_LINEAR);

        return destination;

    }

    /**
     * Prepare document for OCR.
     * Convert Mat to gray then do threshold based on OTSU algorithm.
     *
     * @param originalMat Original Mat
     * @return Transformed Mat
     */
    private Mat prepareDocumentForOCR(Mat originalMat) {

        // Convert to gray
        Mat grayMat = new Mat(originalMat.cols(), originalMat.rows(), CvType.CV_8U, new Scalar(1));
        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_RGB2GRAY, 1);

        Mat thresholdWithMeanC = new Mat(originalMat.cols(), originalMat.rows(), CvType.CV_8U, new Scalar(1));

        /*
        Imgproc.threshold(grayMat, thresholdWithMorphMat, 0.0, 255.0, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        */

        Imgproc.adaptiveThreshold(grayMat, thresholdWithMeanC, 255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 3, 16);

        return thresholdWithMeanC;
    }


}

