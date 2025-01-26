package com.ety.natively.controller;

import com.ety.natively.domain.R;
import com.ety.natively.domain.dto.NaviRequestDto;
import com.ety.natively.domain.dto.NaviSpeakDto;
import com.ety.natively.domain.vo.NaviResult;
import com.ety.natively.service.NaviService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/navi")
@RequiredArgsConstructor
@Deprecated
public class NaviController {

	private final NaviService naviService;

	@PostMapping("/ask")
	public R<NaviResult> askNavi(@RequestBody NaviRequestDto dto){
		NaviResult ret = naviService.askNavi(dto);
		return R.ok(ret);
	}

	@PostMapping("/speak")
	public void speak(@RequestBody NaviSpeakDto dto, HttpServletResponse response) throws ExecutionException, InterruptedException {
		naviService.speak(dto, response).get();
	}

}
