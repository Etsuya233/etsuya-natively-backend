package com.ety.natively.enums;

import lombok.Getter;

@Getter
public enum ExceptionEnum {
	UNKNOWN_ERROR(1000, 200, "unknown.error"),

	USER_ACCESS_TOKEN_FAILED(2000, 200, "user.accessTokenFailed"),
	USER_REFRESH_TOKEN_FAILED(2001, 200, "user.refreshTokenFailed"),
	USER_NOT_FOUND(2002, 200, "user.notFound"),
	USER_PASSWORD_NOT_MATCH(2003, 200, "user.passwordNotMatch"),
	USER_USERNAME_TAKEN(2004, 200, "user.usernameTaken"),
	USER_EMAIL_TAKEN(2005, 200, "user.emailTaken"),
	USER_STATUS_ERROR(2006, 200, "user.statusError"),
	USER_LANGUAGE_FAILED(2007, 200, "user.languageFailed"),
	USER_LANGUAGE_PROFICIENCY_FAILED(2008, 200, "user.languageProficiencyFailed"),
	USER_LOCATION_FAILED(2009, 200, "ex.userLocationFailed");

	final int errorCode;
	final int httpCode;
	final String errorMsgKey;

	ExceptionEnum(int errorCode, int httpCode, String errorMsgKey) {
		this.errorCode = errorCode;
		this.httpCode = httpCode;
		this.errorMsgKey = errorMsgKey;
	}
}
