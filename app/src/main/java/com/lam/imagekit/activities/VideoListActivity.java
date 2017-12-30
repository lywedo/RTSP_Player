package com.lam.imagekit.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.lam.imagekit.R;
import com.lam.imagekit.utils.Utilities;

import java.io.File;
import java.util.List;


import butterknife.BindColor;


public class VideoListActivity extends MediaListActivity {

    // ListView color
    @BindColor(R.color.list_view_default_color)
    int listViewDefaultColor;
    @BindColor(R.color.list_view_checked_color)
    int listViewCheckedColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMediaListView.setEnabled(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMediaListView.setEnabled(false);
    }

    /**
     * 载入媒体文件列表
     * @return  媒体文件列表
     */
    @Override
    protected List<String> reloadMediaList() {
        return Utilities.loadVideoList();
    }

    /**
     * 获取ListAdapter
     * @return  ListAdapter
     */
    @Override
    protected ListAdapter getListAdapter() {
        class ViewHolder{
            TextView tv;
            ImageView iv;
        }

        return new BaseAdapter() {
            @Override
            public int getCount() {
                return mMediaList.size();
            }

            @Override
            public Object getItem(int position) {
                return mMediaList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder holder;
                String videoFilePath = (String)this.getItem(position);
                String videoFileName = new File(videoFilePath).getName();
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.list_item_video, null);
                    holder = new ViewHolder();
                    holder.tv = convertView.findViewById(R.id.list_item_video_file_path_textView);
                    holder.iv = convertView.findViewById(R.id.list_item_video_file_path_imageView);
                    convertView.setTag(holder);
                }else {
                    holder = (ViewHolder) convertView.getTag();
                }

                // Set background color
                convertView.setBackgroundColor(
                        mCheckedArray.get(position) ? listViewCheckedColor : listViewDefaultColor
                );

                holder.tv.setText(videoFileName);
                Log.d("image",videoFilePath);
                Glide.with(VideoListActivity.this).load(Utilities.getThumbnailsPath()+"/"+Utilities.patchThumbName(videoFileName)).into(holder.iv);

                return convertView;
            }
        };
    }

    /**
     * 展示媒体文件
     * @param index 媒体文件索引
     */
    @Override
    protected void displayMediaAtIndex(int index) {
        // Show video
        String videoFilePath = mMediaList.get(index);

        String strPath = videoFilePath;
        String strFileName = strPath.substring(strPath.lastIndexOf("/") + 1);

        VideoActivity.intentTo(this, videoFilePath, strFileName);
    }

}
