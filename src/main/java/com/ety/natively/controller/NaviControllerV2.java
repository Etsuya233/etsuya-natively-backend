package com.ety.natively.controller;

import com.ety.natively.domain.R;
import com.ety.natively.domain.dto.NaviSpeakDto;
import com.ety.natively.domain.navi.*;
import com.ety.natively.service.NaviServiceV2;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/naviV2")
@RequiredArgsConstructor
public class NaviControllerV2 {

	private final NaviServiceV2 naviService;

	@PostMapping("/translate")
	public R<TranslationVo> translate(@RequestBody TranslationDto dto){
		TranslationVo ret = naviService.translate(dto);
		return R.ok(ret);
	}

	@PostMapping("/ask")
	public R<AskVo> ask(@RequestBody AskDto dto){
		AskVo ret = naviService.ask(dto);
		return R.ok(ret);
	}

	@PostMapping("/explain")
	public R<ExplainVo> explain(@RequestBody ExplainDto dto){
		ExplainVo ret = naviService.explain(dto);
		return R.ok(ret);
	}

	@PostMapping("/pronounce")
	public void pronounce(@RequestBody PronounceDto dto, HttpServletResponse response) throws ExecutionException, InterruptedException {
		naviService.pronounce(dto, response).get();
	}

}
