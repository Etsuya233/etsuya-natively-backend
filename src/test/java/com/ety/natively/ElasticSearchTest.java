package com.ety.natively;

import cn.hutool.json.JSONUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.ety.natively.domain.dto.*;
import com.ety.natively.domain.po.Post;
import com.ety.natively.domain.po.PostLanguage;
import com.ety.natively.domain.po.User;
import com.ety.natively.domain.vo.PostSearchResult;
import com.ety.natively.domain.vo.UserSearchResult;
import com.ety.natively.domain.vo.UserVo;
import com.ety.natively.service.IPostLanguageService;
import com.ety.natively.service.IPostService;
import com.ety.natively.service.IPostServiceV2;
import com.ety.natively.service.IUserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest
public class ElasticSearchTest {

	@Autowired
	public ElasticsearchClient client;

	@Autowired
	public IPostServiceV2 postService;

	@Autowired
	public ObjectMapper objectMapper;

	@Autowired
	public IPostLanguageService postLanguageService;

	@Autowired
	public IUserService userService;

	@Test
	public void testSearch() throws IOException {
		SearchRequest request = new SearchRequest.Builder()
				.index("post")
				.query(q -> q
						.match(m -> m.field("content")
								.query("的 地")))
				.highlight(h -> h.fields("content",
						f -> f.preTags("<em>").postTags("</em>"))).build();
		SearchResponse<PostDocument> searchResponse;
		try {
			searchResponse = client.search(request, PostDocument.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		searchResponse.hits().hits().forEach(hit -> {
			PostDocument source = hit.source();
			System.out.println(source);
		});
	}

	@Test
	public void testAddDocument() throws IOException {
		List<Post> posts = postService.lambdaQuery()
				.list();
		Map<Long, List<PostLanguage>> postLangMap = postLanguageService.lambdaQuery()
				.list()
				.stream()
				.collect(Collectors.groupingBy(PostLanguage::getPostId));

		HashMap<Post, String> map = new HashMap<>();

		posts.forEach(post -> {
			String content = post.getContent();
			StringBuilder contentStr = new StringBuilder();
			if (post.getTitle() != null){
				contentStr.append(post.getTitle()).append(" ");
			}
			try {
				List<PostContentTemplate> res = objectMapper.readValue(content, new TypeReference<>() {
				});
				for (PostContentTemplate blockTemplate : res) {
					switch (blockTemplate){
						case PostContentText block -> contentStr.append(block.getValue());
						case PostContentCompare block -> contentStr.append(block.getOldValue()).append(" ").append(block.getNewValue());
						case PostContentImage block -> {
							if(block.getCaption() != null) contentStr.append(block.getCaption());
						}
						case PostContentMarkdown block -> contentStr.append(block.getValue());
						case PostContentVoice block -> {
							if(block.getCaption() != null) contentStr.append(block.getCaption());
						}
						default -> throw new IllegalStateException("Unexpected value: " + blockTemplate);
					}
				}
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
			map.put(post, contentStr.toString());
		});

		List<BulkOperation> operations = new ArrayList<>();
		map.forEach((post, content) -> {
			String postId = post.getId().toString();
			String userId = post.getUserId().toString();
			String title = post.getTitle();
			List<PostLanguage> languages = postLangMap.get(post.getId());
			List<String> langStrs = languages.stream().map(PostLanguage::getLang).toList();
			PostDocument doc = new PostDocument(postId, userId, title, content, langStrs);
			BulkOperation operation = new BulkOperation.Builder()
					.index(in -> in
							.index("post")
							.id(postId)
							.document(doc))
					.build();
			operations.add(operation);
		});

		client.bulk(BulkRequest.of(b -> b.operations(operations)));
	}

	@Test
	public void addUser() throws IOException {
		List<User> list = userService.lambdaQuery().list();

		List<UserSearchResult> users = list.stream().map(user -> {
			UserSearchResult result = new UserSearchResult();
			result.setId(user.getId());
			result.setNickname(user.getNickname());
			result.setUsername(user.getUsername());
			return result;
		}).toList();

		List<BulkOperation> operations = new ArrayList<>();
		users.forEach(user -> {
			BulkOperation operation = new BulkOperation.Builder()
					.index(in -> in
							.index("user")
							.id(user.getId().toString())
							.document(user))
					.build();
			operations.add(operation);
		});

		client.bulk(BulkRequest.of(b -> b.operations(operations)));
	}


	@Data
	public static class MultiLangSearch {
		private String content;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PostDocument {
		private String id;
		private String userId;
		private String title;
		private String content;
		private List<String> lang;
	}

}
