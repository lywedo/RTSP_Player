package com.lam.imagekit.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
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


public class PhotoListActivity extends MediaListActivity {

    // ListView color
    @BindColor(R.color.list_view_default_color)
    int listViewDefaultColor;
    @BindColor(R.color.list_view_checked_color)
    int listViewCheckedColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * 载入媒体文件列表
     * @return  媒体文件列表
     */
    @Override
    protected List<String> reloadMediaList() {
        return Utilities.loadPhotoList();
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
                String imageFilePath = (String)this.getItem(position);
                if (convertView == null) {
                    holder = new ViewHolder();
                    convertView = getLayoutInflater().inflate(R.layout.list_item_photo, null);
                    holder.tv = convertView.findViewById(R.id.list_item_photo_file_path_textView);
                    holder.iv = convertView.findViewById(R.id.list_item_photo_preview_imageView);
                    convertView.setTag(holder);
                }else {
                    holder = (ViewHolder) convertView.getTag();
                }

                // Set background color
                convertView.setBackgroundColor(
                        mCheckedArray.get(position) ? listViewCheckedColor : listViewDefaultColor
                );

                ImageView imageView = (ImageView)convertView
                        .findViewById(R.id.list_item_photo_preview_imageView);
                Glide.with(PhotoListActivity.this).load(imageFilePath).into(holder.iv);
                holder.tv.setText(new File(imageFilePath).getName());

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
        // TODO: Show photo
        String photoFilePath = mMediaList.get(index);
        // Start the photo intent
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + photoFilePath), "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

}
