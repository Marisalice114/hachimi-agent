package com.hachimi.hachimiagent.tools;


import com.hachimi.hachimiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import static cn.hutool.core.io.FileUtil.*;

/**
 * 文件操作工具类
 */
public class FileOperationTool {

    private String filePath = FileConstant.FILE_SAVE_PATH;

    @Tool(description = "read file content")
    public String fileRead(@ToolParam(description = "Name of file to read") String fileName) {
        String fileFullPath = filePath + "/" + fileName;
        try {
            return readUtf8String(fileFullPath);
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }

    }

    @Tool(description = "write file content")
    public String fileWrite(@ToolParam(description = "Name of file to write") String fileName,
                            @ToolParam(description = "Content to write to the file") String content) {
        String fileFullPath = filePath + "/" + fileName;
        try {
            mkdir(filePath); // Ensure the directory exists
            writeUtf8String(content, fileFullPath);
            return "File written successfully: " + fileFullPath;
        } catch (Exception e) {
            return "Error writing file: " + e.getMessage();
        }
    }

}
