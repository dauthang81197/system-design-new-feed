package com.pv.template.exception;

/**
 * Service-specific exception — dùng khi cần throw lỗi nghiệp vụ riêng của service này.
 * Các exception dùng chung (ResourceNotFoundException, ForbiddenException)
 * đã có trong common-lib.
 */
public class ServiceException extends RuntimeException {
    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
