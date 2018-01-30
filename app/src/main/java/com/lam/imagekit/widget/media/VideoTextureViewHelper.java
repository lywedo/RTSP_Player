package com.lam.imagekit.widget.media;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;

import static com.lam.imagekit.widget.media.IRenderView.AR_ASPECT_FILL_PARENT;
import static com.lam.imagekit.widget.media.IRenderView.AR_ASPECT_FIT_PARENT;

/**
 * Created by sknown on 2018/1/30.
 */

public class VideoTextureViewHelper {
    private float DEFAULT_SCALE = 1.0f;
    /** 最小缩放比例*/
    private float MIN_SCALE = DEFAULT_SCALE;
    /** 最大缩放比例*/
    static final float MAX_SCALE = 5f;

    float x_down = 0;
    float y_down = 0;
    Context context;

    PointF prev = new PointF();
    PointF mid = new PointF();

    float oldDist = 1f;
    float oldRotation = 0;
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();

    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    int mode = NONE;

    private int m_videoWidth;
    private int m_videoHeight;

    private TextureView m_textureView;
    public VideoTextureViewHelper(TextureView textureView){
        m_textureView = textureView;
    }

    public void updateTextureViewSizeCenterCrop(){
        Matrix matrix = getOrgMatrix();
        Matrix matrixOrg = new Matrix();
        matrixOrg.set(matrix);
        rotate(matrix, m_rotate);
        m_textureView.setTransform(matrix);
        m_textureView.postInvalidate();
    }

    private int m_currentAspectRatio = AR_ASPECT_FIT_PARENT;
    public void setAspectRatio(int aspectRatio) {
        m_currentAspectRatio = aspectRatio;
        updateTextureViewSizeCenterCrop();
    }
    private Matrix getOrgMatrix(){
        int width = m_textureView.getWidth();
        int height = m_textureView.getHeight();

        int videoWidth = m_videoWidth;
        int videoHeight = m_videoHeight;

        float sx = (float) width / (float) videoWidth;
        float sy = (float) height / (float) videoHeight;
        float maxScale = Math.min(sx, sy);

        if(width<height){
            sx = (float) width / (float) videoHeight;
            sy = (float) height / (float) videoWidth;
            if(m_currentAspectRatio == AR_ASPECT_FIT_PARENT){
                maxScale = sx;
            }else if(m_currentAspectRatio == AR_ASPECT_FILL_PARENT){
                maxScale = sy;
            }
        }else{
            if(m_currentAspectRatio == AR_ASPECT_FIT_PARENT){
                maxScale = sy;
            }else if(m_currentAspectRatio == AR_ASPECT_FILL_PARENT){
                maxScale = sx;
            }
        }

        Matrix matrix = new Matrix();

        //第1步:把视频区移动到View区,使两者中心点重合.
        matrix.preTranslate((width - videoWidth) / 2, (height - videoHeight) / 2);

        //第2步:因为默认视频是fitXY的形式显示的,所以首先要缩放还原回来.
        matrix.preScale(videoWidth / (float) width, videoHeight / (float) height);

        //第3步,等比例放大或缩小,直到视频区的一边超过View一边, 另一边与View的另一边相等. 因为超过的部分超出了View的范围,所以是不会显示的,相当于裁剪了.
        matrix.postScale(maxScale, maxScale, width / 2, height / 2);//后两个参数坐标是以整个View的坐标系以参考的

        float p[] = new float[9];
        matrix.getValues(p);
        m_orgScaleX = p[Matrix.MSCALE_X];
        m_orgScaleY = p[Matrix.MSCALE_Y];
        return matrix;
    }
    private float m_orgScaleX;
    private float m_orgScaleY;

    public void setVideoSize(int videoWidth, int videoHeight) {
        m_videoWidth = videoWidth;
        m_videoHeight = videoHeight;
        updateTextureViewSizeCenterCrop();

    }

    private float getScale(Matrix matrix){
        Matrix matrix1 = new Matrix();
        matrix1.set(matrix);
        matrix1.postRotate(-m_rotate_status, m_textureView.getWidth(), m_textureView.getHeight());
        float p[] = new float[9];
        matrix1.getValues(p);
        float sx = p[Matrix.MSCALE_X];
        float sy = p[Matrix.MSCALE_Y];

        return sx/m_orgScaleX;
    }

    private void checkScale(Matrix matrix, PointF mid){
        if (getScale(matrix)< MIN_SCALE) {
            matrix.set(getOrgMatrix());
            rotate(matrix, m_rotate);
        } else if (getScale(matrix) > MAX_SCALE) {
            float scale = MAX_SCALE/getScale(matrix);
            matrix.postScale(scale, scale, mid.x, mid.y);
        }
    }
    protected void checkEdge(Matrix matrix, boolean horizontal, boolean vertical) {
        Matrix m = new Matrix();
        m.set(matrix);
        RectF rect = new RectF(0, 0, m_textureView.getWidth(), m_textureView.getHeight());
        m.mapRect(rect);

        float deltaX = 0, deltaY = 0;

        int screenWidth = (int)m_textureView.getWidth();//pview.getHeight();
        int screenHeight = (int)m_textureView.getHeight();//pview.getWidth();

        if (vertical) {
            if (rect.height() < screenHeight) {
                deltaY = (screenHeight - rect.height()) / 2 - rect.top;
            } else if(rect.bottom<screenHeight){
                deltaY = screenHeight-rect.bottom;
            }else if(rect.top>0){
                deltaY = -rect.top;
            }

        }

        if (horizontal) {
            if(rect.width()<screenWidth) {
                deltaX = (screenWidth - rect.width()) / 2 - rect.left;
            }else if(rect.right<screenWidth){
                deltaX = screenWidth-rect.right;
            }else if(rect.left>0){
                deltaX=-rect.left;
            }
        }
        Log.d("delta",deltaX+"++++++++"+deltaY);
        matrix.postTranslate(deltaX, deltaY);
    }

    private View.OnTouchListener m_touchListener;

    public void setOnTouchListener(View.OnTouchListener l) {
        m_touchListener = l;
    }

    private class ActionDownHelper implements Runnable{
        private MotionEvent m_event;
        private Handler m_handler = new Handler();
        public ActionDownHelper(){

        }
        private int touch;
        private float touch_x;
        private float touch_y;
        public void setAction(MotionEvent event){
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    touch = 1;
                    touch_x = event.getX();
                    touch_y = event.getY();
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    touch = 0;
                    break;
                case MotionEvent.ACTION_MOVE: {
                    float tx = event.getX()-touch_x;
                    float ty = event.getY() - touch_y;
                    if(tx*tx+ty*ty>280){
                        touch = 0;
                    }
                    break;
                }
                case MotionEvent.ACTION_UP:
                    if(touch == 1){
                        m_event = event;
                        run();
                    }
                    touch = 0;
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    break;
                default:
                    break;
            }
        }

        public void postDelayed(MotionEvent event, int ms){
            m_event = event;
            m_handler.postDelayed(this, ms);
        }

        public void remove(){
            m_handler.removeCallbacks(this);
        }

        @Override
        public void run() {
            if(m_touchListener != null) {
                m_touchListener.onTouch(m_textureView, m_event);
            }
        }
    }

    private boolean m_scaleEnable;
    public void setScaleEnable(boolean enable){
        m_scaleEnable = enable;
    }
    private ActionDownHelper m_actonDownHelper = new ActionDownHelper();
    public boolean onTouchEvent(MotionEvent event) {
        if(!m_scaleEnable){
            return false;
        }

        m_actonDownHelper.setAction(event);
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mode = DRAG;

                x_down = event.getX();
                y_down = event.getY();
                prev.set(x_down, y_down);
                m_textureView.getTransform(savedMatrix);
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                mode = ZOOM;

                oldDist = spacing(event);
                oldRotation = rotation(event);
                midPoint(mid, event);

                m_textureView.getTransform(savedMatrix);

                return true;
            case MotionEvent.ACTION_MOVE:
                Matrix matrix1 = new Matrix();

                if (mode == ZOOM) {
                    matrix1.set(savedMatrix);

                    float newDist = spacing(event);
                    float scale = newDist / oldDist;

                    matrix1.postScale(scale, scale, mid.x, mid.y);// 縮放
                    checkScale(matrix1, mid);

                    checkEdge(matrix1,true, true);
                    //matrix.set(matrix1);
                    m_textureView.setTransform(matrix1);
                    m_textureView.postInvalidate();

                } else if (mode == DRAG) {
                    matrix1.set(savedMatrix);
                    float dx = event.getX() - x_down;
                    float dy = event.getY() - y_down;

                    matrix1.postTranslate(dx, dy);// 平移

                    checkEdge(matrix1,true, true);
                    m_textureView.setTransform(matrix1);
                    m_textureView.postInvalidate();
                }

                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
            default:
                break;
        }

        return false;
    }

    // 触碰两点间距离
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    // 取手势中心点
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    // 取旋转角度
    private float rotation(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    private int m_rotate = 0;
    private int m_rotate_status = 0;
    private void rotate(Matrix matrix, int rotate){
        matrix.postRotate(rotate, m_textureView.getWidth()/2, m_textureView.getHeight()/2);
        checkEdge(matrix,true, true);
    }

    public void rotate(int rotate){

        Matrix matrix = new Matrix();

        m_textureView.getTransform(matrix);
        m_rotate = rotate;

        m_rotate_status %= 360;
        m_rotate %= 360;

        int tmp = m_rotate-m_rotate_status;
        if(tmp<0){
            tmp = 360+tmp;
        }
        rotate(matrix, tmp);
        m_rotate_status+= tmp;
        m_rotate_status %= 360;

        m_textureView.setTransform(matrix);
        m_textureView.postInvalidate();
    }

    private void scale(float scaleX, float scaleY){

        Matrix matrix = new Matrix();
        m_textureView.getTransform(matrix);

        PointF middlePoint = new PointF();
        middlePoint.set(m_textureView.getWidth()/2, m_textureView.getHeight()/2);
        matrix.postScale(scaleX, scaleY, middlePoint.x, middlePoint.y);
        checkScale(matrix, middlePoint);
        m_textureView.setTransform(matrix);
    }

    public void scaleUpView(){

        scale(1.2f, 1.2f);
    }

    public void scaleDownView(){
        scale(0.8f, 0.8f);
    }

}
