package com.ety.natively.service;

import com.ety.natively.domain.po.Language;
import com.ety.natively.domain.po.Location;

import java.util.List;
import java.util.Set;

public interface GeneralService {
	List<Location> getLocations(String lang);

	Set<String> getLanguageCodes();
}
