package com.lam.imagekit.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.lam.imagekit.BaseActivity;
import com.lam.imagekit.R;

/**
 * Created by Lam on 2017/11/23.
 */

public class ReviewActivity extends BaseActivity {
    ImageButton mPhoto;
    ImageButton mVideo;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_review);
        initViews();
    }

    private void initViews() {
        mPhoto = findViewById(R.id.review_photo_button);
        mPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ReviewActivity.this, PhotoListActivity.class));
            }
        });
        mVideo = findViewById(R.id.review_video_button);
        mVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ReviewActivity.this, VideoListActivity.class));
            }
        });
    }
}
