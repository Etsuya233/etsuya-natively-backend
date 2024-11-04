package com.ety.natively.service.impl;

import com.ety.natively.domain.po.Language;
import com.ety.natively.domain.po.Location;
import com.ety.natively.service.GeneralService;
import com.ety.natively.utils.BaseContext;
import jakarta.annotation.PostConstruct;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GeneralServiceImpl implements GeneralService {

	private final List<Location> enLocations = new ArrayList<>(), zhCnLocations = new ArrayList<>(),
			jaLocations = new ArrayList<>(), frLocations = new ArrayList<>(),
			koLocations = new ArrayList<>();

	private final Locale[] languages = new Locale[]{ Locale.ENGLISH, Locale.CHINA,
			Locale.JAPANESE, Locale.FRENCH, Locale.KOREAN};
	private final List<Language> enLanguages = new ArrayList<>(), zhCnLanguages = new ArrayList<>(),
			jaLanguages = new ArrayList<>(), frLanguages = new ArrayList<>(),
			koLanguages = new ArrayList<>();
	public final Set<String> languageCodes = new HashSet<>();

	@PostConstruct
	public void init() {
		for (String locationCode : Locale.getISOCountries()) {
			Locale locale = Locale.of("", locationCode);
			enLocations.add(new Location(locale.getDisplayName(Locale.ENGLISH), locationCode));
			zhCnLocations.add(new Location(locale.getDisplayName(Locale.CHINA), locationCode));
			jaLocations.add(new Location(locale.getDisplayName(Locale.JAPAN), locationCode));
			frLocations.add(new Location(locale.getDisplayName(Locale.FRANCE), locationCode));
			koLocations.add(new Location(locale.getDisplayName(Locale.KOREAN), locationCode));
		}
		for(Locale locale : languages) {
			languageCodes.add(locale.getLanguage());
			enLanguages.add(new Language(locale.getDisplayLanguage(Locale.ENGLISH), locale.getLanguage()));
			zhCnLanguages.add(new Language(locale.getDisplayLanguage(Locale.CHINA), locale.getLanguage()));
			jaLanguages.add(new Language(locale.getDisplayLanguage(Locale.JAPAN), locale.getLanguage()));
			frLanguages.add(new Language(locale.getDisplayLanguage(Locale.FRANCE), locale.getLanguage()));
			koLanguages.add(new Language(locale.getDisplayLanguage(Locale.KOREAN), locale.getLanguage()));
		}
	}

	@Override
	public List<Location> getLocations(String lang) {
		if(lang != null) lang = lang.toLowerCase();
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
	public List<Language> getLanguages(String lang) {
		if(lang != null) lang = lang.toLowerCase();
		else lang = BaseContext.getLanguage().getLanguage();
		return switch (lang) {
			case "zh", "zh-cn" -> zhCnLanguages;
			case "ja" -> jaLanguages;
			case "fr" -> frLanguages;
			case "ko" -> koLanguages;
			default -> enLanguages;
		};
	}

	@Override
	public Set<String> getLanguageCodes() {
		return languageCodes;
	}
}
