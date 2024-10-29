package com.dc.learning.controller;


import com.dc.learning.domain.R;
import com.dc.learning.domain.dto.HelpQueryDto;
import com.dc.learning.domain.po.Help;
import com.dc.learning.domain.po.HelpType;
import com.dc.learning.service.IHelpService;
import com.dc.learning.service.IHelpTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Etsuya
 * @since 2024-10-15
 */
@RestController
@RequestMapping("/help")
@RequiredArgsConstructor
public class HelpController {

	private final IHelpService helpService;
	private final IHelpTypeService helpTypeService;

	@GetMapping("/type")
	public R<List<HelpType>> getAllHelpTypes(){
		List<HelpType> ret = helpTypeService.getAllTypes();
		return R.ok(ret);
	}

	@PostMapping
	public R<List<Help>> getHelpByType(@RequestBody HelpQueryDto dto){
		List<Help> ret = helpService.getHelpByType(dto);
		return R.ok(ret);
	}

}
