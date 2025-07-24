package com.hachimi.hachimiimagesearchmcpserver.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ImageSearchTool {

    @Value("${pexels.api-key}")
    private String PEXELS_API_KEY;

    @Value("${pexels.default-count:5}")
    private int defaultImageCount;

    @Value("${pexels.max-count:20}")
    private int maxImageCount;

    // Pexels 常规搜索接口
    private static final String API_URL = "https://api.pexels.com/v1/search";

    // ========== 原来的方法（保持不变） ==========
    @Tool(description = "search image from web")
    public String searchImage(@ToolParam(description = "Search query keyword") String query) {
        try {
            return String.join(",", searchMediumImages(query));
        } catch (Exception e) {
            return "Error search image: " + e.getMessage();
        }
    }

    // ========== 新增的方法 ==========
    @Tool(description = "Search images with specified count from web")
    public String searchImageWithCount(
            @ToolParam(description = "Search query keyword") String query,
            @ToolParam(description = "Number of images to return (1-20, default 5)") Integer count) {
        try {
            int imageCount = processImageCount(count);
            List<String> images = searchMediumImages(query, imageCount);
            return String.join(",", images);
        } catch (Exception e) {
            return "Error search image: " + e.getMessage();
        }
    }

    @Tool(description = "Search few images from web (quick search, returns 3 images)")
    public String searchFewImages(@ToolParam(description = "Search query keyword") String query) {
        try {
            List<String> images = searchMediumImages(query, 3);
            return String.join(",", images);
        } catch (Exception e) {
            return "Error search image: " + e.getMessage();
        }
    }

    @Tool(description = "Search many images from web (comprehensive search, returns 12 images)")
    public String searchManyImages(@ToolParam(description = "Search query keyword") String query) {
        try {
            List<String> images = searchMediumImages(query, 12);
            return String.join(",", images);
        } catch (Exception e) {
            return "Error search image: " + e.getMessage();
        }
    }

    @Tool(description = "Search single image from web (returns only 1 image)")
    public String searchSingleImage(@ToolParam(description = "Search query keyword") String query) {
        try {
            List<String> images = searchMediumImages(query, 1);
            return images.isEmpty() ? "No image found" : images.get(0);
        } catch (Exception e) {
            return "Error search image: " + e.getMessage();
        }
    }

    // ========== 工具方法 ==========
    /**
     * 处理图片数量参数
     */
    private int processImageCount(Integer count) {
        if (count == null) {
            return defaultImageCount;
        }

        // 限制在合理范围内
        if (count < 1) {
            return 1;
        }
        if (count > maxImageCount) {
            return maxImageCount;
        }

        return count;
    }

    /**
     * 搜索指定数量的中等尺寸图片（新方法）
     */
    public List<String> searchMediumImages(String query, int count) {
        // 设置请求头（包含API密钥）
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", PEXELS_API_KEY);

        // 设置请求参数
        Map<String, Object> params = new HashMap<>();
        params.put("query", query);
        params.put("per_page", Math.min(count, 80)); // Pexels API限制
        params.put("page", 1);

        // 发送 GET 请求
        String response = HttpUtil.createGet(API_URL)
                .addHeaders(headers)
                .form(params)
                .execute()
                .body();

        // 解析响应JSON并限制返回数量
        return JSONUtil.parseObj(response)
                .getJSONArray("photos")
                .stream()
                .map(photoObj -> (JSONObject) photoObj)
                .map(photoObj -> photoObj.getJSONObject("src"))
                .map(photo -> photo.getStr("medium"))
                .filter(StrUtil::isNotBlank)
                .limit(count)  // 限制返回数量
                .collect(Collectors.toList());
    }

    /**
     * 原来的方法（保持兼容性）
     */
    public List<String> searchMediumImages(String query) {
        return searchMediumImages(query, defaultImageCount);
    }
}