package com.sasi.ocrandroidimagecropperdemo;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.FileNotFoundException;

public class MainActivity1 extends AppCompatActivity {

    private static final String TAG = "MainActivity1";

    private TextView tvScannedData;
    private ImageView ivScannedCropped;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main1);

        tvScannedData = (TextView) findViewById(R.id.tvScannedData);
        ivScannedCropped = (ImageView) findViewById(R.id.ivScannedCropped);
    }

    public void scanNow(View view) {
        // start picker to get image for cropping and then use the image in cropping activity
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                Log.d(TAG, "Success - Uri: " + resultUri);

                initiateOCR(resultUri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Log.d(TAG, "Error: " + error.getMessage());
            }

        }
    }

    private void initiateOCR(Uri resultUri) {

        // Reset the text area.
        tvScannedData.setText("");

        launchMediaScanIntent(resultUri);

        Bitmap bitmap = null;

        try {
            bitmap = decodeBitmapUri(resultUri);

            Glide.with(this).load(resultUri).into(ivScannedCropped);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (bitmap == null) {
            Toast.makeText(this, "Unable to get a valid image.", Toast.LENGTH_SHORT).show();
            tvScannedData.setText("Unable to get a valid image (bitmap is null)");
            return;
        }

        TextRecognizer textRecognizer = new TextRecognizer.Builder(this).build();

        if (!textRecognizer.isOperational()) {
            // Note: The first time that an app using a Vision API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any text,
            // barcodes, or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(TAG, "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
                tvScannedData.setText("textRecognizer is NOT Operational: " + getString(R.string.low_storage_error));
            }
            else {
                tvScannedData.setText("textRecognizer is NOT Operational: UNKNOWN reason");
            }

            return;
        }

        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<TextBlock> textBlocks = textRecognizer.detect(frame);

        String blocks = "";
        String lines = "";
        String words = "";

        Log.d(TAG, "*textBlocks.size: " + textBlocks.size());

        String printStr = "";


        for (int index = 0; index < textBlocks.size(); index++) {
            //extract scanned text blocks here
            TextBlock tBlock = textBlocks.valueAt(index);
            blocks = blocks + tBlock.getValue() + "\n" + "\n";

            printStr = printStr + "\n**block: " + tBlock.getValue();
            Log.d(TAG, "**block: " + tBlock.getValue());

            for (Text line : tBlock.getComponents()) {
                //extract scanned text lines here
                lines = lines + line.getValue() + "\n";

                printStr = printStr + "\n***line: " + line.getValue();
                Log.d(TAG, "***line: " + line.getValue());

                for (Text element : line.getComponents()) {
                    //extract scanned text words here
                    printStr = printStr + "\n****word: " + element.getValue();
                    Log.d(TAG, "****word: " + element.getValue());
                    words = words + element.getValue() + ", ";
                }
            }
        }

//        Log.d(TAG, "printStr: " + printStr);

        if (textBlocks.size() == 0) {
            tvScannedData.setText("Scan Failed: Found nothing to scan");
        } else {
            tvScannedData.setText(printStr);
        }
    }

    private void printTextToTextView(String s, boolean isNextLine) {

        tvScannedData.setText(tvScannedData.getText() + (isNextLine ? "\n" : "") + s);
        Log.d(TAG, "printTextToTextView: " + s + " -------- " + tvScannedData.getText());
    }

    private void launchMediaScanIntent(Uri imageUri) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(imageUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private Bitmap decodeBitmapUri(Uri uri) throws FileNotFoundException {
        int targetW = 600;
        int targetH = 600;
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(this.getContentResolver().openInputStream(uri), null, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeStream(this.getContentResolver().openInputStream(uri), null, bmOptions);
    }
}
