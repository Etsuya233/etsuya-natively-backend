package com.ety.natively.service;

import com.ety.natively.domain.dto.NaviRequestDto;
import com.ety.natively.domain.dto.NaviSpeakDto;
import com.ety.natively.domain.vo.NaviResult;
import jakarta.servlet.http.HttpServletResponse;

import java.util.concurrent.CompletableFuture;

@Deprecated
public interface NaviService {
	NaviResult askNavi(NaviRequestDto dto);

	CompletableFuture<Void> speak(NaviSpeakDto dto, HttpServletResponse response);
}
