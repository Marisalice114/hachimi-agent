package com.hachimi.hachimiagent.common;

import com.hachimi.hachimiagent.exception.ErrorCode;

public class ResultUtils {

    /**
     * 成功
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok");
    }

    /**
     * 成功(无数据)
     */
    public static <T> BaseResponse<T> success() {
        return new BaseResponse<>(0, null, "ok");
    }

    /**
     * 成功(自定义消息)
     */
    public static <T> BaseResponse<T> success(T data, String message) {
        return new BaseResponse<>(0, data, message);
    }

    // =================== 错误处理 ===================

    /**
     * 失败 - 使用 ErrorCode
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode.getCode(), null, errorCode.getMessage());
    }

    /**
     * 失败 - 使用 code + message
     */
    public static <T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }

    /**
     * 失败 - 默认错误码 500
     */
    public static <T> BaseResponse<T> error(String message) {
        return new BaseResponse<>(500, null, message);
    }
}