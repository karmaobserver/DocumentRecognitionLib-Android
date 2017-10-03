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
import static org.opencv.imgproc.Imgproc.MORPH_DILATE;
import static org.opencv.imgproc.Imgproc.MORPH_ELLIPSE;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.getStructuringElement;

/**
 *  Detect whether provided image is document or picture;
 *  if the image is a document, it prepares it for OCR
 *
 */
@EBean
public class DocumentDetector {

    // define maximum border size of the provided image
    // if a border is larger than defined value, image is scaled
    private static final int PREFERRED_SIZE = 1000;

    private static final String TAG = "DocumentDetector";

    @RootContext
    Context context;


    /**
     * Converts original image to gray
     *
     * @param originalMat
     * @return grayMat
     */
    private Mat convertToGrayImage(Mat originalMat) {
        Mat grayMat = new Mat(originalMat.cols(), originalMat.rows(), CvType.CV_8U, new Scalar(1));
        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_RGB2GRAY, 1);

        return grayMat;
    }


    /**
     * Detects if the image is document or picture
     *
     * Suggested values for parameters:
     *
     * @param originalMat   original Mat object
     * @param tolerance     threshold value for converting gray scale image to binary image
     *                      suggested value for this parameter: 100
     * @param percentage    minimum percentage of white pixels that image needs to have to be a document
     *                      suggested value for this parameter: 40f
     * @param regions       minimum number of regions that image needs to have to be a document
     *                      suggested value for this parameter: 82
     *
     * @return True if the image is document, False otherwise
     */
    public boolean detectDocument(Mat originalMat, Integer tolerance, Float percentage, Integer regions) {

        boolean isWhite;

        Mat grayMat = convertToGrayImage(originalMat);

        // check image size and scale it if necessary
        grayMat = checkImageSize(grayMat);

        Mat thresholdMat = convertToBinaryImage(grayMat, tolerance);

        int whitePixels = countNonZero(thresholdMat);
        int blackPixels = thresholdMat.cols() * thresholdMat.rows() - whitePixels;

        int pixels = blackPixels + whitePixels;
        int pixelsPercentage = (int) (pixels * (percentage * 1.0/ 100.0f));

        // check if the number of white pixels is greater than provided percentage
        if (pixelsPercentage < whitePixels) {
            isWhite = true;
            Log.d(TAG, "There are more then " + percentage + "% white pixels");
        } else {
            Log.d(TAG, "There are less then " + percentage + "% white pixels");
            return false;
        }

        Mat morphMat = applyMorphologicalGradient(grayMat);

        // using Gaussian algorithm to choose the optimal threshold value to convert the processed image to binary image.
        Mat thresholdWithMorphMat = convertImageUsingAdaptiveThreshold(thresholdMat, morphMat);

        // apply Closing Morphological Transformation
        Mat morphClosingMat = applyClosingMorphologicalTransformation(thresholdWithMorphMat);

        List<MatOfPoint> contours = new ArrayList<>();

        Mat hierarchy = new Mat();

        // find contours
        Imgproc.findContours(morphClosingMat, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        Log.d(TAG, "Number of Regions: " + contours.size());

        // if there are more white pixels than defined and if there are more contours than defined,
        // return true: provided image is document; return false otherwise
        if (isWhite && contours.size() > regions) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Converts image to Binary image using given tolerance
     *
     * @param grayMat           given Mat object
     * @param tolerance         threshold value for converting grayMat to binary mat
     * @return  thresholdMat    converted Mat object
     */
    private Mat convertToBinaryImage(Mat grayMat, Integer tolerance) {
        Mat thresholdMat = new Mat(grayMat.cols(), grayMat.rows(), CvType.CV_8U, new Scalar(1));
        Imgproc.threshold(grayMat, thresholdMat, tolerance, 255, Imgproc.THRESH_BINARY);

        return thresholdMat;
    }

    /**
     * Applies Morphological Gradient on given Mat object
     *
     * @param grayMat
     * @return  morphMat
     */
    private Mat applyMorphologicalGradient(Mat grayMat) {

        Mat morphMat = new Mat(grayMat.cols(), grayMat.rows(), CvType.CV_8U, new Scalar(1));
        Mat morphStructure = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        Imgproc.morphologyEx(grayMat, morphMat, Imgproc.MORPH_GRADIENT, morphStructure);

        return morphMat;
    }

    /**
     * Applies adaptive threshold on given Mat object using Gaussian method
     *
     * @param thresholdMat
     * @param morphMat
     * @return  thresholdWithMorphMat
     */
    private Mat convertImageUsingAdaptiveThreshold(Mat thresholdMat, Mat morphMat) {
        Mat thresholdWithMorphMat = new Mat(thresholdMat.cols(), thresholdMat.rows(), CvType.CV_8U, new Scalar(1));
        Imgproc.adaptiveThreshold(morphMat, thresholdWithMorphMat, 255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 21, 12);

        return thresholdWithMorphMat;
    }

    /**
     * Applies Closing Morphological Transformation on the given Mat object
     *
     * @param thresholdWithMorphMat
     * @return morphClosingMat
     */
    private Mat applyClosingMorphologicalTransformation(Mat thresholdWithMorphMat) {
        Mat morphClosingMat = new Mat(thresholdWithMorphMat.cols(), thresholdWithMorphMat.rows(), CvType.CV_8U, new Scalar(1));
        Mat morphStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(15, 1));
        Imgproc.morphologyEx(thresholdWithMorphMat, morphClosingMat, Imgproc.MORPH_CLOSE, morphStructure);

        return morphClosingMat;
    }


    /**
     * Checks if the image should be scaled and resize it if necessary
     *
     * @param imageMat
     * @return retVal       returns scaled image if resizing was necessary,
     * otherwise returns imageMat
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
     * Scales provided image to the predefined size
     *
     * @param imageMat Mat object to be scaled
     * @return Mat object of scaled image
     */
    private Mat scalePicture(Mat imageMat) {

        int pictureWidth = imageMat.width();
        int pictureHeight = imageMat.height();
        double scale = 0.0;

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
     * Prepares document for OCR.
     *
     * @param originalMat
     * @return thresholdWithGaussian
     */
    public Mat prepareDocumentForOCR(Mat originalMat) {

        // convert to gray
        Mat grayMat = new Mat(originalMat.cols(), originalMat.rows(), CvType.CV_8U, new Scalar(1));
        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_RGB2GRAY, 1);

        Mat thresholdWithMean = new Mat(originalMat.cols(), originalMat.rows(), CvType.CV_8U, new Scalar(1));

        // apply Mean method
        Imgproc.adaptiveThreshold(grayMat, thresholdWithMean, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 201, 24);

        Imgproc.erode(thresholdWithMean, thresholdWithMean, getStructuringElement(MORPH_DILATE, new Size(2, 2)));

        Imgproc.dilate(thresholdWithMean, thresholdWithMean, getStructuringElement(MORPH_DILATE, new Size(2, 2)));

        return thresholdWithMean;
    }
}