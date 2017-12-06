package cn.com.buildwin.gosky.activities;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import cn.com.buildwin.gosky.R;
import cn.com.buildwin.gosky.fragments.HelpPageFragment;

// Note: 修改帮助页面图像请到HelpPageFragment.java

public class HelpActivity extends FragmentActivity {

    private ViewPager mViewPager;
    private PagerAdapter mPagerAdapter;
    
    private ImageButton backButton;
    private ImageButton prevPageButton;
    private ImageButton nextPageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_help);

        // Instantiate a ViewPager and a PagerAdapter
        mViewPager = (ViewPager)findViewById(R.id.help_viewPager);
        mPagerAdapter = new HelpPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {
                // 更新按键状态
                updateButtonState();
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        // Back Button
        backButton = (ImageButton)findViewById(R.id.help_backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Previous Page Button
        prevPageButton = (ImageButton)findViewById(R.id.help_prev_pageButton);
        prevPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
            }
        });

        // Next Page Button
        nextPageButton = (ImageButton)findViewById(R.id.help_next_pageButton);
        nextPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
            }
        });
    }

    /**
     * 更新按键状态
     */
    private void updateButtonState() {
        HelpPagerAdapter adapter = (HelpPagerAdapter)mViewPager.getAdapter();
        HelpPageFragment fragment = (HelpPageFragment)adapter
                .instantiateItem(mViewPager, mViewPager.getCurrentItem());

        int pageNumber = fragment.getPageNumber();              // 当前页码
        int pageNumbers = HelpPageFragment.getPageNumbers();    // 总页数

        if (pageNumber == 0) {
            prevPageButton.setVisibility(View.INVISIBLE);
        } else {
            prevPageButton.setVisibility(View.VISIBLE);
        }
        if (pageNumber == pageNumbers - 1) {
            nextPageButton.setVisibility(View.INVISIBLE);
        } else {
            nextPageButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 帮助页面的PagerAdapter
     */
    private class HelpPagerAdapter extends FragmentStatePagerAdapter {
        public HelpPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return HelpPageFragment.create(position);   // 创建新页面
        }

        @Override
        public int getCount() {
            return HelpPageFragment.getPageNumbers();   // 总页数
        }
    }

}
