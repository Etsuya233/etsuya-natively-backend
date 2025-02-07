package com.ety.natively.service;

import com.ety.natively.domain.navi.AskStreamDto;
import com.ety.natively.domain.navi.*;
import jakarta.servlet.http.HttpServletResponse;

import java.security.Principal;
import java.util.concurrent.CompletableFuture;

public interface NaviServiceV2 {
	TranslationVo translate(TranslationDto dto);

	AskVo ask(AskDto dto);

	ExplainVo explain(ExplainDto dto);

	CompletableFuture<Void> pronounce(PronounceDto dto, HttpServletResponse response);

	void askStream(AskStreamDto dto, Principal principal);

	void explainStream(ExplainStreamDto dto, Principal principal);

	void translateStream(TranslateStreamDto dto, Principal principal);
}
