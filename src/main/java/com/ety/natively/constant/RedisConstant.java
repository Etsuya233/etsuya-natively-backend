package com.ety.natively.constant;

public interface RedisConstant {
	String AI_CONVERSATION_PREFIX = "ai:conversation:";
	String AI_RECORD_PREFIX = "ai:record:";

	String CHECK_IN_RECORD_PREFIX = "checkin:record:";
	String CHECK_IN_CONSECUTIVE_PREFIX = "checkin:consecutive:";

	String USER_REFRESH_TOKEN_PREFIX = "user:refresh:";
	String USER_VERSION_TOKEN_PREFIX = "user:version:";
	String USER_OAUTH = "user:oauth:";

	String SOCKET_IO_USER_TO_UUID = "socketio:user";
	String SOCKET_IO_UUID_TO_USER = "socketio:uuid";

	/**
	 * 过期时间（秒）
	 */
	Long AI_GENERAL_TTL = 6 * 60 * 60L;
	Long CHECK_IN_CONSECUTIVE_TTL = 2 * 24 + 1L;
	Long USER_VERSION_TTL = 2 * 24 * 60 * 60L;
	Long USER_OAUTH_TTL = 20 * 60L;

	String POST_VERIFICATION_CODE_PREFIX = "post:verification:";
	Long POST_VERIFICATION_CODE_TTL = 10 * 60L; // 10 min

	String POST_SCORE = "post:score";
	String POST_TRENDING_ID_SET = "post:trending:id:set";
	String POST_TRENDING_ID_LIST = "post:trending:id:list";
}
