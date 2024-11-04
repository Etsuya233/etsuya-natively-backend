package com.ety.natively.controller;

import com.ety.natively.domain.R;
import com.ety.natively.domain.po.Language;
import com.ety.natively.domain.po.Location;
import com.ety.natively.service.GeneralService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/general")
@RequiredArgsConstructor
public class GeneralController {

	private final GeneralService generalService;

	@GetMapping("/location/{lang}")
	public R<List<Location>> getLocations(@PathVariable(value = "lang", required = false) String lang){
		List<Location> ret = generalService.getLocations(lang);
		return R.ok(ret);
	}

	@GetMapping("/language/{lang}")
	public R<List<Language>> getLanguages(@PathVariable(value = "lang", required = false) String lang){
		List<Language> ret = generalService.getLanguages(lang);
		return R.ok(ret);
	}

}
