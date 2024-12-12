package com.ety.natively.service;

import com.ety.natively.domain.dto.LookUpDto;
import com.ety.natively.domain.po.Language;
import com.ety.natively.domain.vo.ExplanationVo;
import com.ety.natively.domain.vo.TranslationVo;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

public interface LanguageService {
	List<Language> getLanguages(String lang);

	TranslationVo getTranslation(LookUpDto dto);

	List<Language> getLanguageInNative();

	ExplanationVo getExplanation(LookUpDto dto);

	Flux<ServerSentEvent<String>> getExplanationStream(Long referenceId, Integer type, String originalText, String accessToken, String language);
}
