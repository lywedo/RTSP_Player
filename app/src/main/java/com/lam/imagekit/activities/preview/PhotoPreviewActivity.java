package com.lam.imagekit.activities.preview;

import android.os.Bundle;

import com.lam.imagekit.activities.PhotoListActivity;


/**
 * Created by Lam on 2017/9/27.
 */

public class PhotoPreviewActivity extends BasePhotoPreviewActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init(getIntent().getExtras());
    }
    protected void init(Bundle extras) {
        if (extras == null)
            return;

            photos =  extras.getStringArrayList(PhotoListActivity.LIST_ALL);
            current = extras.getInt(PhotoListActivity.LIST_POSITION, 0);
            updatePercent();
            bindData();

    }
}
