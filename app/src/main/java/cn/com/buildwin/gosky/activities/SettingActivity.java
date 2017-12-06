package cn.com.buildwin.gosky.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;

import cn.com.buildwin.gosky.BuildConfig;
import cn.com.buildwin.gosky.R;
import cn.com.buildwin.gosky.application.Settings;

public class SettingActivity extends AppCompatActivity implements View.OnClickListener {

    // Show version
    private TextView mVersionTextView;

    // Headers
    private String[] headers;
    // Titles
    private String[][] titles;

    private ImageButton backButton;
    private ListView mListView;

    private boolean bAutosave;

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
                getResources().getString(R.string.setting_first_header_title),
                getResources().getString(R.string.setting_second_header_title),
//                getResources().getString(R.string.setting_third_header_title),  // uncomment when necessary
        };
        titles = new String[][] {
                new String[] {
                        getResources().getString(R.string.setting_first_section_first_row_title),
                        getResources().getString(R.string.setting_first_section_second_row_title),
                },
                new String[] {
                        getResources().getString(R.string.setting_second_section_first_row_title),
                },
//                new String[] {
//                        getResources().getString(R.string.setting_third_section_first_row_title),    // uncomment when necessary
//                }
        };

        // Back Button
        backButton = (ImageButton)findViewById(R.id.setting_backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // ListView
        mListView = (ListView)findViewById(R.id.setting_listView);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO: List item 点击事件
                IndexPath indexPath = new IndexPath(position);
                int section = indexPath.getSection();
                int row = indexPath.getRow();
                // Section 0
                if (section == 0) {
                    // Row 1
                    if (row == 1) {
                        // Reset parameters
                        Settings.getInstance(SettingActivity.this).resetSettings();
                        Toast.makeText(SettingActivity.this,
                                R.string.setting_alert_reset_parameter_success, Toast.LENGTH_SHORT).show();
                    }
                }
                // Section 2
                else if (section == 2) {
                    // Row 0
                    if (row == 0) {
                        Intent i = new Intent(SettingActivity.this, RenameSSIDActivity.class);
                        startActivity(i);
                    }
                }
            }
        });
        SettingAdapter adapter = new SettingAdapter();
        mListView.setAdapter(adapter);

        bAutosave = Settings.getInstance(this).getParameterForAutosave();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (!bAutosave) {
            Settings.getInstance(this).resetSettings();
        }
    }

    /**
     * ListAdapter
     */
    private class SettingAdapter extends BaseAdapter {

        private static final int TYPE_LIST_ITEM = 0;
        private static final int TYPE_LIST_SECTION = 1;

        private ArrayList<String> itemString = new ArrayList<>();
        private TreeSet<Integer> sectionHeader = new TreeSet<>();

        public SettingAdapter() {
            // Add header and title to itemString
            for (int i = 0; i < headers.length; i++) {
                itemString.add(headers[i]);
                sectionHeader.add(itemString.size() - 1);
                itemString.addAll(Arrays.asList(titles[i]));
            }
        }

        @Override
        public int getItemViewType(int position) {
            return sectionHeader.contains(position) ? TYPE_LIST_SECTION : TYPE_LIST_ITEM;
        }

        @Override
        public int getCount() {
            return itemString.size();
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItemViewType(position) == TYPE_LIST_ITEM;
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
                    headerTextView.setText(itemString.get(position));
                    TextPaint tp = headerTextView.getPaint();
                    tp.setFakeBoldText(true);
                    break;
                // Row
                case TYPE_LIST_ITEM:
                    convertView = getLayoutInflater().inflate(R.layout.list_item_switch, null);

                    // 读取配置信息
                    boolean autoSave =
                            Settings.getInstance(SettingActivity.this).getParameterForAutosave();
                    boolean rightHandMode =
                            Settings.getInstance(SettingActivity.this).getParameterForRightHandMode();

                    // Row title
                    TextView titleTextView = (TextView)convertView
                            .findViewById(R.id.list_item_switch_title_textView);
                    titleTextView.setText(itemString.get(position));
                    // Row switch
                    ImageButton imageButton = (ImageButton)convertView
                            .findViewById(R.id.list_item_switch_switch_imageButton);

                    IndexPath indexPath = new IndexPath(position);
                    int section = indexPath.getSection();
                    int row = indexPath.getRow();

                    // Set position as Tag
                    imageButton.setTag(position);

                    // TODO: Set the row
                    // Section 0
                    if (section == 0) {
                        // Row 0
                        if (row == 0) {
                            if (autoSave) {
                                imageButton.setImageResource(R.mipmap.switch_enable);
                            } else {
                                imageButton.setImageResource(R.mipmap.switch_disable);
                            }
                            imageButton.setOnClickListener(SettingActivity.this);
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
                            imageButton.setOnClickListener(SettingActivity.this);
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

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            return itemString.get(position);
        }
    }

    /**
     * Button click listener
     */
    @Override
    public void onClick(View v) {
        boolean autoSave = Settings.getInstance(this).getParameterForAutosave();
        boolean rightHandMode = Settings.getInstance(this).getParameterForRightHandMode();

        int tag = (int)v.getTag();
        IndexPath indexPath = new IndexPath(tag);
        int section = indexPath.getSection();
        int row = indexPath.getRow();

        // TODO: Button action
        // Section 0
        if (section == 0) {
            // Row 0
            if (row == 0) {
                // TODO: Auto save
                autoSave = !autoSave;
                if (autoSave) {
                    ((ImageButton)v).setImageResource(R.mipmap.switch_enable);
                } else {
                    ((ImageButton)v).setImageResource(R.mipmap.switch_disable);
                }
                // Save setting
                Settings.getInstance(this).saveParameterForAutosave(autoSave);

                // Current setting
                bAutosave = autoSave;
            }

        }
        // Section 1
        else if (section == 1) {
            // Row 0
            if (row == 0) {
                // TODO: Right hand mode
                rightHandMode = !rightHandMode;
                if (rightHandMode) {
                    ((ImageButton)v).setImageResource(R.mipmap.switch_enable);
                } else {
                    ((ImageButton)v).setImageResource(R.mipmap.switch_disable);
                }
                // Save setting
                Settings.getInstance(this).saveParameterForRightHandMode(rightHandMode);
            }
        }
    }

    /**
     * IndexPath Class
     * Use with headers & titles (Array)
     */
    private class IndexPath {

        private int mPosition;
        public int mSection;
        public int mRow;

        private void convert(int position) {
            int curr = 0;
            int next = 0;
            int sec = 0;
            do {
                curr = next;
                next += (1 + titles[sec].length);
                if (mPosition > curr && mPosition < next) {
                    mSection = sec;
                    mRow = position - curr - 1;
                    break;
                }
                sec++;
            } while (sec != titles.length);
        }

        public IndexPath(int position) {
            mPosition = position;

            mSection = 0;
            mRow = 0;
            convert(mPosition);
        }

        public int getSection() {
            return mSection;
        }

        public int getRow() {
            return mRow;
        }
    }

}
