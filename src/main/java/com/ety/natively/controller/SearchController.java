package com.ety.natively.controller;

import cn.hutool.core.annotation.MirroredAnnotationAttribute;
import com.ety.natively.domain.ElasticVo;
import com.ety.natively.domain.R;
import com.ety.natively.domain.dto.SearchDto;
import com.ety.natively.domain.vo.PostSearchResult;
import com.ety.natively.domain.vo.UserVo;
import com.ety.natively.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

	private final SearchService searchService;

	@PostMapping
	public R<ElasticVo<List<PostSearchResult>>> search(@RequestBody SearchDto dto){
		ElasticVo<List<PostSearchResult>> ret = searchService.search(dto);
		return R.ok(ret);
	}

	@PostMapping("/user")
	public R<ElasticVo<List<UserVo>>> searchUser(@RequestBody SearchDto dto){
		ElasticVo<List<UserVo>> ret = searchService.searchUser(dto);
		return R.ok(ret);
	}

}
