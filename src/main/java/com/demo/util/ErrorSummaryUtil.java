package com.demo.util;


public class ErrorSummaryUtil {

    private static final int MAX_LENGTH = 200;

    /**
     * 提取精简的错误信息（适合存数据库）
     */
    public static String extractSummary(Exception e) {
        String message = e.getMessage();
        String className = e.getClass().getSimpleName();

        if (message == null || message.isEmpty()) {
            return className;
        }

        // 去除换行符，压缩空格
        String cleaned = message.replaceAll("\\s+", " ").trim();

        // 截断
        if (cleaned.length() > MAX_LENGTH) {
            cleaned = cleaned.substring(0, MAX_LENGTH) + "...";
        }

        return className + ": " + cleaned;
    }

}
