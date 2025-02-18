package com.ety.natively.config;

public interface StompConstant {
	String USER_NAVI_CHANNEL = "/queue/navi"; // /exchange/amq.direct/system
	String USER_CHAT_CHANNEL = "/queue/chat"; // /exchange/amq.direct/chat
	String USER_SYSTEM_CHANNEL = "/queue/system"; // /exchange/amq.direct/navi
}
