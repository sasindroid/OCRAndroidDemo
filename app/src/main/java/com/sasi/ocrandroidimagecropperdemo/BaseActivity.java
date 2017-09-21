package com.sasi.ocrandroidimagecropperdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.google.android.gms.samples.vision.ocrreader.OcrCaptureActivity;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
    }

    public void ocrRealtimeDetection(View view) {
        startActivity(new Intent(this, OcrCaptureActivity.class));
    }

    public void ocrUsingImage(View view) {
        startActivity(new Intent(this, MainActivity1.class));
    }
}
