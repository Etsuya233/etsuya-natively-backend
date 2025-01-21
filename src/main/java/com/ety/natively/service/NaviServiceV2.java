package com.ety.natively.service;

import com.ety.natively.domain.navi.TranslationDto;
import com.ety.natively.domain.navi.TranslationVo;

public interface NaviServiceV2 {
	TranslationVo translate(TranslationDto dto);
}
