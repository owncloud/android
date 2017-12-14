package com.owncloud.android.ui.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.VideoView;

import com.owncloud.android.R;


public class CapturedFileChecker extends AppCompatActivity {

    private static final int CANCEL_CODE = 100;
    private static final int UPLOAD_CODE = 101;
    private static int code;

    private ImageView capturedImage;
    private Button cancelButton;
    private Button uploadCapturedFileButton;

    private static Bitmap capturedFile;
    private static Uri capturedFileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captured_file_checker);
        capturedImage = (ImageView) findViewById(R.id.imageCaptured);
        cancelButton = (Button) findViewById(R.id.cancelCapturedUpload);
        uploadCapturedFileButton = (Button) findViewById(R.id.uploadCapturedFile);
        final Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        code = extras.getInt("mediaTypeCode");
        if(code == 1){
            capturedImage.setVisibility(View.VISIBLE);
            capturedFile = (Bitmap) extras.get("capturedImage");
            capturedImage.setImageBitmap(capturedFile);
        }
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = setIntentMediaType();
                setResult(CANCEL_CODE,intent);
                finish();
            }
        });
        uploadCapturedFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = setIntentMediaType();
                setResult(UPLOAD_CODE,intent);
                finish();
            }
        });
    }

    public static Intent setIntentMediaType(){
        Intent intent = new Intent();
        if(code == 1){
            intent.putExtra("mediaTypeCode",1);
            intent.putExtra("capturedImage",capturedFile);

        }
        return intent;
    }
}
