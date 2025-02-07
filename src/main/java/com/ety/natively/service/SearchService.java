package com.ety.natively.service;

import com.ety.natively.domain.ElasticVo;
import com.ety.natively.domain.dto.SearchDto;
import com.ety.natively.domain.vo.PostSearchResult;
import com.ety.natively.domain.vo.UserVo;

import java.util.List;

public interface SearchService {
	ElasticVo<List<PostSearchResult>> search(SearchDto dto);

	ElasticVo<List<UserVo>> searchUser(SearchDto dto);
}
