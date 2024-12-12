package com.ety.natively.service.impl;

import com.ety.natively.domain.po.Language;
import com.ety.natively.domain.po.Location;
import com.ety.natively.service.GeneralService;
import com.ety.natively.utils.BaseContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static com.ety.natively.utils.I18NUtil.*;

@Service
public class GeneralServiceImpl implements GeneralService {

	@Override
	public List<Location> getLocations(String lang) {
		if (lang != null) lang = lang.toLowerCase();
		else lang = BaseContext.getLanguage().getLanguage();
		return switch (lang) {
			case "zh", "zh-cn" -> zhCnLocations;
			case "ja" -> jaLocations;
			case "fr" -> frLocations;
			case "ko" -> koLocations;
			default -> enLocations;
		};
	}

	@Override
	public Set<String> getLanguageCodes() {
		return languageCodes;
	}
}
