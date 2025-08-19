package com.example.note.data.entity;

/**
 * 单元格类型枚举
 * 支持自动识别的数据类型
 */
public enum CellType {
    TEXT("文本"),
    NUMBER("数字"),
    DATE("日期"),
    BOOLEAN("布尔"),
    IMAGE("图片");
    
    private final String displayName;
    
    CellType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 根据内容自动识别单元格类型
     */
    public static CellType detectType(String content) {
        if (content == null || content.trim().isEmpty()) {
            return TEXT;
        }
        
        content = content.trim();
        
        // 检查是否为布尔值
        if (isBooleanValue(content)) {
            return BOOLEAN;
        }
        
        // 检查是否为数字
        if (isNumericValue(content)) {
            return NUMBER;
        }
        
        // 检查是否为日期
        if (isDateValue(content)) {
            return DATE;
        }
        
        // 检查是否为图片路径
        if (isImagePath(content)) {
            return IMAGE;
        }
        
        // 默认为文本
        return TEXT;
    }
    
    private static boolean isBooleanValue(String content) {
        String lower = content.toLowerCase();
        return lower.equals("true") || lower.equals("false") ||
               lower.equals("是") || lower.equals("否") ||
               lower.equals("✓") || lower.equals("✗") ||
               lower.equals("1") || lower.equals("0");
    }
    
    private static boolean isNumericValue(String content) {
        try {
            Double.parseDouble(content);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private static boolean isDateValue(String content) {
        // 简单的日期格式检查
        return content.matches("\\d{4}-\\d{1,2}-\\d{1,2}") ||
               content.matches("\\d{4}/\\d{1,2}/\\d{1,2}") ||
               content.matches("\\d{1,2}-\\d{1,2}-\\d{4}") ||
               content.matches("\\d{1,2}/\\d{1,2}/\\d{4}");
    }
    
    private static boolean isImagePath(String content) {
        String lower = content.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
               lower.endsWith(".png") || lower.endsWith(".gif") ||
               lower.endsWith(".bmp") || lower.endsWith(".webp") ||
               content.startsWith("content://") || content.startsWith("file://");
    }
}