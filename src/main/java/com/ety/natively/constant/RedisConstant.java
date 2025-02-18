package com.ety.natively.constant;

public interface RedisConstant {
	String USER_REFRESH_TOKEN_PREFIX = "user:refresh:";
	String USER_VERSION_TOKEN_PREFIX = "user:version:";
	String USER_OAUTH = "user:oauth:";

	/**
	 * 过期时间（秒）
	 */
	Long USER_VERSION_TTL = 24 * 60 * 60L;
	Long USER_OAUTH_TTL = 20 * 60L;

	String POST_VERIFICATION_CODE_PREFIX = "post:verification:";
	Long POST_VERIFICATION_CODE_TTL = 10 * 60L; // 10 min

	String POST_SCORE = "post:score";
	String POST_TRENDING_ID_SET = "post:trending:id:set";
	String POST_TRENDING_ID_LIST = "post:trending:id:list";
}
