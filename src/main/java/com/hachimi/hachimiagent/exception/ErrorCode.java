package com.hachimi.hachimiagent.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败"),
    SESSION_NOT_FOUND(40401, "会话不存在"),
    SESSION_DELETE_FAILED(50002, "删除会话失败"),
    SESSION_LOAD_FAILED(50003, "加载会话失败"),
    INVALID_REQUEST(40001, "请求参数无效"),
    OPERATION_NOT_ALLOWED(40301, "操作不允许"),
    AI_PROCESS_ERROR(50002, "AI处理失败"),
    STREAM_INTERRUPT_ERROR(50003, "流式处理中断失败"),
    SESSION_STOP_ERROR(50004, "会话停止失败");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}

