package com.ety.natively.controller;

import com.ety.natively.domain.R;
import com.ety.natively.domain.dto.ExplanationDto;
import com.ety.natively.domain.dto.LookUpDto;
import com.ety.natively.domain.po.Language;
import com.ety.natively.domain.vo.ExplanationVo;
import com.ety.natively.domain.vo.TranslationVo;
import com.ety.natively.service.LanguageService;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/language")
@RequiredArgsConstructor
public class LanguageController {

	private final LanguageService languageService;
	private final SqlSessionFactory sqlSessionFactory;

	@GetMapping("/lang/{lang}")
	public R<List<Language>> getLanguages(@PathVariable(value = "lang", required = false) String lang){
		List<Language> ret = languageService.getLanguages(lang);
		return R.ok(ret);
	}

	@GetMapping("/lang/native")
	public R<List<Language>> getLanguagesInNativeForm(){
		List<Language> ret = languageService.getLanguageInNative();
		return R.ok(ret);
	}

	@Deprecated
	@PostMapping("/translation")
	public R<TranslationVo> getTranslation(@RequestBody LookUpDto dto){
		TranslationVo vo = languageService.getTranslation(dto);
		return R.ok(vo);
	}

	@Deprecated
	@PostMapping("/explanation")
	public R<ExplanationVo> getExplanation(@RequestBody LookUpDto dto){
		ExplanationVo ret = languageService.getExplanation(dto);
		return R.ok(ret);
	}

	@Deprecated
	@GetMapping("/explanation/stream")
	public Flux<ServerSentEvent<String>> getExplanationStream(@RequestParam(required = false) Long referenceId,
															  @RequestParam(required = true) Integer type,
															  @RequestParam(required = false) String originalText,
															  @RequestParam(required = true) String accessToken,
															  @RequestParam(required = true) String language){
		return languageService.getExplanationStream(referenceId, type, originalText, accessToken, language);
	}

}
