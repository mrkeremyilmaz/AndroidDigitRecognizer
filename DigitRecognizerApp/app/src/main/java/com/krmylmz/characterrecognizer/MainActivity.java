package com.krmylmz.characterrecognizer;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.TextView;

import com.krmylmz.characterrecognizer.drawing.DrawView;
import com.krmylmz.characterrecognizer.models.Classification;
import com.krmylmz.characterrecognizer.models.Classifier;
import com.krmylmz.characterrecognizer.models.TensorFlowClassifier;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.tensorflow.Tensor;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.opencv.imgproc.Imgproc.resize;

public class MainActivity extends Activity {

    // MNIST dataset property
    private static final int PIXEL_WIDTH = 28;

    // UI Elements
    private DrawView drawView;
    private TextView textView;

    // Classifier list for pre-trained models
    private List<Classifier> mClassifiers = new ArrayList<>();

    // Float array of pixels for processed bitmap, will be fed into the model
    private float[] fPixels;

// TODO: Can we do it without native part?? for size reduction
//    static {
//        System.loadLibrary("native-lib");
//    }

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.i("ERROR_OpenCV", "OpenCV is not successfully loaded!");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawView = findViewById(R.id.drawView);
        textView = findViewById(R.id.resultText);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        drawView.init(metrics);

        // check if api > 23
        if(Build.VERSION.SDK_INT > 23)
        {
            //123 is just an ID for the request, no magic number
            int REQUEST_CODE_ASK_PERMISSIONS = 123;

            int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_ASK_PERMISSIONS);
            }
        }

        loadModel();
    }

    /**
     * Calls drawView's clear method which clears the drawing canvas.
     * Also sets the result text to empty string "".
     * @param v necessary for being an onClick function (for clearButton)
     */
    public void clearDisplay(View v) {
        drawView.clear();
        textView .setText("");
    }

    /**
     * The drawView canvas is width*width according to your phone model. However,
     * MNIST dataset is formed by 28x28 images with three specifications:
     *      1. The drawing is black with white background.
     *      2. The drawing part is 20x20 and its in the center.
     *      3. Around the drawing(20x20) there is white padding to make it 28x28.
     */
    public void processBitmap() {

        Preprocessor preprocessor = new Preprocessor(drawView.getBitmap(), 28);

        Mat resizedMat = preprocessor.resize();
        Bitmap resizedBitmap = preprocessor.matToBitmap(resizedMat);

        double[] bgColor = {255,255,255};
        int[] rowBounds = preprocessor.getMatBoundariesVertical(bgColor);
        int[] colBounds = preprocessor.getMatBoundariesHorizontal(bgColor);

        Log.i("TODO_1:rowBound:0:upper", "" + rowBounds[0]);
        Log.i("TODO_1:rowBound:1:lower", "" + rowBounds[1]);

        Log.i("TODO_1:colBound:0:left", "" + colBounds[0]);
        Log.i("TODO_1:colBound:1:right", "" + colBounds[1]);

        resizedMat = preprocessor.scale(colBounds[0], colBounds[1], rowBounds[0], rowBounds[1], 20);

        resizedBitmap = preprocessor.matToBitmap(resizedMat);

        Bitmap finalBitmap = preprocessor.fillBackground();

        saveBitmapForDebugging(resizedBitmap, "resized");
        // save bitmap for debugging ROFL
        saveBitmapForDebugging(finalBitmap, "final");

        // initialize pixel array (for int 28x28)
        int[] iPixels = new int[28 * 28];
        finalBitmap.getPixels(iPixels, 0, 28, 0, 0, 28, 28);


        // convert the int pixels to float pixels & divide by 255.0
        fPixels = new float[iPixels.length];
        for (int i = 0; i < iPixels.length; ++i) {
            // Set 0 for white and 255 for black pixel
            int pix = iPixels[i];
            int b = pix & 0xff; // filter with 255
            fPixels[i] = (float) ((0xff - b) / 255.0);
        }

    }

    /**
     * Takes the processed bitmap and saves it to externalStorage/Documents/test.png
     * Only the last detected bitmap stays, i.e. always overwrites previous file if exists.
     * @param bitmap the new processed bitmap (28x28) to be saved
     */
    private void saveBitmapForDebugging(Bitmap bitmap, String name){
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/Pictures/" + name +".png");
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            // PNG is a lossless format, the compression factor (100) is ignored
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (out != null) {
                    out.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * A method that is called when detectButton is pressed. Calls processBitmap,
     * makes the classification using Classifier class, prints it to the screen.
     * @param v necessary for being an onClick function (for detectButton)
     */
    public void detectClass(View v) {

        processBitmap();

        // LOG
        Log.i("INFO", "start detectClass");

        // initialize result string
        String text = "Result: ";

        // For any number of classifiers u like
        for (Classifier classifier : mClassifiers) {
            // recognize the image giving float flattened array of pixels
            final Classification res = classifier.recognize(fPixels);
            // if classification fails, put a question mark (?)

            if (res.getLabel() == null) {
                text += classifier.name() + ": ?\n";
            }

            else {
                // else output the classification result with its confidence
                text += String.format(Locale.ENGLISH, "%s: %s, %f\n",
                                                        classifier.name(),
                                                        res.getLabel(),
                                                        res.getConf());
            }
        }

        // Display the result to the user
        textView.setText(text);

        // LOG
        Log.i("INFO", "end detectClass");
    }


    /**
     * Uses TensorFlowClassifier to load the pre-trained models for Android use.
     * We need to specify modelPath, labelfile, input_node name, output_none name etc.
     */
    private void loadModel() {
        //The Runnable interface is another way in which you can implement multi-threading other than extending the
        // //Thread class due to the fact that Java allows you to extend only one class. Runnable is just an interface,
        // //which provides the method run.
        // //Threads are implementations and use Runnable to call the method run().
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //add n classifiers to our classifier arraylist
                    //i.e. the tensorflow classifier and the keras classifier
//                    mClassifiers.add(
//                            TensorFlowClassifier.create(getAssets(), "Keras",
//                                    "opt_mnist_convnet-keras.pb", "labels.txt", PIXEL_WIDTH,
//                                    "dense_1_input_2", "output_node0", false));
//                    mClassifiers.add(
//                            TensorFlowClassifier.create(getAssets(), "Keras-old",
//                                    "opt_mnist_convnet-keras.pb", "labels.txt", PIXEL_WIDTH,
//                                    "conv2d_1_input", "dense_2/Softmax", false));
                    mClassifiers.add(
                            TensorFlowClassifier.create(getAssets(), "",
                                    "opt_1608_mnist_convnet.pb", "labels.txt", PIXEL_WIDTH,
                                    "conv2d_7_input", "dense_6/Softmax", false));
                }

                catch (final Exception e) {
                    //if they aren't found, throw an error!
                    throw new RuntimeException("Error initializing classifiers!", e);
                }
            }
        }).start();
    }
}