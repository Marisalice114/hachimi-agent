package com.hachimi.hachimiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TerminalOperationTool {

    @Tool(description = "Execute a command in the terminal (supports Windows, Linux, and macOS)")
    public String executeTerminalCommand(@ToolParam(description = "Command to execute in the terminal") String command) {
        return executeTerminalCommand(command, null);
    }

    /**
     * 执行终端命令，可以强制指定操作系统类型
     * @param command 要执行的命令
     * @param forceOS 强制指定操作系统类型 ("windows", "linux", "mac", null为自动检测)
     */
    public String executeTerminalCommand(String command, String forceOS) {
        StringBuilder output = new StringBuilder();

        try {
            Process process = createProcess(command, forceOS);

            // 读取标准输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // 读取错误输出
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    output.append("ERROR: ").append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                output.append("Command execution failed with exit code: ").append(exitCode);
            }

        } catch (IOException | InterruptedException e) {
            output.append("Error executing command: ").append(e.getMessage());
        }

        return output.toString();
    }

    /**
     * 根据操作系统类型创建相应的进程
     */
    private Process createProcess(String command, String forceOS) throws IOException {
        String osType = forceOS != null ? forceOS.toLowerCase() :
                System.getProperty("os.name").toLowerCase();

        if (osType.contains("win")) {
            // Windows系统
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
            return builder.start();
        } else if (osType.contains("nix") || osType.contains("nux") || osType.contains("mac") || osType.contains("linux")) {
            // Unix/Linux/macOS系统
            ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", command);
            return builder.start();
        } else {
            // 其他系统，尝试直接执行
            return Runtime.getRuntime().exec(command);
        }
    }

    /**
     * 获取当前操作系统类型
     */
    public String getOperatingSystem() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return "Windows";
        } else if (osName.contains("mac")) {
            return "macOS";
        } else if (osName.contains("nix") || osName.contains("nux")) {
            return "Linux/Unix";
        } else {
            return "Unknown: " + osName;
        }
    }
}
