package com.krmylmz.characterrecognizer;


// Android imports
import android.graphics.Bitmap;
import android.util.Log;

// OpenCV imports
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class Preprocessor {

    // Constants
    private final int PIXEL_SIZE;
    private final int CONFIG = CvType.CV_8UC1;

    // Member variables
    private Mat matToBeProcessed;

    //Ctors
    /**
     * Converts bitmapIn to Mat and stores it in matToBeProcessed
     * @param bitmapIn the copy of drawView bitmap
     * @param pixel_size the pixel size that all the operations will be scaled to
     */
    public Preprocessor(Bitmap bitmapIn, int pixel_size){
        this.PIXEL_SIZE = pixel_size;

        // convert bitmapIn to mat (for OpenCV operations)
        matToBeProcessed= new Mat(bitmapIn.getWidth(), bitmapIn.getHeight(), CONFIG);
        Utils.bitmapToMat(bitmapIn, matToBeProcessed);
    }

    /**
     * Returns matToBeProcessed
     * @return matToBeProcessed
     */
    public Mat getMat() {
            return matToBeProcessed;
    }

    /**
     * Resizes the given Mat to given metrics, then returns it
     * @param matIn Mat to be resized
     * @param width self-explanatory
     * @param height self-explanatory
     * @return resized Mat
     */
    public Mat resize(Mat matIn, int width, int height, int CvType_config){

        Mat matOut = new Mat(width, height, CvType_config);
        Size size = new Size(width, height);

        Imgproc.resize(matIn, matOut, size);

        matToBeProcessed = matOut;
        return matToBeProcessed;
    }

    /**
     * resize method that uses class constants for matIn
     * @param matIn
     * @return
     */
    public Mat resize(Mat matIn){

        Mat matOut = new Mat(PIXEL_SIZE, PIXEL_SIZE, CONFIG);
        Size size = new Size(PIXEL_SIZE, PIXEL_SIZE);

        Imgproc.resize(matIn, matOut, size);

        matToBeProcessed = matOut;
        return matToBeProcessed;
    }

    /**
     * Default resize method
     * @return resized matToBeProcessed
     */
    public Mat resize(){

        Mat matOut = new Mat(PIXEL_SIZE, PIXEL_SIZE, CONFIG);
        Size size = new Size(PIXEL_SIZE, PIXEL_SIZE);

        Imgproc.resize(matToBeProcessed, matOut, size);

        matToBeProcessed = matOut;
        return matToBeProcessed;
    }

    /**
     * Converts matIn to Bitmap for Java(Android) use
     * @param matIn mat to be converted
     * @return bitmap
     */
    public Bitmap matToBitmap(Mat matIn){

        Bitmap newBitmap = Bitmap.createBitmap(matIn.width(), matIn.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matIn, newBitmap);

        return newBitmap;
    }

    /**
     * Converts matToBeProcessed to Bitmap for Java(Android) use
     * @return bitmap
     */
    public Bitmap matToBitmap(){

        Bitmap newBitmap = Bitmap.createBitmap(matToBeProcessed.width(), matToBeProcessed.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matToBeProcessed, newBitmap);

        return newBitmap;
    }

    /**
     * 1. compute the row pixel value sum
     * 2. compare sum[i] with (Mat.rows() * backgroundColorRGB[i])
     * 3. return first rows that doesn't match
     * (Detailed explanations are in function implementation)
     * 
     * @param backgroundColorRGB double array with 3 length ( RED, GREEN, BLUE)
     *                          in range (0.0 ,255.0)
     * @return the upper and lower bounds of drawing to extract background (find the desired area)
     */
    public int[] getMatBoundariesVertical(double[] backgroundColorRGB){

        double[] rowSum = {0, 0, 0};
        double[] data;

        int[] rowBounds = new int[2];
        rowBounds[1] = matToBeProcessed.rows() - 1; // lower bound _ start with max
        rowBounds[0] = 0;                           // upper bound _ start with min

        boolean foundUpperBound = false;
        boolean foundLowerBound = false;

        // checks for R, G, B values
        double checkIndex0 = matToBeProcessed.rows() * backgroundColorRGB[0];
        double checkIndex1 = matToBeProcessed.rows() * backgroundColorRGB[1];
        double checkIndex2 = matToBeProcessed.rows() * backgroundColorRGB[2];
        double[] checkIndex = {checkIndex0, checkIndex1, checkIndex2};


        for (int row = 0; row < matToBeProcessed.rows() && !foundUpperBound; ++row) {
            for (int col = 0; col < matToBeProcessed.cols(); ++col) {
                data = matToBeProcessed.get(row, col);
                for (int rgb_index = 0; rgb_index < rowSum.length; ++rgb_index) {
                    rowSum[rgb_index] += data[rgb_index];
                }
            }
            for(int checkSum = 0; checkSum < rowSum.length; ++checkSum) {
                // if sum of R, G, B values of every pixel in a row
                // equals the corresponding checksum and foundUpperBound is false,
                // this means this row only contains the background
                // i.e. no user interaction/drawing in that row

                // so if rowSum is not equal to corresponding checkSum
                // and upperBoundFound is false
                // that row is where user interaction/drawing starts
                // ie our upper bound
                if (rowSum[checkSum] != checkIndex[checkSum] && !foundUpperBound){
                        rowBounds[0] = row;
                        foundUpperBound = true;
                }
            }

            // reinitialize rowSums to 0
            for (int index = 0; index < rowSum.length; ++index){
                rowSum[index] = 0;
            }
        }


        for (int row = matToBeProcessed.rows() - 1; row >= 0 && !foundLowerBound; --row) {
            for (int col = 0; col < matToBeProcessed.cols(); ++col) {
                data = matToBeProcessed.get(row, col);
                for (int rgb_index = 0; rgb_index < rowSum.length; ++rgb_index) {
                    rowSum[rgb_index] += data[rgb_index];
                }
            }
            for(int checkSum = 0; checkSum < rowSum.length; ++checkSum) {
                // if sum of R, G, B values of every pixel in a row
                // equals the corresponding checksum and foundLowerBound is false,
                // this means this row only contains the background
                // i.e. no user interaction/drawing in that row

                // so if rowSum is not equal to corresponding checkSum
                // and foundLowerBound is false
                // that row is where user interaction/drawing starts
                // ie our lower bound
                if (rowSum[checkSum] != checkIndex[checkSum] && !foundLowerBound){
                    rowBounds[1] = row;
                    foundLowerBound = true;
                }
            }

            // reinitialize rowSums to 0
            for (int index = 0; index < rowSum.length; ++index){
                rowSum[index] = 0;
            }
        }

        return rowBounds;
    }


    /**
     * 1. compute the col pixel value sum
     * 2. compare sum[i] with (Mat.cols() * backgroundColorRGB[i])
     * 3. return first cols that doesn't match
     * (Detailed explanations are in function implementation)
     *
     * @param backgroundColorRGB double array with 3 length ( RED, GREEN, BLUE)
     *                          in range (0.0 ,255.0)
     * @return the left and right bounds of drawing to extract background (find the desired area)
     */
    public int[] getMatBoundariesHorizontal(double[] backgroundColorRGB){

        double[] colSum = {0, 0, 0};
        double[] data;

        int[] colBounds = new int[2];
        colBounds[1] = matToBeProcessed.cols() - 1; // left bound _ start with max
        colBounds[0] = 0;                           // right bound _ start with min

        boolean foundLeftBound = false;
        boolean foundRightBound = false;

        // checks for R, G, B values
        double checkIndex0 = matToBeProcessed.cols() * backgroundColorRGB[0];
        double checkIndex1 = matToBeProcessed.cols() * backgroundColorRGB[1];
        double checkIndex2 = matToBeProcessed.cols() * backgroundColorRGB[2];
        double[] checkIndex = {checkIndex0, checkIndex1, checkIndex2};


        for (int col = 0; col < matToBeProcessed.cols() && !foundLeftBound; ++col) {
            for (int row = 0; row < matToBeProcessed.rows(); ++row) {
                data = matToBeProcessed.get(row, col);
                for (int rgb_index = 0; rgb_index < colSum.length; ++rgb_index) {
                    colSum[rgb_index] += data[rgb_index];
                }
            }
            for(int checkSum = 0; checkSum < colSum.length; ++checkSum) {
                // if sum of R, G, B values of every pixel in a col
                // equals the corresponding checksum and foundLeftBound is false,
                // this means this col only contains the background
                // i.e. no user interaction/drawing in that col

                // so if colSum is not equal to corresponding checkSum
                // and foundLeftBound is false
                // that col is where user interaction/drawing starts
                // ie our left bound
                if (colSum[checkSum] != checkIndex[checkSum] && !foundLeftBound){
                    colBounds[0] = col;
                    foundLeftBound = true;
                }
            }

            // reinitialize colSums to 0
            for (int index = 0; index < colSum.length; ++index){
                colSum[index] = 0;
            }
        }


        for (int col = matToBeProcessed.cols() - 1; col >= 0 && !foundRightBound; --col) {
            for (int row = 0; row < matToBeProcessed.rows(); ++row) {
                data = matToBeProcessed.get(row, col);
                for (int rgb_index = 0; rgb_index < colSum.length; ++rgb_index) {
                    colSum[rgb_index] += data[rgb_index];
                }
            }
            for(int checkSum = 0; checkSum < colSum.length; ++checkSum) {
                // if sum of R, G, B values of every pixel in a col
                // equals the corresponding checksum and foundRightBound is false,
                // this means this col only contains the background
                // i.e. no user interaction/drawing in that col

                // so if colSum is not equal to corresponding checkSum
                // and foundRightBound is false
                // that col is where user interaction/drawing starts
                // ie our lower bound
                if (colSum[checkSum] != checkIndex[checkSum] && !foundRightBound){
                    colBounds[1] = col;
                    foundRightBound = true;
                }
            }

            // reinitialize colSums to 0
            for (int index = 0; index < colSum.length; ++index){
                colSum[index] = 0;
            }
        }

        return colBounds;
    }


    /**
     * Given starting and ending coordinates for vertical and horizontal lines
     * Extracts the character as a submat and scales it to the pixel value
     *
     * @param x1 start coordinate of x
     * @param x2 end coordinate of x
     * @param y1 startcoordinate of x
     * @param y2 end coordinate of x
     * @param pixel pixel to calculate scaling for
     * @return extracted character
     */
    public Mat scale(int x1, int x2, int y1, int y2, int pixel){

        // some magic values
        y1 = (y1 >= 1) ? y1 - 1 : 0;
        y2 = (y2 <= matToBeProcessed.rows() - 1) ? y2 + 1 : matToBeProcessed.rows();
        x1 = (x1 >= 1) ? x1 - 1 : 0;
        x2 = (x2 <= matToBeProcessed.cols() - 1) ? x2 + 1 : matToBeProcessed.cols();

        matToBeProcessed = matToBeProcessed.submat(y1, y2, x1, x2);

        Log.i("Preprocessor::", "" + x1);
        Log.i("Preprocessor::", "" + x2);

        Log.i("Preprocessor::", "" + y1);
        Log.i("Preprocessor::", "" + y2);

        // Lengths to find scale to resize
        int xLength = x2 - x1 + 1;
        int yLength = y2 - y1 + 1;
        Log.i("Preprocessor::", "xLen::" + xLength);
        Log.i("Preprocessor::", "yLen::" + yLength);

        double scale = pixel * 1.0 / Math.max(xLength, yLength);
        Log.i("Preprocessor::", "scale::" + scale);

        Size newSize = new Size(matToBeProcessed.width() * scale, matToBeProcessed.height() * scale);
        Mat newMat = new Mat(newSize, CONFIG);

        Imgproc.resize(matToBeProcessed, newMat, newSize);

        matToBeProcessed = newMat;
        return matToBeProcessed;
    }


    /**
     *
     * Given starting and ending coordinates for vertical and horizontal lines
     * Extracts the character as a submat and scales it to the pixel value
     *
     * @param matIn the mat to be scaled to
     * @param x1 starting coordinate of x
     * @param x2 starting coordinate of x
     * @param y1 starting coordinate of x
     * @param y2 starting coordinate of x
     * @param pixel pixel to calculate scaling for
     * @return extracted character
     */
    public Mat scale(Mat matIn, int x1, int x2, int y1, int y2, int pixel){

        // some magic values
        y1 = (y1 >= 1) ? y1 - 1 : 0;
        y2 = (y2 <= matToBeProcessed.rows() - 2) ? y2 + 1 : matToBeProcessed.rows();
        x1 = (x1 >= 1) ? x1 - 1 : 0;
        x2 = (y2 <= matToBeProcessed.cols() - 2) ? x2 + 1 : matToBeProcessed.cols();

        Mat subMat = matIn.submat(y1, y2, x1, x2);

        // Lengths to find scale to resize
        int xLength = x2 - x1 + 1;
        int yLength = y2 - y1 + 1;
        Log.i("Preprocessor::", "xLen::" + xLength);
        Log.i("Preprocessor::", "yLen::" + yLength);

        double scale = pixel * 1.0 / Math.max(xLength, yLength);
        Log.i("Preprocessor::", "scale::" + scale);

        Size newSize = new Size(subMat.width() * scale, subMat.height() * scale);
        Mat newMat = new Mat(newSize, CONFIG);

        Imgproc.resize(subMat, newMat, newSize);

        matToBeProcessed = newMat;
        return matToBeProcessed;
    }


    public Bitmap fillBackground(Mat matIn){

        int yLength = matIn.rows();
        int xLength = matIn.cols();

        int fillTop = (((28 - yLength) / 2.0) % 1 == 0) ? (int)((28 - yLength) / 2.0) : (int)Math.floor((28 - yLength) / 2.0) + 1 ;
        int fillBottom = (((28 - yLength) / 2.0) % 1 == 0) ? (int)((28 - yLength) / 2.0) : (int)Math.floor((28 - yLength) / 2.0);
        int fillLeft = (((28 - xLength) / 2.0) % 1 == 0) ? (int)((28 - xLength) / 2.0) : (int)Math.floor((28 - xLength) / 2.0) + 1;
        int fillRight = (((28 - xLength) / 2.0) % 1 == 0) ? (int)((28 - xLength) / 2.0) : (int)Math.floor((28 - xLength) / 2.0);

        Core.copyMakeBorder(matIn, matIn, fillTop, fillBottom, fillLeft, fillRight, Core.BORDER_CONSTANT, Scalar.all(255));

        if(matIn.cols() != 28){
            Log.i("ERROR:", "copyMakeBorder cols != 28");
        }

        if(matIn.rows() != 28){
            Log.i("ERROR:", "copyMakeBorder rows != 28");
        }

        matToBeProcessed = matIn;
        return matToBitmap(matIn);
    }

    public Bitmap fillBackground(){

        int yLength = matToBeProcessed.rows();
        int xLength = matToBeProcessed.cols();

        int fillTop = (((28 - yLength) / 2.0) % 1 == 0) ? (int)((28 - yLength) / 2.0) : (int)Math.floor((28 - yLength) / 2.0) + 1 ;
        int fillBottom = (((28 - yLength) / 2.0) % 1 == 0) ? (int)((28 - yLength) / 2.0) : (int)Math.floor((28 - yLength) / 2.0);
        int fillLeft = (((28 - xLength) / 2.0) % 1 == 0) ? (int)((28 - xLength) / 2.0) : (int)Math.floor((28 - xLength) / 2.0) + 1;
        int fillRight = (((28 - xLength) / 2.0) % 1 == 0) ? (int)((28 - xLength) / 2.0) : (int)Math.floor((28 - xLength) / 2.0);

        Log.i("fillBackground::", "xLength::" + xLength);
        Log.i("fillBackground::", "yLength::" + yLength);
        Log.i("fillBackground::", "fillTop::" + fillTop);
        Log.i("fillBackground::", "fillBottom::" + fillBottom);
        Log.i("fillBackground::", "fillLeft::" + fillLeft);
        Log.i("fillBackground::", "fillRight::" + fillRight);


        Core.copyMakeBorder(matToBeProcessed, matToBeProcessed, fillTop, fillBottom, fillLeft, fillRight, Core.BORDER_CONSTANT, Scalar.all(255));

        if(matToBeProcessed.cols() != 28){
            Log.i("ERROR:", "copyMakeBorder cols != 28");
        }

        if(matToBeProcessed.rows() != 28){
            Log.i("ERROR:", "copyMakeBorder rows != 28");
        }

        return matToBitmap(matToBeProcessed);
    }

}
