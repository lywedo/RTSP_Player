package com.lam.imagekit.activities;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.TextPaint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.ftr.utils.FTRCallback;
import com.lam.imagekit.AppContext;
import com.lam.imagekit.BuildConfig;
import com.lam.imagekit.R;
import com.lam.imagekit.data.CameraParam;
import com.lam.imagekit.data.StreamParam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class SettingActivity extends AppCompatActivity {
    private final int RESOLUTION_SECTION_INDEX = 0;
    private static boolean m_noCamera = true;

    // Show version
    private TextView mVersionTextView;

    // Headers
    private String[] headers;

    private ImageButton backButton;
    private ListView mListView;

    private boolean bAutosave;
    private int m_picformat;
    private int m_resolutionCurrentIndex;


    private ArrayList<String> m_resolutionList = new ArrayList<>();
    private SettingAdapter m_settingAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(option);
            getWindow().setStatusBarColor(getResources().getColor(R.color.main_color));
        }
        setContentView(R.layout.activity_setting);

        // Version TextView
        mVersionTextView = (TextView)findViewById(R.id.setting_version_textView);
        // Show version
        String versionName = BuildConfig.VERSION_NAME;
        mVersionTextView.setText("v" + versionName );

        // Header & Title
        headers = new String[] {
                getString(R.string.resolution),
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
        m_settingAdapter = new SettingAdapter();
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (!m_noCamera){
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
            }
        });
        mListView.setAdapter(m_settingAdapter);

    }

    @Override
    protected void onStart() {
        super.onStart();
        AppContext.getInstance().setScanResultCallback(new FTRCallback() {
            @Override
            public Object process(Object object, int what, int param1, int parma2) {

                m_handler.post(new Runnable() {
                    @Override
                    public void run() {
                        refreshList(false);
                        mListView.requestLayout();
                        AppContext.getInstance().setScanResultCallback(null);
                    }
                });
                //refreshList(false);
                //AppContext.getInstance().setScanResultCallback(null);
                return null;
            }
        });

        refreshList(true);
        refreshResolution();
    }

    @Override
    protected void onStop() {
        super.onStop();
        m_handler.removeCallbacks(m_refreshRunnable);
        AppContext.getInstance().setScanResultCallback(null);
    }

    private void refreshList(boolean isforce){
        if(isforce) {
            m_resolutionList.clear();
            m_resolutionCurrentIndex = genResolutionList(m_resolutionList);
        }else{
            ArrayList<String> list = new ArrayList<>();
            int currentIndex = genResolutionList(list);
            if(m_resolutionList.size() != list.size() || currentIndex != m_resolutionCurrentIndex){
                m_resolutionList.clear();
                m_resolutionList.addAll(list);
                m_resolutionCurrentIndex = currentIndex;
            }else{
                int i = 0;
                int count = m_resolutionList.size();
                boolean diff = false;
                for(i=0; i<count; i++){
                    if(!m_resolutionList.get(i).equals(list.get(i))){
                        m_resolutionList.clear();
                        m_resolutionList.addAll(list);
                        m_resolutionCurrentIndex = currentIndex;
                        diff = true;
                        break;
                    }
                }
                if(!diff){
                    return;
                }
            }
        }

        if(m_resolutionList.size() == 0){
            m_noCamera = true;
            m_resolutionList.add(getString(R.string.please_camera));
        }else{
            m_noCamera = false;
        }

        m_settingAdapter.updateSection(0, m_resolutionList);
        m_settingAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private Handler m_handler = new Handler();
    private Runnable m_refreshRunnable = new Runnable() {
        @Override
        public void run() {
            AppContext.getInstance().getBroadCtrl().getuvc();
            m_handler.postDelayed(m_refreshRunnable, 3000);
        }
    };

    private void refreshResolution(){
        m_handler.post(m_refreshRunnable);
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

                    // Section 0
                    if (section == RESOLUTION_SECTION_INDEX) {//res
                        // Row 0
                        if (row == m_resolutionCurrentIndex+1 && !m_noCamera) {//1 for head
                            imageButton.setImageResource(R.mipmap.select_index);
                        }
                        // Row except above
                        else {
                            imageButton.setVisibility(View.INVISIBLE);
                        }
                    }
//                    // Section 1
//                    else if (section == 1) {
//                        // Row 0
//                        if (row == 0) {
//                            if (rightHandMode) {
//                                imageButton.setImageResource(R.mipmap.switch_enable);
//                            } else {
//                                imageButton.setImageResource(R.mipmap.switch_disable);
//                            }
//                        }
//                        // Row except above
//                        else {
//                            imageButton.setVisibility(View.INVISIBLE);
//                        }
//                    }
//                    // Section 2
//                    else if (section == 2) {
//                        imageButton.setVisibility(View.INVISIBLE);
//                    }
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
    Comparator comp = new Comparator() {
        public int compare(Object o1, Object o2) {
            StreamParam p1 = (StreamParam) o1;
            StreamParam p2 = (StreamParam) o2;

            int s1 = p1.width*p1.height;
            int s2 = p2.width*p2.height;

            if (s1 < s2)
                return -1;
            else if (s1 == s2)
                return 0;
            else if (s1 > s2)
                return 1;
            return 0;
        }
    };
    private int genResolutionList(ArrayList<String> resolutionList){
        int currentIndex = 0;
        if(resolutionList.size() == 0){
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
            ArrayList<StreamParam> sortStreamList = new ArrayList<>();
            sortStreamList.addAll(sparamList);
            Collections.sort(sortStreamList, comp);

            if (sortStreamList != null){
                for(StreamParam streamParam : sortStreamList){
                    resolutionList.add("" + streamParam.width + "x" + streamParam.height);
                    if(streamParam.width == cparam.curWidth && streamParam.height == cparam.curHeight){
                        currentIndex = resolutionList.size()-1;
                    }
                }
            }
        }

        return currentIndex;
    }
}
