package com.ety.natively.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.ety.natively.constant.ElasticConstant;
import com.ety.natively.domain.ElasticVo;
import com.ety.natively.domain.dto.SearchDto;
import com.ety.natively.domain.vo.PostSearchResult;
import com.ety.natively.domain.vo.UserSearchResult;
import com.ety.natively.domain.vo.UserVo;
import com.ety.natively.service.IUserService;
import com.ety.natively.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

	private final ElasticsearchClient client;
	private final IUserService userService;

	@Override
	public ElasticVo<List<PostSearchResult>> search(SearchDto dto) {
		String userInput = dto.getContent();
		int from = dto.getFrom() == null? 0: dto.getFrom();
		if(!StringUtils.hasText(userInput)){
			return ElasticVo.of(new ArrayList<>(), from);
		}

		SearchRequest request = new SearchRequest.Builder()
				.index("post")
				.query(q -> q
						.match(m -> m.field("content")
						.query(userInput)))
				.from(from)
				.size(10)
				.highlight(h -> h.fields("content",
						f -> f.preTags("<em>").postTags("</em>"))).build();
		SearchResponse<PostSearchResult> searchResponse;
		try {
			searchResponse = client.search(request, PostSearchResult.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if(searchResponse.hits().hits().isEmpty()){
			return ElasticVo.of(new ArrayList<>(), from);
		}

		List<PostSearchResult> results = new ArrayList<>();
		List<Long> userIds = new ArrayList<>();
		from += searchResponse.hits().hits().size();
		searchResponse.hits().hits().forEach(hit -> {
			PostSearchResult source = hit.source();
			if(source != null){
				source.setHighlightedContent(hit.highlight().get("content").getFirst());
				source.setContent(null);
				results.add(source);
				userIds.add(source.getUserId());
			}
		});

		List<UserVo> users = userService.getUserByIds(userIds);
		Map<Long, UserVo> userMap = users.stream()
				.collect(Collectors.toMap(UserVo::getId, Function.identity()));

		results.forEach(result -> {
			UserVo user = userMap.get(result.getUserId());
			result.setNickname(user.getNickname());
			result.setAvatar(user.getAvatar());
			result.setUserLanguages(user.getLanguages());
		});

		// todo elastic search not return raw content

		return ElasticVo.of(results, from);
	}

	@Override
	public ElasticVo<List<UserVo>> searchUser(SearchDto dto) {
		String content = dto.getContent();
		int from = dto.getFrom() == null? 0: dto.getFrom();
		if(!StringUtils.hasText(content)){
			return ElasticVo.of(
					new ArrayList<>(),
					from
			);
		}
		SearchRequest searchRequest = new SearchRequest.Builder()
				.index(ElasticConstant.USER_INDEX)
				.query(qu -> qu.multiMatch(mu -> mu
						.fields(List.of("username.ngram", "nickname.ngram", "id"))
						.query(content)))
				.from(from)
				.size(10)
				.build();

		List<Long> userIds = new ArrayList<>();
		try {
			SearchResponse<UserSearchResult> response = client.search(searchRequest, UserSearchResult.class);
			if(response.hits().hits().isEmpty()){
				return ElasticVo.of(new ArrayList<>(), from);
			}
			for (Hit<UserSearchResult> hit : response.hits().hits()) {
				UserSearchResult source = hit.source();
				if(source != null && source.getId() != null){
					userIds.add(source.getId());
				}
			}
			from += response.hits().hits().size();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		List<UserVo> users = userService.getUserByIds(userIds);

		return ElasticVo.of(users, from);
	}
}
