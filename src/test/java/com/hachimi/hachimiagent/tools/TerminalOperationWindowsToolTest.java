package com.hachimi.hachimiagent.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TerminalOperationWindowsToolTest {

    @Test
    void executeTerminalCommand() {

        TerminalOperationWindowsTool tool = new TerminalOperationWindowsTool();
        String command = "echo Hello, World!";
        String result = tool.executeTerminalCommand(command);
        assertNotNull(result);
        assertTrue(result.contains("Hello, World!"), "The command output should contain 'Hello, World!'");
    }
}