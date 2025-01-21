package com.ety.natively.constant;

public interface Constant {
	long ACCESS_TOKEN_TTL = 25 * 60 * 60;
	long REFRESH_TOKEN_TTL = 3 * 24 * 60 * 60;

	int POST_BLOCK_LIMIT = 30;
	int POST_IMAGE_LIMIT = 10;
	int POST_VOICE_LIMIT = 10;
	int POST_CONTENT_LIMIT = 65500;
	int POST_TITLE_LIMIT = 245;
	int POST_PREVIEW_LIMIT = 250;
	int COMMENT_LENGTH_LIMIT = 3096;
}
