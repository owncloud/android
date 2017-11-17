package com.ortiz.touch;

import com.example.touch.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;


public class MainActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.single_touchimageview_button).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, SingleTouchImageViewActivity.class));
			}
		});
        findViewById(R.id.viewpager_example_button).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, ViewPagerExampleActivity.class));
			}
		});
        findViewById(R.id.mirror_touchimageview_button).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, MirroringExampleActivity.class));
			}
		});
        findViewById(R.id.switch_image_button).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, SwitchImageExampleActivity.class));
			}
		});
        findViewById(R.id.switch_scaletype_button).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, SwitchScaleTypeExampleActivity.class));
			}
		});
    }
}