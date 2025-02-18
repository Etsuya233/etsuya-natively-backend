package com.ety.natively.constant;

import com.ety.natively.domain.vo.PostPreview;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public interface Constant {
	long ACCESS_TOKEN_TTL = 25 * 60 * 60;
	long REFRESH_TOKEN_TTL = 3 * 24 * 60 * 60;

	int POST_BLOCK_LIMIT = 30;
	int POST_IMAGE_LIMIT = 10;
	int POST_VOICE_LIMIT = 10;
	int POST_IMAGE_SIZE_LIMIT = 4;
	int POST_VOICE_SIZE_LIMIT = 2;
	int POST_CONTENT_LIMIT = 30000;
	int POST_TITLE_LIMIT = 200;
	int POST_PREVIEW_LIMIT = 250;
	int COMMENT_LENGTH_LIMIT = 3096;

	int BOOKMARK_CONTENT_LIMIT = 30000;
	int BOOKMARK_NOTE_LIMIT = 1000;

	// Trending
	AtomicBoolean POST_TRENDING_RECORDING = new AtomicBoolean(false);
	List<Long> POST_TRENDING_ID_LIST = new ArrayList<>();
	Set<Long> POST_TRENDING_ID_SET = new HashSet<>();
	List<PostPreview> POST_TRENDING_PREVIEW_LIST = new ArrayList<>();
}
