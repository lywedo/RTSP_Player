package com.lam.imagekit.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.lam.imagekit.R;
import com.lam.imagekit.utils.Utilities;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


import butterknife.BindColor;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;


public abstract class MediaListActivity extends AppCompatActivity {

    @BindView(R.id.media_back_button)
    ImageButton mBackButton;
//    @BindView(R.id.media_cancel_button)
//    Button mCancelButton;
    @BindView(R.id.media_delete_button)
    Button mDeleteButton;
    @BindView(R.id.media_list_listView)
    ListView mMediaListView;
    @BindView(R.id.media_select_button)
    ImageButton mMore;
    @BindView(R.id.ll_bar_control)
    LinearLayout mBar;
    @BindView(R.id.media_select_cancel)
    ImageButton mMediaCancel;
    @BindView(R.id.media_select_all)
    ImageButton mSelectAll;
    @BindView(R.id.media_select_delect)
    ImageButton mDelect;

    // Delete button
    @BindColor(R.color.delete_button_normal_color)
    int deleteButtonNormalColor;
    @BindColor(R.color.delete_button_highlight_color)
    int deleteButtonHighlightColor;
    @BindString(R.string.media_list_delete_button)
    String deleteButtonString;
    @BindString(R.string.media_list_delete_button_highlight_format)
    String deleteButtonHighlightFormat;

    private LinearLayout parentLinearLayout;

    protected List<String> mMediaList;

    private boolean mCheckedMode;
    protected SparseBooleanArray mCheckedArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        initContentView(R.layout.activity_media_list);
        ButterKnife.bind(this);

        // Load media list
        mMediaList = new ArrayList<>();

        /**
         * Media ListView, set adapter
         */
        mMediaListView.setAdapter(getListAdapter());

        // Init checkedArray
        mCheckedArray = new SparseBooleanArray();
    }

    /**
     * 布局模板使用
     *
     * @param layoutResId 布局资源ID
     */
    private void initContentView(int layoutResId) {
        ViewGroup viewGroup = (ViewGroup) findViewById(android.R.id.content);
        viewGroup.removeAllViews();
        parentLinearLayout = new LinearLayout(this);
        parentLinearLayout.setOrientation(LinearLayout.VERTICAL);
        viewGroup.addView(parentLinearLayout);
        LayoutInflater.from(this).inflate(layoutResId, parentLinearLayout, true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Load media list
        mMediaList = reloadMediaList();
        if (mMediaList == null) {
            Toast.makeText(this, R.string.media_list_empty_list, Toast.LENGTH_LONG).show();
            mMediaList = new ArrayList<>();
        }
        ((BaseAdapter) mMediaListView.getAdapter()).notifyDataSetChanged();
        // Listview layout animation
        mMediaListView.startLayoutAnimation();
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

    /**
     * 以下三个作为布局模板时使用
     */
    @Override
    public void setContentView(int layoutResId) {
        LayoutInflater.from(this).inflate(layoutResId, parentLinearLayout, true);
    }

    @Override
    public void setContentView(View view) {
        parentLinearLayout.addView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        parentLinearLayout.addView(view, params);
    }

    // 防止短时间多次点击
    private static final int MIN_CLICK_DELAY_TIME = 500;
    private long lastClickTime = 0;

    /**
     * ListView onItemClick
     */
    @OnItemClick(R.id.media_list_listView)
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (mCheckedMode) {
            mCheckedArray = mMediaListView.getCheckedItemPositions();
            mMediaListView.invalidateViews();

            updateDeleteButton();
        } else {
            long currentTime = Calendar.getInstance().getTimeInMillis();
            if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {
                lastClickTime = currentTime;
                // Display media
                displayMediaAtIndex(i);
            }
        }
    }

    /**
     * Buttons OnClick
     */
    @OnClick({R.id.media_back_button,  R.id.media_delete_button, R.id.media_select_button,
            R.id.media_select_cancel, R.id.media_select_all, R.id.media_select_delect})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.media_back_button: {
                if (mCheckedMode) {
                    // Reset listView's checkedItems
                    mMediaListView.clearChoices();
                    mCheckedArray = mMediaListView.getCheckedItemPositions();
                    mMediaListView.invalidateViews();

                    mMediaListView.setChoiceMode(ListView.CHOICE_MODE_NONE);
                    mCheckedMode = false;

//                    mCancelButton.setVisibility(View.INVISIBLE);
                    mBackButton.setVisibility(View.VISIBLE);

                    updateDeleteButton();
                }else {
                    finish();
                    // Activity slide from left
                    overridePendingTransition(
                            android.R.anim.slide_in_left,
                            android.R.anim.slide_out_right
                    );
                }
                break;
            }
            case R.id.media_select_button:
                mCheckedMode = true;
                mBar.setVisibility(View.VISIBLE);
                mMore.setVisibility(View.GONE);
                mCheckedMode = true;
                mMediaListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

                mBackButton.setVisibility(View.INVISIBLE);
//                mCancelButton.setVisibility(View.VISIBLE);

                updateDeleteButton();
                break;
            case R.id.media_select_cancel:
                mCheckedMode = false;
                mBar.setVisibility(View.GONE);
                mMore.setVisibility(View.VISIBLE);
                // Reset listView's checkedItems
                mMediaListView.clearChoices();
                mCheckedArray = mMediaListView.getCheckedItemPositions();
                mMediaListView.invalidateViews();

                mMediaListView.setChoiceMode(ListView.CHOICE_MODE_NONE);
//                mCancelButton.setVisibility(View.INVISIBLE);
                mBackButton.setVisibility(View.VISIBLE);

                updateDeleteButton();
                break;
            case R.id.media_select_all:
                if (mMediaListView.getCheckedItemCount()!=mMediaListView.getCount()){
                    for (int i = 0; i < mMediaListView.getCount(); i++) {
                        mMediaListView.setItemChecked(i, true);
                    }
                }else {
                    mMediaListView.clearChoices();
                }
                mCheckedArray = mMediaListView.getCheckedItemPositions();
                mMediaListView.invalidateViews();
                updateDeleteButton();
                break;
            case R.id.media_select_delect:
                if (mCheckedMode) {
                    if (mMediaListView.getCheckedItemCount() > 0) {
                        // Delete files
                        for (int i = 0; i < mMediaListView.getCount(); i++) {
                            if (mCheckedArray.get(i)) {
                                String filePath = mMediaList.get(i);
                                Utilities.deleteFile(filePath);
                            }
                        }

                        // Reset listView's checkedItems
                        mMediaListView.clearChoices();
                        mCheckedArray = mMediaListView.getCheckedItemPositions();
                        mMediaListView.invalidateViews();
                        // Reload list
                        mMediaList = reloadMediaList();
                        ((BaseAdapter) mMediaListView.getAdapter()).notifyDataSetChanged();

                        updateDeleteButton();
                    }
                } else {
                    mCheckedMode = true;
                    mMediaListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

                    mBackButton.setVisibility(View.INVISIBLE);
//                    mCancelButton.setVisibility(View.VISIBLE);

                    updateDeleteButton();
                }
                break;


        }
    }

    /**
     * 更新删除按钮的显示
     */
    private void updateDeleteButton() {
        if (mCheckedMode) {
            mDeleteButton.setVisibility(View.VISIBLE);
            int checkedItemCount = mMediaListView.getCheckedItemCount();
            String deleteButtonText =
                    String.format(Locale.getDefault(), deleteButtonHighlightFormat, checkedItemCount);
            mDeleteButton.setText(deleteButtonText);
            mDeleteButton.setTextColor(Color.WHITE);
        } else {
            mDeleteButton.setVisibility(View.GONE);
            mDeleteButton.setText(deleteButtonString);
            mDeleteButton.setTextColor(deleteButtonNormalColor);
        }
    }

    // Abstract method

    /**
     * 载入媒体文件列表
     *
     * @return 媒体文件列表
     */
    protected abstract List<String> reloadMediaList();

    /**
     * 获取ListAdapter
     *
     * @return ListAdapter
     */
    protected abstract ListAdapter getListAdapter();

    /**
     * 展示媒体文件
     *
     * @param index 媒体文件索引
     */
    protected abstract void displayMediaAtIndex(int index);
}
