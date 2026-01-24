package com.banking.common.response;

public class BaseResponse<T> {

    private T data;
    private String message;
    private boolean success;

    public BaseResponse(T data, String message, boolean success) {
        this.data = data;
        this.message = message;
        this.success = success;
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(data, "Success", true);
    }

    public static <T> BaseResponse<T> error(String message) {
        return new BaseResponse<>(null, message, false);
    }

    // Getters and Setters
    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
