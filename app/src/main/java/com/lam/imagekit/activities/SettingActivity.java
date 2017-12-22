package com.lam.imagekit.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextPaint;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.lam.imagekit.AppContext;
import com.lam.imagekit.BuildConfig;
import com.lam.imagekit.R;
import com.lam.imagekit.data.CameraParam;
import com.lam.imagekit.data.StreamParam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;


public class SettingActivity extends AppCompatActivity {
    // Show version
    private TextView mVersionTextView;

    // Headers
    private String[] headers;

    private ImageButton backButton;
    private ListView mListView;

    private boolean bAutosave;
    private int m_picformat;
    private ArrayList<String> m_resolutionList = new ArrayList<>();
    private SettingAdapter m_settingAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_setting);

        // Version TextView
        mVersionTextView = (TextView)findViewById(R.id.setting_version_textView);
        // Show version
        int versionCode = BuildConfig.VERSION_CODE;
        String versionName = BuildConfig.VERSION_NAME;
        mVersionTextView.setText("Ver " + versionName + " (Build " + versionCode + ")");

        // Header & Title
        headers = new String[] {
                "分辨率",
        };

//        // Back Button
        backButton = (ImageButton)findViewById(R.id.setting_backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
//
//        // ListView
        mListView = (ListView)findViewById(R.id.setting_listView);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO: List item 点击事件
            }
        });
        m_settingAdapter = new SettingAdapter();
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                int width = 0;
                int height = 0;
                SettingAdapter adapter = (SettingAdapter) adapterView.getAdapter();
                String resu = (String) adapter.getItem(i);
                String []wh = resu.split("x");
                width = Integer.valueOf(wh[0]);
                height = Integer.valueOf(wh[1]);
                AppContext.getInstance().getBroadCtrl().setuvc(width, height);
                finish();
            }
        });
        mListView.setAdapter(m_settingAdapter);

    }

    @Override
    protected void onStart() {
        super.onStart();

        m_resolutionList.clear();
        genResolutionList();
        if(m_resolutionList.size() == 0){
            m_resolutionList.add("请插入摄像头");
        }

        m_settingAdapter.updateSection(0, m_resolutionList);
        m_settingAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    /**
     * ListAdapter
     */
    private class SettingAdapter extends BaseAdapter {

        private static final int TYPE_LIST_ITEM = 0;
        private static final int TYPE_LIST_SECTION = 1;

        private ArrayList<ArrayList<String>> m_sectionList = new ArrayList<>();

        public SettingAdapter() {
            // Add header and title to itemString
            for (int i = 0; i < headers.length; i++) {
                ArrayList<String> section = new ArrayList<>();
                section.add(headers[i]);
                m_sectionList.add(section);
            }
            updateCount();
        }

        public void updateSection(int sectionIndex, ArrayList<String> stringArrayList){
            ArrayList<String> sectionHead = m_sectionList.get(sectionIndex);
            String title = sectionHead.get(0);
            sectionHead.clear();
            sectionHead.add(title);
            sectionHead.addAll(stringArrayList);

            updateCount();
        }

        private int m_count;
        private int updateCount(){
            synchronized (this) {
                m_count = 0;
                for (ArrayList<String> section : m_sectionList) {
                    for (String string : section) {
                        m_count++;
                    }
                }
            }

            return m_count;
        }

        @Override
        public int getItemViewType(int position) {
            int index = 0;
            for(ArrayList<String> section:m_sectionList){
                for(String string: section){
                    if(index == position){
                        if(section.indexOf(string) == 0){
                            return TYPE_LIST_SECTION;
                        }else{
                            return TYPE_LIST_ITEM;
                        }
                    }
                    index++;
                }
            }
            return TYPE_LIST_ITEM;
        }

        @Override
        public int getCount() {
            return m_count;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItemViewType(position) == TYPE_LIST_ITEM;
        }

        private IndexPath getPath(int position){
            int index = 0;
            int row = 0;
            int sec = 0;
            for(ArrayList<String> section:m_sectionList){
                for(String string: section){
                    if(index == position){
                        IndexPath indexPath = new IndexPath();
                        indexPath.postion = position;
                        indexPath.section = sec;
                        indexPath.row = row;
                        return indexPath;
                    }
                    index++;
                    row++;
                }
                sec++;
                row = 0;
            }

            return null;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            int rowType = getItemViewType(position);

            // 根据类型返回Section/Row
            switch (rowType) {
                // Section header
                case TYPE_LIST_SECTION:
                    convertView = getLayoutInflater().inflate(R.layout.list_item_section, null);

                    TextView headerTextView = (TextView)convertView
                            .findViewById(R.id.list_item_section_header_textView);
                    headerTextView.setText((String)getItem(position));
                    TextPaint tp = headerTextView.getPaint();
                    tp.setFakeBoldText(true);
                    break;
                // Row
                case TYPE_LIST_ITEM:
                    convertView = getLayoutInflater().inflate(R.layout.list_item_switch, null);

                    TextView titleTextView = (TextView)convertView
                            .findViewById(R.id.list_item_switch_title_textView);
                    titleTextView.setText((String)getItem(position));
                    // Row switch
                    ImageButton imageButton = (ImageButton)convertView
                            .findViewById(R.id.list_item_switch_switch_imageButton);
//
                    IndexPath indexPath = getPath(position);
                    int section = indexPath.section;
                    int row = indexPath.row;

                    // Set position as Tag
                    imageButton.setTag(position);

                    // TODO: Set the row
                    // Section 0
                    if (section == 0) {
                        // Row 0
                        if (position == AppContext.getInstance().getResolutionIndex()) {
                            imageButton.setImageResource(R.mipmap.select_index);
                        }
                        // Row except above
                        else {
                            imageButton.setVisibility(View.INVISIBLE);
                        }
                    }
                    // Section 1
                    else if (section == 1) {
                        // Row 0
                        if (row == 0) {
                            if (rightHandMode) {
                                imageButton.setImageResource(R.mipmap.switch_enable);
                            } else {
                                imageButton.setImageResource(R.mipmap.switch_disable);
                            }
                        }
                        // Row except above
                        else {
                            imageButton.setVisibility(View.INVISIBLE);
                        }
                    }
                    // Section 2
                    else if (section == 2) {
                        imageButton.setVisibility(View.INVISIBLE);
                    }
                    break;
            }

            return convertView;
        }
        boolean rightHandMode = true;
        boolean autoSave = false;
        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            int index = 0;
            for(ArrayList<String> section:m_sectionList){
                for(String string: section){
                    if(index == position){
                        return string;
                    }
                    index++;
                }
            }

            return null;
        }
    }

    private class IndexPath{
        int postion;
        int section;
        int row;
    }

    private void genResolutionList(){
        if(m_resolutionList.size() == 0){
            CameraParam cparam = AppContext.getInstance().getCameraParam();
            int sourceIndex = CameraParam.SOURCE_USB;
            CameraParam.Source source = cparam.getSource(sourceIndex);
            if(source.size() == 0){
                source = cparam.getSource(CameraParam.SOURCE_VI);
                sourceIndex = CameraParam.SOURCE_VI;
            }

            int i=0;
            int picformat = CameraParam.FORMAT_MJPEG;
            m_picformat = picformat;
            CameraParam.StreamParamList sparamList = cparam.getFormat(sourceIndex, picformat);
            if (sparamList != null){
                for(StreamParam streamParam : sparamList){
                    m_resolutionList.add("" + streamParam.width + "x" + streamParam.height);
                }
            }
        }
    }
}