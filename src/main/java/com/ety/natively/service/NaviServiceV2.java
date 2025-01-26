package com.ety.natively.service;

import com.ety.natively.domain.dto.NaviSpeakDto;
import com.ety.natively.domain.navi.*;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface NaviServiceV2 {
	TranslationVo translate(TranslationDto dto);

	AskVo ask(AskDto dto);

	ExplainVo explain(ExplainDto dto);

	CompletableFuture<Void> pronounce(PronounceDto dto, HttpServletResponse response);
}
