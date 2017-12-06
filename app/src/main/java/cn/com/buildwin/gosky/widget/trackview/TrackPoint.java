package cn.com.buildwin.gosky.widget.trackview;

public class TrackPoint {

    public float x;
    public float y;

    public float ux;
    public float uy;

    public TrackPoint(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public TrackPoint(TouchPoint touchPoint) {
        this.x = touchPoint.x;
        this.y = touchPoint.y;
    }
}
