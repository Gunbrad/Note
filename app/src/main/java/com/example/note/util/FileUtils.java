package com.example.note.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 文件工具类
 * 处理图片文件的保存、删除和管理
 */
public class FileUtils {
    
    private static final String TAG = "FileUtils";
    private static final String IMAGES_DIR = "images";
    private static final int MAX_IMAGE_SIZE = 1920; // 最大图片尺寸
    private static final int JPEG_QUALITY = 85; // JPEG压缩质量
    
    /**
     * 获取图片存储目录
     */
    public static File getImagesDir(Context context) {
        File imagesDir = new File(context.getFilesDir(), IMAGES_DIR);
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
        }
        return imagesDir;
    }
    
    /**
     * 根据图片ID获取图片文件
     */
    public static File getImageFile(Context context, String imageId) {
        return new File(getImagesDir(context), imageId + ".jpg");
    }
    
    /**
     * 保存图片到本地存储
     * @param context 上下文
     * @param bitmap 要保存的图片
     * @param imageId 图片ID
     * @return 是否保存成功
     */
    public static boolean saveImage(Context context, Bitmap bitmap, String imageId) {
        try {
            File imageFile = getImageFile(context, imageId);
            
            // 压缩图片
            Bitmap compressedBitmap = compressBitmap(bitmap);
            
            // 保存到文件
            FileOutputStream fos = new FileOutputStream(imageFile);
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos);
            fos.close();
            
            // 如果压缩后的图片不是原图片，则回收压缩后的图片
            if (compressedBitmap != bitmap) {
                compressedBitmap.recycle();
            }
            
            Log.d(TAG, "Image saved: " + imageFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to save image: " + imageId, e);
            return false;
        }
    }
    
    /**
     * 从URI保存图片
     */
    public static boolean saveImageFromUri(Context context, Uri uri, String imageId) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            if (bitmap != null) {
                boolean result = saveImage(context, bitmap, imageId);
                bitmap.recycle();
                return result;
            }
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Failed to save image from URI: " + uri, e);
            return false;
        }
    }
    
    /**
     * 加载图片
     */
    public static Bitmap loadImage(Context context, String imageId) {
        try {
            File imageFile = getImageFile(context, imageId);
            if (imageFile.exists()) {
                return BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load image: " + imageId, e);
        }
        return null;
    }
    
    /**
     * 删除图片文件
     */
    public static boolean deleteImage(Context context, String imageId) {
        try {
            File imageFile = getImageFile(context, imageId);
            if (imageFile.exists()) {
                boolean deleted = imageFile.delete();
                Log.d(TAG, "Image deleted: " + imageFile.getAbsolutePath() + ", success: " + deleted);
                return deleted;
            }
            return true; // 文件不存在也算删除成功
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete image: " + imageId, e);
            return false;
        }
    }
    
    /**
     * 检查图片文件是否存在
     */
    public static boolean imageExists(Context context, String imageId) {
        File imageFile = getImageFile(context, imageId);
        return imageFile.exists();
    }
    
    /**
     * 获取图片文件大小（字节）
     */
    public static long getImageSize(Context context, String imageId) {
        File imageFile = getImageFile(context, imageId);
        return imageFile.exists() ? imageFile.length() : 0;
    }
    
    /**
     * 压缩图片
     */
    private static Bitmap compressBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        // 如果图片尺寸已经合适，直接返回
        if (width <= MAX_IMAGE_SIZE && height <= MAX_IMAGE_SIZE) {
            return bitmap;
        }
        
        // 计算缩放比例
        float scale = Math.min((float) MAX_IMAGE_SIZE / width, (float) MAX_IMAGE_SIZE / height);
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);
        
        // 创建缩放后的图片
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }
    
    /**
     * 清理未使用的图片文件
     * 根据数据库中的图片ID列表，删除不再使用的图片文件
     */
    public static void cleanupUnusedImages(Context context, java.util.Set<String> usedImageIds) {
        File imagesDir = getImagesDir(context);
        File[] imageFiles = imagesDir.listFiles();
        
        if (imageFiles != null) {
            for (File file : imageFiles) {
                String fileName = file.getName();
                if (fileName.endsWith(".jpg")) {
                    String imageId = fileName.substring(0, fileName.length() - 4);
                    if (!usedImageIds.contains(imageId)) {
                        boolean deleted = file.delete();
                        Log.d(TAG, "Cleanup unused image: " + fileName + ", deleted: " + deleted);
                    }
                }
            }
        }
    }
    
    /**
     * 获取图片存储目录的总大小（字节）
     */
    public static long getImagesDirSize(Context context) {
        File imagesDir = getImagesDir(context);
        return getDirSize(imagesDir);
    }
    
    /**
     * 计算目录大小
     */
    private static long getDirSize(File dir) {
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += getDirSize(file);
                }
            }
        }
        return size;
    }
}