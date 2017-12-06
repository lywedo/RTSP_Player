package cn.com.buildwin.gosky.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import cn.com.buildwin.gosky.R;

public class ReviewActivity extends AppCompatActivity {

    private ImageButton mBackButton;
    private ImageButton mPhotoButton;
    private ImageButton mVideoButton;
    private ImageButton mCardButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_review);

        /**
         * Back Button
         */
        mBackButton = (ImageButton)findViewById(R.id.review_back_button);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                // Activity slide from left
                overridePendingTransition(
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right
                );
            }
        });

        /**
         * Photo Button
         */
        mPhotoButton = (ImageButton)findViewById(R.id.review_photo_button);
        mPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ReviewActivity.this, PhotoListActivity.class);
                startActivity(i);
                // Activity slide from left
                overridePendingTransition(
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right
                );
            }
        });

        /**
         * Video Button
         */
        mVideoButton = (ImageButton)findViewById(R.id.review_video_button);
        mVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ReviewActivity.this, VideoListActivity.class);
                startActivity(i);
                // Activity slide from left
                overridePendingTransition(
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right
                );
            }
        });

        /**
         * Card Media Button
         */
        mCardButton = (ImageButton)findViewById(R.id.review_card_media_button);
        mCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ReviewActivity.this, CardMediaListActivity.class);
                startActivity(intent);
                // Activity slide from left
                overridePendingTransition(
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right
                );
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        // Activity slide from left
        overridePendingTransition(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
        );
    }
}
