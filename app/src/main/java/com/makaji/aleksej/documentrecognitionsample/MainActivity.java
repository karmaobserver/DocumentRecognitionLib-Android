package com.makaji.aleksej.documentrecognitionsample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.makaji.aleksej.documentdetector.DocumentDetector;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.ViewById;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.io.IOException;
import java.io.InputStream;

import static org.opencv.android.LoaderCallbackInterface.SUCCESS;

import org.opencv.android.OpenCVLoader;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {

    private static final String DATASET_NAME = "dataset";

    @Bean
    DocumentDetector documentDetector;

    @ViewById
    ImageView imageThresholdTolerance;

    @ViewById
    ImageView imageThresholdOTSU;

    // async loader of OpenCV4Android lib
    private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case SUCCESS: {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, loaderCallback);
            OpenCVLoader.initDebug();
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            loaderCallback.onManagerConnected(SUCCESS);
        }
    }

    @AfterViews
    void init() {
    }

    @Click
    void detectAndPrepare() {

        //Get image and cover to Mat (simulation what i get from other developer)
        Mat imageMat = getImageMat();

        Mat processedMat;

        processedMat = documentDetector.detectAndPrepareDocument(imageMat, 210, 40.0f, 1);
        convertMatToBitmapAndDrawImages(processedMat, imageMat);

    }

    private Mat getImageMat() {

        InputStream inputstream = null;

        try {
            inputstream = getApplicationContext().getAssets().open(DATASET_NAME + "/" + getApplicationContext().getAssets().list(DATASET_NAME)[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Bitmap bitmap = BitmapFactory.decodeStream(inputstream);

        // first convert bitmap into OpenCV mat object
        Mat imageMat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8U, new Scalar(4));
        Bitmap myBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(myBitmap, imageMat);

        return imageMat;
    }

    private void convertMatToBitmapAndDrawImages(Mat matTolerance, Mat matOTSU) {

        // convert back to bitmap for displaying
        Bitmap resultBitmapTolerance = Bitmap.createBitmap(matTolerance.cols(), matTolerance.rows(), Bitmap.Config.ARGB_8888);
        matTolerance.convertTo(matTolerance, CvType.CV_8UC1);
        Utils.matToBitmap(matTolerance, resultBitmapTolerance);

        Drawable newImageTolerance = new BitmapDrawable(resultBitmapTolerance);
        //It scales the image after use, so i used code above which is departed
        //Drawable newImage = new BitmapDrawable(getResources(), resultBitmap);

        imageThresholdTolerance.setImageDrawable(newImageTolerance);

        // convert back to bitmap for displaying
        Bitmap resultBitmapOTSU = Bitmap.createBitmap(matOTSU.cols(), matOTSU.rows(), Bitmap.Config.ARGB_8888);
        matOTSU.convertTo(matOTSU, CvType.CV_8UC1);
        Utils.matToBitmap(matOTSU, resultBitmapOTSU);

        Drawable newImageOTSU = new BitmapDrawable(resultBitmapOTSU);
        //It scales the image after use, so i used code above which is departed
        //Drawable newImage = new BitmapDrawable(getResources(), resultBitmap);

        imageThresholdOTSU.setImageDrawable(newImageOTSU);
    }
}
