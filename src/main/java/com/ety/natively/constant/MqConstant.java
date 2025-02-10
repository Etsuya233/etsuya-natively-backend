package com.ety.natively.constant;

public interface MqConstant {
	interface EXCHANGE {
		String POST_TOPIC = "post.topic";
	}

	interface KEY {
		String POST_DELETE = "post.delete";
		String COMMENT_DELETE = "comment.delete";
		String COMMENT_SCORE = "comment.score";
		String POST_AI = "post.ai";
	}

	interface QUEUE {
		String POST_DELETE = "post.delete";
		String COMMENT_DELETE = "comment.delete";
		String COMMENT_SCORE = "comment.score";
		String POST_AI = "post.ai";
	}
}
