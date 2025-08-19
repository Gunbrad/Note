package com.example.note.util;

import java.util.UUID;

/**
 * UUID工具类
 * 用于生成唯一标识符
 */
public class UuidUtils {
    
    /**
     * 生成随机UUID字符串
     */
    public static String generateUuid() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * 生成不带连字符的UUID字符串
     */
    public static String generateUuidWithoutHyphens() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 生成图片ID
     * 使用不带连字符的UUID格式
     */
    public static String generateImageId() {
        return generateUuidWithoutHyphens();
    }
    
    /**
     * 验证UUID字符串格式是否正确
     */
    public static boolean isValidUuid(String uuidStr) {
        if (uuidStr == null || uuidStr.isEmpty()) {
            return false;
        }
        try {
            UUID.fromString(uuidStr);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * 验证图片ID格式是否正确
     * 图片ID为32位不带连字符的十六进制字符串
     */
    public static boolean isValidImageId(String imageId) {
        if (imageId == null || imageId.length() != 32) {
            return false;
        }
        return imageId.matches("[0-9a-fA-F]{32}");
    }
    
    /**
     * 将带连字符的UUID转换为不带连字符的格式
     */
    public static String removeHyphens(String uuidStr) {
        if (uuidStr == null) {
            return null;
        }
        return uuidStr.replace("-", "");
    }
    
    /**
     * 将不带连字符的UUID转换为标准格式
     */
    public static String addHyphens(String uuidStr) {
        if (uuidStr == null || uuidStr.length() != 32) {
            return uuidStr;
        }
        return String.format("%s-%s-%s-%s-%s",
                uuidStr.substring(0, 8),
                uuidStr.substring(8, 12),
                uuidStr.substring(12, 16),
                uuidStr.substring(16, 20),
                uuidStr.substring(20, 32));
    }
}