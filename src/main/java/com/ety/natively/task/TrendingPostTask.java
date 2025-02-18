package com.ety.natively.task;

import com.ety.natively.constant.Constant;
import com.ety.natively.constant.RedisConstant;
import com.ety.natively.domain.po.Post;
import com.ety.natively.domain.vo.PostPreview;
import com.ety.natively.service.IPostService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrendingPostTask {

	private final StringRedisTemplate redisTemplate;
	private final IPostService postService;

	@PostConstruct
	public void init(){
		Constant.POST_TRENDING_RECORDING.compareAndSet(false, true);

		List<String> postIdsStr = redisTemplate.opsForList().range(RedisConstant.POST_TRENDING_ID_LIST, 0, 99);

		try {
			List<Long> postIds = postIdsStr
					.stream()
					.map(Long::parseLong)
					.toList();
			Constant.POST_TRENDING_ID_LIST.addAll(postIds);
			Constant.POST_TRENDING_ID_SET.addAll(postIds);
		} catch (Exception e) {
			log.error("Failed to init trending post!!!", e);
		} finally {
			Constant.POST_TRENDING_RECORDING.compareAndSet(true, false);
		}

	}

	@Scheduled(cron = "0 0 0 * * *")
	public void recordTrendingPost() {
		Constant.POST_TRENDING_RECORDING.compareAndSet(false, true);

		try {
			// acquire list
			Set<ZSetOperations.TypedTuple<String>> res = redisTemplate.opsForZSet()
					.reverseRangeByScoreWithScores(RedisConstant.POST_SCORE, 0, Integer.MAX_VALUE, 0, 100);

			List<Long[]> list = new ArrayList<>();
			for (ZSetOperations.TypedTuple<String> tuple: res) {
				String postId = tuple.getValue();
				Double score = tuple.getScore();
				list.add(new Long[]{ Long.parseLong(postId), score.longValue()});
			}

			// high to low
			List<Long> postIdsSorted = list.stream().sorted((o1, o2) -> {
				if (!o1[1].equals(o2[1])) {
					return o2[1].compareTo(o1[1]);
				} else {
					return o1[0].compareTo(o2[0]);
				}
			}).map(o -> o[0]).toList();

			// save ids to java
			Constant.POST_TRENDING_ID_LIST.clear();
			Constant.POST_TRENDING_ID_LIST.addAll(postIdsSorted);
			Constant.POST_TRENDING_ID_SET.clear();
			Constant.POST_TRENDING_ID_SET.addAll(postIdsSorted);

			// save ids to redis
			redisTemplate.delete(RedisConstant.POST_TRENDING_ID_SET);
			String[] postIdsStr = postIdsSorted
					.stream()
					.map(Object::toString)
					.toArray(String[]::new);
			redisTemplate.opsForSet().add(RedisConstant.POST_TRENDING_ID_SET, postIdsStr);
			redisTemplate.delete(RedisConstant.POST_TRENDING_ID_LIST);
			redisTemplate.opsForList().rightPushAll(RedisConstant.POST_TRENDING_ID_LIST, postIdsStr);

			// remove score
			redisTemplate.delete(RedisConstant.POST_SCORE);

			// save preview to java
			List<Post> posts = postService.lambdaQuery()
					.in(Post::getId, postIdsSorted)
					.list();
			Map<Long, Post> postMap = posts.stream()
					.collect(Collectors.toMap(Post::getId, Function.identity()));
			List<Post> postListSorted = new ArrayList<>();
			for(Long postId: postIdsSorted){
				Post post = postMap.get(postId);
				if(post == null){
					continue;
				}
				postListSorted.add(post);
			}
			List<PostPreview> postPreview = postService.getPostPreview(postListSorted);
			Constant.POST_TRENDING_ID_SET.clear();
			Constant.POST_TRENDING_PREVIEW_LIST.addAll(postPreview);

		} catch (Exception e){
			Constant.POST_TRENDING_ID_LIST.clear();
			Constant.POST_TRENDING_ID_SET.clear();
			Constant.POST_TRENDING_ID_SET.clear();
			log.error("Failed to record trending post!!!", e);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		} finally {
			Constant.POST_TRENDING_RECORDING.compareAndSet(true, false);
		}
	}

}
