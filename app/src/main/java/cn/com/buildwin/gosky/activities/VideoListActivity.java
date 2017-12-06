package cn.com.buildwin.gosky.activities;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import buildwin.common.Utilities;
import butterknife.BindColor;
import butterknife.BindString;
import cn.com.buildwin.gosky.R;

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
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.list_item_video, null);
                }

                // Set background color
                convertView.setBackgroundColor(
                        mCheckedArray.get(position) ? listViewCheckedColor : listViewDefaultColor
                );

                String videoFilePath = (String)this.getItem(position);
                String videoFileName = new File(videoFilePath).getName();

                TextView textView = (TextView)convertView
                        .findViewById(R.id.list_item_video_file_path_textView);
                // Set File Name
                textView.setText(videoFileName);

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
