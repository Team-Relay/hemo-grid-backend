package com.sentinel.hemo_grid.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

	private final HttpStatus status;
	private final ErrorCode code;

	public BusinessException(HttpStatus status, ErrorCode code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	public HttpStatus status() {
		return status;
	}

	public ErrorCode code() {
		return code;
	}
}
