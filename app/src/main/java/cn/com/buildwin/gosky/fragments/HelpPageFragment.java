package cn.com.buildwin.gosky.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import cn.com.buildwin.gosky.R;


public class HelpPageFragment extends Fragment {

    public static final String ARG_PAGE = "page";

    // TODO: 这里添加/删除帮助页面(图像)
    private static int[] pages = new int[] {
            R.mipmap.help_page_0,
            R.mipmap.help_page_1,
            R.mipmap.help_page_2,
            R.mipmap.help_page_3,
            R.mipmap.help_page_4,
    };

    /**
     * 获取页面数
     * @return  页面数
     */
    public static int getPageNumbers() {
        return pages.length;
    }

    // 当前页码
    private int mPageNumber;

    /**
     * 创建一个HelpPageFragment
     * @param pageNumber    页码
     * @return              HelpPageFragment实例
     */
    public static HelpPageFragment create(int pageNumber) {
        HelpPageFragment fragment = new HelpPageFragment();
        // 设置参数
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, pageNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 获取传入的页码
        mPageNumber = getArguments().getInt(ARG_PAGE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FrameLayout rootView = new FrameLayout(getActivity());

        // 加载图像
        int pageId = pages[mPageNumber];        // 对应页码的图像
        ImageView imageView = new ImageView(getActivity());     // 创建
        imageView.setImageResource(pageId);     // 载入图像
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);     // 拉伸
        rootView.addView(imageView);            // 添加图像到视图

        return rootView;
    }

    /**
     * 获取页码
     * @return  页码
     */
    public int getPageNumber() {
        return mPageNumber;
    }

}
