package com.ety.natively.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.ety.natively.constant.ElasticConstant;
import com.ety.natively.domain.ElasticVo;
import com.ety.natively.domain.dto.SearchDto;
import com.ety.natively.domain.elastic.PostDocument;
import com.ety.natively.domain.vo.PostSearchResult;
import com.ety.natively.domain.vo.UserSearchResult;
import com.ety.natively.domain.vo.UserVo;
import com.ety.natively.enums.ExceptionEnum;
import com.ety.natively.exception.BaseException;
import com.ety.natively.service.IUserService;
import com.ety.natively.service.SearchService;
import com.ety.natively.utils.I18NUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

	private final ElasticsearchClient client;
	private final IUserService userService;

	// todo search result
	// todo user bio
	@Override
	public ElasticVo<List<PostSearchResult>> search(SearchDto dto) {
		String userInput = dto.getContent();
		int from = dto.getFrom() == null? 0: dto.getFrom();
		if(!StringUtils.hasText(userInput)){
			return ElasticVo.of(new ArrayList<>(), from);
		}

		List<String> excludeLanguage = dto.getExcludeLanguage();
		ArrayList<FieldValue> excludeLanguageFieldValue = new ArrayList<>();
		if(excludeLanguage != null){
			for(String language: excludeLanguage){
				if(!I18NUtil.isSupportedLanguage(language)){
					throw new BaseException(ExceptionEnum.SEARCH_NOT_SUPPORTED_LANGUAGE);
				}
				excludeLanguageFieldValue.add(FieldValue.of(language));
			}
		}

		// using multi match
		SortOptions sortOption = switch (dto.getSort()){
			case 1 -> SortOptions.of(s -> s.field(fi -> fi.field("id").order(SortOrder.Desc)));
			case 2 -> SortOptions.of(s -> s.field(fi -> fi.field("id").order(SortOrder.Asc)));
			default -> SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc)));
		};
		SearchResponse<PostDocument> searchResponse;
		try {
			int finalFrom = from;
			searchResponse = client.search(se -> se.
					index(ElasticConstant.POST_INDEX)
							.query(qu -> qu
									.bool(bo -> bo
											.must(mu -> mu
													.multiMatch(mm -> mm
															.fields("content", "content.cn", "content.en",
																	"content.ja", "content.fr", "content.ko")
															.query(userInput)))
											.mustNot(mn -> mn
													.terms(te -> te
															.field("languages")
															.terms(t -> t.value(excludeLanguageFieldValue))))))
							.highlight(h -> h
									.preTags("<em>")
									.postTags("</em>")
									.fields("content", f -> f)
									.fields("content.cn", f -> f)
									.fields("content.en", f -> f)
									.fields("content.ja", f -> f)
									.fields("content.fr", f -> f)
									.fields("content.ko", f -> f)
							)
							.sort(sortOption)
							.from(finalFrom)
							.size(10)
					, PostDocument.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if(searchResponse.hits().hits().isEmpty()){
			return ElasticVo.of(new ArrayList<>(), from);
		}

		List<PostSearchResult> results = new ArrayList<>();
		from += searchResponse.hits().hits().size();
		searchResponse.hits().hits().forEach(hit -> {
			PostDocument source = hit.source();
			if(source != null){
				PostSearchResult result = new PostSearchResult();

				// basic information
				result.setId(source.getId());
				result.setTitle(source.getTitle());
				result.setContent(source.getContent());
				result.setLanguages(source.getLanguages());

				// pick one highlighted content
				Set<Map.Entry<String, List<String>>> highlightEntries = hit.highlight().entrySet();
				var entry = highlightEntries.iterator().next();
				List<String> highlightedContents = entry.getValue();
				String highlightedContent = highlightedContents.getFirst();
				result.setHighlightedContent(highlightedContent);

				// create time
				Instant instant = Instant.ofEpochMilli(source.getCreateTime());
				LocalDateTime createTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
				result.setCreateTime(createTime);

				results.add(result);
			}
		});

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
