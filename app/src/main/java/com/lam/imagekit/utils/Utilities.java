package com.lam.imagekit.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.lam.imagekit.AppContext;
import com.lam.imagekit.BuildConfig;
import com.lam.imagekit.activities.ReviewActivity;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Utilities {

    // 主目录名
    private static  String HOME_PATH_NAME = "imagekit";
    // 照片和视频的子目录名
    private static final String PHOTO_PATH_NAME = "Image";
    public static final String VIDEO_PATH_NAME = "Movie";
    private static final String CARD_MEDIA_PATH_NAME = "CardMedia";
    private static final String CARD_MEDIA_IMAGE_PATH_NAME = "Image";
    private static final String CARD_MEDIA_VIDEO_PATH_NAME = "Video";
    public static final String THUMBLENAIL_PATH_NAME = "Thumbnail";
    // 照片和视频的扩展名
    private static final String PHOTO_FILE_EXTENSION_PNG = "png";
    private static final String PHOTO_FILE_EXTENSION_JPG = "jpg";
    private static final String VIDEO_FILE_EXTENSION = "avi";

    static {
        String[] packageName = AppContext.getInstance().getPackageName().split("\\.");
        HOME_PATH_NAME = packageName[packageName.length - 1];
//        HOME_PATH_NAME = BuildConfig.FLAVOR;
    }

    /**
     * 获取应用数据主目录
     * @return  主目录路径
     */
    static public String getHomePath() {
        String homePath = null;

        try {
            String extStoragePath = Environment.getExternalStorageDirectory().getCanonicalPath();
            File homeFile = new File(extStoragePath, HOME_PATH_NAME);
            homePath = homeFile.getCanonicalPath();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return homePath;
    }

    /**
     * 获取父目录下子目录
     */
    static public String getSubDir(String parent, String dir) {
        if (parent == null)
            return null;

        String subDirPath = null;

        try {
            // 获取展开的子目录路径
            File subDirFile = new File(parent, dir);

            if (!subDirFile.exists())
                subDirFile.mkdirs();

            subDirPath = subDirFile.getCanonicalPath();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return subDirPath;
    }

    /**
     * 获取主目录下照片目录
     * @return  照片目录路径
     */
    static public String getPhotoPath() {
        return getSubDir(getHomePath(), PHOTO_PATH_NAME);
    }

    /**
     * 获取主目录下视频目录
     * @return  视频目录路径
     */
    static public String getVideoPath() {
        return getSubDir(getHomePath(), VIDEO_PATH_NAME);
    }

    static public String getThumbnailsPath(){
        return getSubDir(getHomePath(), THUMBLENAIL_PATH_NAME);
    }

    /**
     * 获取主目录下卡媒体目录
     * @return
     */
    static private String getCardMediaPath() {
        return getSubDir(getHomePath(), CARD_MEDIA_PATH_NAME);
    }

    /**
     * 获取卡媒体目录中视频目录
     * @return
     */
    static public String getCardMediaVideoPath() {
        return getSubDir(getCardMediaPath(), CARD_MEDIA_VIDEO_PATH_NAME);
    }

    /**
     * 获取卡媒体目录下图像目录
     * @return
     */
    static public String getCardMediaImagePath() {
        return getSubDir(getCardMediaPath(), CARD_MEDIA_IMAGE_PATH_NAME);
    }

    /**
     * 载入照片文件路径列表
     * @return  照片文件路径列表
     */
    static public List<String> loadPhotoList() {
        String photoPathName = getPhotoPath();
        File photoPath = new File(photoPathName);

        // 使用过滤器过滤照片文件列表
        File[] photoFiles = photoPath.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                // 根据扩展名过滤照片文件
                try {
                    String filePath = file.getCanonicalPath();
                    String extension = filePath.substring(filePath.lastIndexOf(".") + 1);
                    if (extension.equalsIgnoreCase(PHOTO_FILE_EXTENSION_PNG) || extension.equalsIgnoreCase(PHOTO_FILE_EXTENSION_JPG)) {
                        return true;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });

        // 使用List装载照片文件路径
        List<String> photoFileNameList = null;
        if (photoFiles != null) {
            photoFileNameList = new ArrayList<>();
            for (File file : photoFiles) {
                // 逐个添加路径到列表
                photoFileNameList.add(file.getPath());
            }

            // 翻转顺序,使最新文件在前
            Collections.reverse(photoFileNameList);
        }

        return photoFileNameList;
    }

    /**
     * 载入视频文件路径列表
     * @return  视频文件路径列表
     */
    static public List<String> loadVideoList() {
        File videoPath = new File(getVideoPath());

        // 使用过滤器过滤视频文件列表
        File[] videoFiles = videoPath.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                // 根据扩展名过滤视频文件
                try {
                    String filePath = file.getCanonicalPath();
                    String extension = filePath.substring(filePath.lastIndexOf(".") + 1);
                    if (extension.equalsIgnoreCase(VIDEO_FILE_EXTENSION)) {
                        return true;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });

        // 使用List装载视频文件列表
        List<String> videoFileNameList = null;
        if (videoFiles != null) {
            videoFileNameList = new ArrayList<>();
            for (File file : videoFiles) {
                // 逐个添加路径到列表
                videoFileNameList.add(file.getPath());
            }

            // 翻转顺序,使最新文件在前
            Collections.reverse(videoFileNameList);
        }

        return videoFileNameList;
    }

    /**
     * 获取图片目录路径
     * @return  图片目录路径
     */
    static public String getPhotoDirPath() {
        String photoPath = getPhotoPath();
        if (photoPath == null)
            return null;

        // 如果文件夹不存在, 则创建
        File photoDir = new File(photoPath);
        if (!photoDir.exists()) {
            // 创建失败则返回null
            if (!photoDir.mkdirs()) return null;
        }

        return photoDir.getAbsolutePath();
    }

    /**
     * 获取视频目录路径
     * @return  视频目录路径
     */
    static public String getVideoDirPath() {
        String videoPath = getVideoPath();
        if (videoPath == null)
            return null;

        // 如果文件夹不存在, 则创建
        File videoDir = new File(videoPath);
        if (!videoDir.exists()) {
            // 创建失败则返回null
            if (!videoDir.mkdirs()) return null;
        }

        return videoDir.getAbsolutePath();
    }

    /**
     * 获取媒体文件名称
     * @return  媒体文件名称
     */
    static public String getMediaFileName() {
        // 由日期创建文件名
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmsss", Locale.getDefault());
        String dateString = format.format(date);
//        String photoFileName = dateString + "." + PHOTO_FILE_EXTENSION;
        String photoFileName = dateString;

        return photoFileName;
    }

    /**
     * Add border to bitmap
     * @param bmp           位图
     * @param color         颜色
     * @param borderSize    线宽
     * @return              Bitmap with border
     */
    static public Bitmap addBorderToBitmap(Bitmap bmp, int color, int borderSize) {
        Bitmap bmpWithBorder = Bitmap.createBitmap(
                bmp.getWidth() + borderSize * 2,
                bmp.getHeight() + borderSize * 2,
                bmp.getConfig()
        );
        Canvas canvas = new Canvas(bmpWithBorder);
        // Set paint
        Paint paint = new Paint();
        paint.setColor(color);
        // Draw top
        canvas.drawLine(0, 0, bmp.getWidth() + borderSize * 2, borderSize * 2, paint);
        // Draw bottom
        canvas.drawLine(0, bmp.getHeight() + borderSize, bmp.getWidth() + borderSize * 2, bmp.getHeight() + borderSize * 2, paint);
        // Draw left
        canvas.drawLine(0, 0, borderSize, bmp.getHeight() + borderSize * 2, paint);
        // Draw right
        canvas.drawLine(bmp.getWidth() + borderSize, 0, bmp.getWidth() + borderSize * 2, bmp.getHeight() + borderSize * 2, paint);
        // Draw Original Image
        canvas.drawBitmap(bmp, borderSize, borderSize, null);
        return bmpWithBorder;
    }

    /**
     * 删除文件
     * @param filePath  文件路径
     * @return  是否成功删除（路径为空或者文件不存在均返回true）
     */
    static public boolean deleteFile(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return true;
        }

        File file  = new File(filePath);

        return !file.exists() || file.delete();
    }

    /**
     * 获取存储目录（卡）的可用空间
     */
    static public long getFreeDiskSpace() {
        long size = 0;

        String homePath = getHomePath();
        if (homePath != null) {
            File homeDir = new File(homePath);
            size = homeDir.getFreeSpace();
        }

        return size;
    }

    /**
     * 格式化Size
     */
    public static String memoryFormatter(long size) {
        double bytes = 1.0 * size;
        double kilobytes = bytes / 1024;
        double megabytes = bytes / (1024 * 1024);
        double gigabytes = bytes / (1024 * 1024 * 1024);

        if (gigabytes >= 1.0)
            return String.format(Locale.getDefault(), "%1.2f GB", gigabytes);
        else if (megabytes >= 1.0)
            return String.format(Locale.getDefault(), "%1.2f MB", megabytes);
        else if (kilobytes >= 1.0)
            return String.format(Locale.getDefault(), "%1.2f KB", kilobytes);
        else
            return String.format(Locale.getDefault(), "%1.0f bytes", bytes);
    }

    public static String patchThumbName(String name){
        File file = new File(getThumbnailsPath());
        if (!file.exists()){
            return "";
        }
        File[] fillAll = file.listFiles();
        for (int i = 0; i < fillAll.length; i++) {
            if (fillAll[i].getName().split("\\.")[0] .equals(name.split("\\.")[0])){
                return fillAll[i].getName();
            }
        }
        return "";
    }

    /**
     * 将dip或dp值转换为px值，保证尺寸大小不变
     *
     * @param dipValue
     * @param scale
     *            （DisplayMetrics类中属性density）
     * @return
     */
    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }
}
