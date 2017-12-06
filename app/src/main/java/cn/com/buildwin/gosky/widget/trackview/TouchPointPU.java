package cn.com.buildwin.gosky.widget.trackview;

import java.util.ArrayList;

public class TouchPointPU {

    private ArrayList<TrackPoint> mTrackPoints;
    private TrackPoint mCurrentPoint;

    // 单位长度
    private float perUnitLength;

    public void setPerUnitLength(float perUnitLength) {
        this.perUnitLength = perUnitLength;
    }

    // 事件监听
    private OnProcessPointEventListener mOnProcessPointEventListener;

    public void setOnProcessPointEventListener(OnProcessPointEventListener onProcessPointEventListener) {
        mOnProcessPointEventListener = onProcessPointEventListener;
    }

    /**
     * 处理触摸点
     * @param touchPoints    触摸点
     */
    public void processTouchPoints(ArrayList<TouchPoint> touchPoints) {
        // 清除当前点
        mCurrentPoint = null;

        // 准备TrackPoints
        mTrackPoints = new ArrayList<>();
        for (TouchPoint touchPoint: touchPoints) {
            mTrackPoints.add(new TrackPoint(touchPoint));
        }

        // 通知
        onBeginProcessingPoint();
    }

    /**
     * 返回下一点
     * @return  下一个TrackPoint
     */
    public TrackPoint dequeueTrackPoint() {
        // 如果剩下的点数量为0，则返回null
        if (mTrackPoints.size() == 0)
            return null;

        // 如果currentPoint不为空，表示已经处理第一点
        if (mCurrentPoint != null) {
            TrackPoint p1;
            TrackPoint p2 = mCurrentPoint;

            float p1x;
            float p1y;
            float p2x;
            float p2y;

            float pul = perUnitLength;
            float ppLen = 0;

            do {
                p1 = p2;
                p2 = mTrackPoints.get(0);

                if (mTrackPoints.contains(p1))
                    mTrackPoints.remove(p1);

                p1x = p1.x;
                p1y = p1.y;
                p2x = p2.x;
                p2y = p2.y;

                // 计算点点路径长度
                ppLen += Math.sqrt(Math.pow(p2x - p1x, 2) + Math.pow(p2y - p1y, 2));
            } while (ppLen < pul && mTrackPoints.size() != 0);

            // 计算单位坐标（角度计算）
            double ur = Math.atan2(p2.y - mCurrentPoint.y, p2.x - mCurrentPoint.x);
            double ux = 1.0 * Math.cos(ur);
            double uy = 1.0 * Math.sin(ur);
            mCurrentPoint.ux = (float)ux;
            mCurrentPoint.uy = -(float)uy;  // 显示坐标与单位坐标相反

            // 计算新的当前点的显示坐标
            double r = Math.atan2(p1y - p2y, p1x - p2x);
            double deltaX = (ppLen - pul) * Math.cos(r);
            double deltaY = (ppLen - pul) * Math.sin(r);
            double newX = p2x + deltaX;
            double newY = p2y + deltaY;

            mCurrentPoint.x = (float)newX;
            mCurrentPoint.y = (float)newY;
        }
        // 处理第一点
        else {
            mCurrentPoint = mTrackPoints.get(0);
            mTrackPoints.remove(0);

            mCurrentPoint.ux = 0;
            mCurrentPoint.uy = 0;
        }

        return mCurrentPoint;
    }

    /**
     * 触摸点处理事件接口
     */
    public interface OnProcessPointEventListener {
        void beginProcessingPoint();
    }

    private void onBeginProcessingPoint() {
        if (mOnProcessPointEventListener != null) {
            mOnProcessPointEventListener.beginProcessingPoint();
        }
    }
}
