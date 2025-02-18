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
	USER_LOCATION_FAILED(2009, 200, "ex.userLocationFailed"),
	USER_CANNOT_FOLLOW_WHEN_YOU_BLOCKED_SOMEONE(2010, 200, "user.cannotFollowWhenYouBlockedSomeone"),
	USER_YOU_ARE_BLOCKED(2011, 200, "user.youAreBlocked"),
	USER_OAUTH2_FAILED(2012, 200, "user.oauth2Failed"),
	USER_AVATAR_UPLOAD_FAILED(2013, 200, "user.avatarUploadError"),
	USER_PASSWORD_FORMAT_NOT_MATCH(2014, 200, "user.passwordFormatNotMatch"),

	POST_COMMENT_NOT_YOURS(3000, 200, "post.commentNotYours"),
	POST_TITLE_CANNOT_BE_EMPTY(3001, 200, "post.postTitleEmpty"),
	POST_TITLE_RULE(3002, 200, "post.postTitleRule"),
	POST_NOT_EXIST(3003, 200, "post.notExist"),
	POST_IMAGE_UPLOAD_ERROR(3004, 200, "post.imageUploadError"),
	POST_VOICE_UPLOAD_ERROR(3005, 200, "post.voiceUploadError"),
	POST_VERIFICATION_FAILED(3006, 200, "post.verificationFailed"),
	POST_BLOCK_OVER_LIMIT(3007, 200, "post.blockOverLimit"),
	POST_CONTENT_OVER_LIMIT(3008, 200, "post.contentOverLimit"),
	POST_IMAGE_OVER_COUNT_LIMIT(3009, 200, "post.imageOverCountLimit"),
	POST_VOICE_OVER_COUNT_LIMIT(3010, 200, "post.voiceOverCountLimit"),
	POST_TYPE_NOT_EXIST(3011, 200, "post.typeNotExist"),
	POST_TITLE_TOO_LONG(3012, 200, "post.titleTooLong"),
	POST_CANNOT_REPEAT_VOTE(3013, 200, "post.cannotRepeatVote"),
	POST_PARSE_FAILED(3014, 200, "post.parseFailed"),
	POST_COMMENT_LENGTH_LIMIT(3015, 200 , "post.commentLengthLimit" ),
	POST_COMMENT_NOT_EXIST(3016, 200, "post.commentNotExist"),
	POST_LANGUAGE_REQUIRED(3017, 200, "post.languageRequired"),
	POST_UNSUPPORTED_LANGUAGE(3018, 200, "post.unsupportedLanguage"),
	POST_BOOKMARK_CONTENT_LIMIT(3019, 200, "post.bookmarkContentLimit"),
	POST_BOOKMARK_NOTE_LIMIT(3020, 200, "post.bookmarkNoteLimit"),
	POST_BOOKMARK_NOT_EXIST(3021, 200, "post.bookmarkNotExist"),
	POST_NOT_YOURS(3022, 200, "post.notYours"),
	POST_ATTACHMENT_UNSUPPORTED_TYPE(3023, 200, "post.attachmentUnsupportedType"),
	POST_IMAGE_OVER_SIZE_LIMIT(3024, 200, "post.imageOverSizeLimit"),
	POST_VOICE_OVER_SIZE_LIMIT(3025, 200, "post.voiceOverSizeLimit"),
	USER_UNSUPPORTED_LANGUAGE(3026, 200, "post.userUnsupportedLanguage"),

	CHAT_NOT_CONTACT(4001, 200, "chat.notContact"),
	CHAT_ONLY_ONE_MESSAGE_IN_A_WEEK_WHEN_NOT_CONTACT(4002, 200, "chat.onlyOneMessageInAWeekWhenNotContact"),

	NAVI_SERVER_ERROR(5001, 200, "navi.serverError"),

	SEARCH_NOT_SUPPORTED_LANGUAGE(6001, 200, "search.notSupportedLanguage");

	final int errorCode;
	final int httpCode;
	final String errorMsgKey;

	ExceptionEnum(int errorCode, int httpCode, String errorMsgKey) {
		this.errorCode = errorCode;
		this.httpCode = httpCode;
		this.errorMsgKey = errorMsgKey;
	}
}
