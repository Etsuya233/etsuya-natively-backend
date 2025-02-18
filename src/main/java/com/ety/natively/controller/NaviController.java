package com.ety.natively.controller;

import com.ety.natively.domain.R;
import com.ety.natively.domain.navi.TranslateStreamDto;
import com.ety.natively.domain.navi.*;
import com.ety.natively.service.NaviService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.concurrent.ExecutionException;

@RestController
@Slf4j
@RequestMapping("/naviV2")
@RequiredArgsConstructor
public class NaviController {

	private final NaviService naviService;

	@PostMapping("/translate")
	public R<TranslationVo> translate(@RequestBody TranslationDto dto) {
		TranslationVo ret = naviService.translate(dto);
		return R.ok(ret);
	}

	@PostMapping("/ask")
	public R<AskVo> ask(@RequestBody AskDto dto) {
		AskVo ret = naviService.ask(dto);
		return R.ok(ret);
	}

	@MessageMapping("/ask/stream")
	public void askStream(AskStreamDto dto, Principal principal) {
		naviService.askStream(dto, principal);
	}


	@PostMapping("/explain")
	public R<ExplainVo> explain(@RequestBody ExplainDto dto) {
		ExplainVo ret = naviService.explain(dto);
		return R.ok(ret);
	}

	@MessageMapping("/explain/stream")
	public void explainStream(ExplainStreamDto dto, Principal principal) {
		naviService.explainStream(dto, principal);
	}

	@MessageMapping("/translate/stream")
	public void translateStream(TranslateStreamDto dto, Principal principal){
		naviService.translateStream(dto, principal);
	}

	@PostMapping("/pronounce")
	public void pronounce(@RequestBody PronounceDto dto, HttpServletResponse response) throws ExecutionException, InterruptedException {
		naviService.pronounce(dto, response).get();
	}
}
