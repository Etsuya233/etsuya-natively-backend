package com.ety.natively.controller;

import com.ety.natively.domain.R;
import com.ety.natively.domain.navi.TranslationDto;
import com.ety.natively.domain.navi.TranslationVo;
import com.ety.natively.service.NaviServiceV2;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
