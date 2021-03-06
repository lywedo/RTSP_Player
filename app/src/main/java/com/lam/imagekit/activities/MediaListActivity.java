package com.lam.imagekit.activities;

import android.graphics.Color;
import android.os.Build;
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
import android.widget.TextView;
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
import butterknife.OnItemLongClick;


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
    TextView mMore;
    @BindView(R.id.ll_bar_control)
    LinearLayout mBar;
    @BindView(R.id.media_select_cancel)
    TextView mMediaCancel;
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
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(option);
            getWindow().setStatusBarColor(getResources().getColor(R.color.title_color));
        }

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
     * ??????????????????
     *
     * @param layoutResId ????????????ID
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
     * ???????????????????????????????????????
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

    // ???????????????????????????
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
    @OnItemLongClick(R.id.media_list_listView)
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l){
        mCheckedMode = true;
        updateDeleteButton();
        mMediaListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mMediaListView.setSelection(i);
        mMediaListView.clearChoices();
        mCheckedArray = mMediaListView.getCheckedItemPositions();
        mMediaListView.invalidateViews();
        mBar.setVisibility(View.VISIBLE);
        mMore.setVisibility(View.GONE);

        mBackButton.setVisibility(View.INVISIBLE);
        // Reload list
        mMediaList = reloadMediaList();
        return false;
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
                                if (filePath.split("[.]")[1].equals("avi")){
                                    String pathRoot = filePath.split("Movie")[0];
                                    String fileName = filePath.split(Utilities.VIDEO_PATH_NAME+"/")[1];
                                    Utilities.deleteFile(pathRoot + Utilities.THUMBLENAIL_PATH_NAME+ "/" + fileName.split("[.]")[0] + ".png");
                                }
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
     * ???????????????????????????
     */
    private void updateDeleteButton() {
        if (mCheckedMode) {
            mDeleteButton.setVisibility(View.VISIBLE);
            int checkedItemCount = mMediaListView.getCheckedItemCount();
            String deleteButtonText =
                    String.format(Locale.getDefault(), deleteButtonHighlightFormat, checkedItemCount, mMediaListView.getCount());
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
     * ????????????????????????
     *
     * @return ??????????????????
     */
    protected abstract List<String> reloadMediaList();

    /**
     * ??????ListAdapter
     *
     * @return ListAdapter
     */
    protected abstract ListAdapter getListAdapter();

    /**
     * ??????????????????
     *
     * @param index ??????????????????
     */
    protected abstract void displayMediaAtIndex(int index);
}
