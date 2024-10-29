package com.dc.learning.constant;

public interface RedisConstant {
	String AI_CONVERSATION_PREFIX = "ai:conversation:";
	String AI_RECORD_PREFIX = "ai:record:";

	String CHECK_IN_RECORD_PREFIX = "checkin:record:";
	String CHECK_IN_CONSECUTIVE_PREFIX = "checkin:consecutive:";

	String USER_REFRESH_TOKEN_PREFIX = "user:refresh:";
	String USER_VERSION_TOKEN_PREFIX = "user:version:";

	/**
	 * 过期时间（秒）
	 */
	Long AI_GENERAL_TTL = 6 * 60 * 60L;
	Long CHECK_IN_CONSECUTIVE_TTL = 2 * 24 + 1L;
}
