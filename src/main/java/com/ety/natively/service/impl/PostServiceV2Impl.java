package com.ety.natively.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ety.natively.constant.*;
import com.ety.natively.domain.dto.*;
import com.ety.natively.domain.po.*;
import com.ety.natively.domain.vo.*;
import com.ety.natively.enums.ExceptionEnum;
import com.ety.natively.exception.BaseException;
import com.ety.natively.mapper.PostLanguageMapper;
import com.ety.natively.mapper.PostMapper;
import com.ety.natively.service.*;
import com.ety.natively.utils.BaseContext;
import com.ety.natively.utils.I18NUtil;
import com.ety.natively.utils.MinioUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceV2Impl extends ServiceImpl<PostMapper, Post> implements IPostServiceV2 {

	private final StringRedisTemplate redisTemplate;
	private final MinioUtils minioUtils;
	private final RabbitTemplate rabbitTemplate;
	private final ObjectMapper objectMapper;
	private final IPostSummaryService postSummaryService;
	private final ICommentSummaryService commentSummaryService;
	private final IUserService userService;
	private final IVoteService voteService;
	private final ICommentService commentService;
	private final IPostLanguageService postLanguageService;
	private final PostLanguageMapper postLanguageMapper;
	private final IBookmarkService bookmarkService;
	private final PostMapper postMapper;

	@Override
	public String getCreatePostVerificationCode() {
		// generate random code
		String code = UUID.randomUUID().toString();

		// store it in redis
		Long userId = BaseContext.getUserId();
		String key = RedisConstant.POST_VERIFICATION_CODE_PREFIX + code;
		redisTemplate.opsForValue().set(key, userId.toString(), RedisConstant.POST_VERIFICATION_CODE_TTL, TimeUnit.SECONDS);
		return code;
	}

	@Override
	public String uploadPostAttachment(MultipartFile file, String verificationCode) {
		// upload
		String name;
		try {
			name = minioUtils.uploadFile(file, MinioConstant.POST_ATTACHMENT_BUCKET, Map.of("verificationCode", verificationCode));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// TODO auto delete when not used using rabbit

		return name;
	}

	@Override
	public Long createPost(PostCreationDto dto) {
		// verification
		Long userId = BaseContext.getUserId();
		String code = dto.getVerificationCode();
		String key = RedisConstant.POST_VERIFICATION_CODE_PREFIX + code;
		String codeOwner = redisTemplate.opsForValue().get(key);
		if (codeOwner == null || !codeOwner.equals(userId.toString())) {
			throw new BaseException(ExceptionEnum.POST_VERIFICATION_FAILED);
		}
		List<String> languages = dto.getLanguages();

		// info
		Integer type = dto.getType();
		String title = dto.getTitle();
		List<PostContentTemplate> content = dto.getContent();

		// info limitation
		if (type < 0 || type > 2) {
			throw new BaseException(ExceptionEnum.POST_TYPE_NOT_EXIST);
		}
		if (title != null && title.length() > Constant.POST_TITLE_LIMIT) {
			throw new BaseException(ExceptionEnum.POST_TITLE_TOO_LONG);
		}

		// type
		if (dto.getType().equals(PostType.QUESTION) && title == null) {
			throw new BaseException(ExceptionEnum.POST_TITLE_CANNOT_BE_EMPTY);
		}

		// language
		if(CollUtil.isEmpty(languages)) {
			throw new BaseException(ExceptionEnum.POST_LANGUAGE_REQUIRED);
		}
		for (String language : languages) {
			if(!I18NUtil.isSupportedLanguage(language)) {
				throw new BaseException(ExceptionEnum.POST_UNSUPPORTED_LANGUAGE);
			}
		}

		// content limitation and generate preview
		long contentLength = 0, compareCount = 0, imageCount = 0, voiceCount = 0, markdownCount = 0;
		boolean hasMore = false;
		StringBuilder previewText = new StringBuilder(), textContent = new StringBuilder();
		if(title != null){
			textContent.append(title).append("\n");
		}
		String previewImage = null, previewVoice = null;
		List<String> attachments = new ArrayList<>();
		if (content.size() > Constant.POST_BLOCK_LIMIT) {
			throw new BaseException(ExceptionEnum.POST_BLOCK_OVER_LIMIT);
		}
		for (PostContentTemplate block : content) {
			switch (block.getType()) {
				case PostContentType.TEXT: {
					PostContentText textBlock = (PostContentText) block;
					if (textBlock.getValue() == null) {
						textBlock.setValue("");
					}
					contentLength += textBlock.getValue().length();
					if (previewText.length() < Constant.POST_PREVIEW_LIMIT) {
						int appendLength = Math.min(Constant.POST_PREVIEW_LIMIT - previewText.length(), textBlock.getValue().length());
						if(appendLength < textBlock.getValue().length()) {
							hasMore = true;
						}
						previewText.append(textBlock.getValue(), 0, appendLength);
						previewText.append('\n');
						textContent.append(textBlock.getValue());
						textContent.append('\n');
					} else {
						hasMore = true;
					}
					break;
				}
				case PostContentType.COMPARE: {
					PostContentCompare compareBlock = (PostContentCompare) block;
					compareCount++;
					if (compareBlock.getOldValue() == null) {
						compareBlock.setOldValue("");
					}
					if (compareBlock.getNewValue() == null) {
						compareBlock.setNewValue("");
					}
					contentLength += compareBlock.getOldValue().length();
					contentLength += compareBlock.getNewValue().length();
					textContent.append("比较模块: \n\t原始值: ").append(compareBlock.getOldValue());
					textContent.append("\n\t改进值: ").append(compareBlock.getNewValue()).append("\n");
					if (previewText.length() < Constant.POST_PREVIEW_LIMIT) {
						int appendLength = Math.min(Constant.POST_PREVIEW_LIMIT - previewText.length(), compareBlock.getOldValue().length());
						if(appendLength < compareBlock.getOldValue().length()) {
							hasMore = true;
						}
						previewText.append(compareBlock.getOldValue(), 0, appendLength);
						previewText.append(' ');
					} else hasMore = true;
					if (previewText.length() < Constant.POST_PREVIEW_LIMIT) {
						int appendLength = Math.min(Constant.POST_PREVIEW_LIMIT - previewText.length(), compareBlock.getNewValue().length());
						if(appendLength < compareBlock.getNewValue().length()) {
							hasMore = true;
						}
						previewText.append(compareBlock.getNewValue(), 0, appendLength);
						previewText.append('\n');
					} else hasMore = true;
					break;
				}
				case PostContentType.IMAGE: {
					PostContentImage imageBlock = (PostContentImage) block;
					imageCount++;
					Map<String, String> tags = minioUtils.getObjectTags(MinioConstant.POST_ATTACHMENT_BUCKET, imageBlock.getName());
					if (tags == null || !tags.containsKey("verificationCode") || !tags.get("verificationCode").equals(code)) {
						throw new BaseException(ExceptionEnum.POST_VERIFICATION_FAILED);
					}
					attachments.add(imageBlock.getName());
					imageBlock.setName(minioUtils.generateFileUrl(MinioConstant.POST_ATTACHMENT_BUCKET, imageBlock.getName()));
					if (previewImage == null) {
						previewImage = imageBlock.getName();
					}
					break;
				}
				case PostContentType.VOICE: {
					PostContentVoice voiceBlock = (PostContentVoice) block;
					voiceCount++;
					Map<String, String> tags = minioUtils.getObjectTags(MinioConstant.POST_ATTACHMENT_BUCKET, voiceBlock.getName());
					if (tags == null || !tags.containsKey("verificationCode") || !tags.get("verificationCode").equals(code)) {
						throw new BaseException(ExceptionEnum.POST_VERIFICATION_FAILED);
					}
					attachments.add(voiceBlock.getName());
					voiceBlock.setName(minioUtils.generateFileUrl(MinioConstant.POST_ATTACHMENT_BUCKET, voiceBlock.getName()));
					if (previewVoice == null) {
						previewVoice = voiceBlock.getName();
					}
					break;
				}
				case PostContentType.MARKDOWN: {
					PostContentMarkdown markdownBlock = (PostContentMarkdown) block;
					markdownCount++;
					if (markdownBlock.getValue() == null) {
						markdownBlock.setValue("");
					}
					contentLength += markdownBlock.getValue().length();
					textContent.append("Markdown 块: \n").append(markdownBlock.getValue()).append("\n");
					if (previewText.length() < Constant.POST_PREVIEW_LIMIT) {
						int appendLength = Math.min(Constant.POST_PREVIEW_LIMIT - previewText.length(), markdownBlock.getValue().length());
						if(appendLength < markdownBlock.getValue().length()) {
							hasMore = true;
						}
						previewText.append(markdownBlock.getValue(), 0, appendLength);
						previewText.append(' ');
					} else hasMore = true;
					break;
				}
			}
		}
		if (contentLength > Constant.POST_CONTENT_LIMIT) {
			throw new BaseException(ExceptionEnum.POST_CONTENT_OVER_LIMIT);
		}
		if (imageCount > Constant.POST_IMAGE_LIMIT) {
			throw new BaseException(ExceptionEnum.POST_IMAGE_OVER_LIMIT);
		}
		if (voiceCount > Constant.POST_VOICE_LIMIT) {
			throw new BaseException(ExceptionEnum.POST_VOICE_OVER_LIMIT);
		}

		// create post
		Post post = new Post();
		post.setUserId(userId);
		post.setTitle(title);
		post.setType(type);
		post.setPreviewText(previewText.toString());
		post.setPreviewImage(previewImage);
		post.setPreviewVoice(previewVoice);
		post.setPreviewHasMore(hasMore);
		String contentJson;
		try {
			contentJson = objectMapper.writeValueAsString(content);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		post.setContent(contentJson);
		this.save(post);

		// set attachment used
		attachments.forEach(attachment -> {
			minioUtils.setObjectTags(MinioConstant.POST_ATTACHMENT_BUCKET, attachment, Map.of("used", userId.toString()));
		});

		// language
		List<PostLanguage> postLanguages = languages.stream().map(language -> {
			PostLanguage postLanguage = new PostLanguage();
			postLanguage.setPostId(post.getId());
			postLanguage.setLang(language);
			return postLanguage;
		}).toList();
		postLanguageService.saveBatch(postLanguages);

		// summary
		PostSummary summary = new PostSummary();
		summary.setPostId(post.getId());
		postSummaryService.save(summary);

		// ai response
		rabbitTemplate.convertAndSend(MqConstant.EXCHANGE.POST_TOPIC, MqConstant.KEY.POST_AI, new NaviReplyDto(post.getId(), textContent.toString()));

		return post.getId();
	}

	@Override
	public List<PostPreview> getPostRecommendation(Long lastId) {
		// TODO refined recommendation algorithm
//		UserVo user = userService.getUserInfo(BaseContext.getUserId());
//		List<String> languages = user.getLanguages().stream().map(UserLanguageVo::getLanguage).toList();
//		List<PostLanguage> postLanguages = this.postLanguageService.lambdaQuery()
//				.lt(lastId != null, PostLanguage::getPostId, lastId)
//				.in(PostLanguage::getLang, languages)
//				.orderByDesc(PostLanguage::getPostId)
//				.last("limit 8")
//				.list();
//		List<Long> preIds = postLanguages.stream()
//				.map(PostLanguage::getPostId)
//				.distinct()
//				.toList();
//		if(preIds.isEmpty()) {
//			return new ArrayList<>();
//		}
//		Map<Long, Long> postLanguageCountFromUser = postLanguages.stream()
//				.collect(Collectors.groupingBy(PostLanguage::getPostId, Collectors.counting()));
//		List<PostLanguageCountDto> postLanguageCounts = postLanguageMapper.selectPostLanguageCountByIds(preIds);
//		List<Long> postIds = new ArrayList<>();
//		for (PostLanguageCountDto postLanguageCount : postLanguageCounts) {
//			Long postId = postLanguageCount.getPostId();
//			Long languageCount = postLanguageCount.getCount();
//			Long countByUser = postLanguageCountFromUser.get(postId);
//			if(countByUser == null || languageCount == null || countByUser < languageCount) {
//				continue;
//			}
//			postIds.add(postId);
//		}

		Long userId = BaseContext.getUserId();
		List<Long> postIds = postMapper.getRecommendedPostId(userId, lastId);

		List<Post> posts = this.lambdaQuery()
				.in(Post::getId, postIds)
				.list();
		if(posts.isEmpty()) {
			return new ArrayList<>();
		}
		posts.sort(Post.compareByIdDesc);

		return getPostPreview(posts);
	}

	@Override
	public List<PostPreview> getPostByFollowing(Long lastId) {
		Long userId = BaseContext.getUserId();

		List<Post> posts = this.getBaseMapper().getUserFollowingFeed(userId, lastId);

		return getPostPreview(posts);
	}

	@Override
	public List<PostPreview> getPostTrending(Integer rank) {
		if (Constant.POST_TRENDING_RECORDING.get()) {
			return new ArrayList<>();
		}

		// get trending post ids
		int size = Constant.POST_TRENDING_ID_LIST.size();
		if(size == 0){
			return new ArrayList<>();
		}
		int l = rank - 1, r = Math.min(size, rank + 5);
		List<Long> ids = Constant.POST_TRENDING_ID_LIST.subList(l, r);

		if(ids.isEmpty()){
			return new ArrayList<>();
		}

		// get trending posts
		List<Post> posts = this.lambdaQuery()
				.in(Post::getId, ids)
				.list();

		// get sorted trending posts
		Map<Long, Post> postMap = posts.stream().collect(Collectors.toMap(Post::getId, Function.identity()));
		List<Post> postSorted = new ArrayList<>();
		for(Long id: ids){
			postSorted.add(postMap.get(id));
		}

		return this.getPostPreview(postSorted);
	}

	@Override
	@Transactional
	public VoteCompleteVo vote(VoteDto dto) {
		Long userId = BaseContext.getUserId();

		Vote vote = this.voteService.lambdaQuery()
				.eq(Vote::getUserId, userId)
				.eq(dto.getPost(), Vote::getPostId, dto.getId())
				.eq(!dto.getPost(), Vote::getCommentId, dto.getId())
				.one();

		VoteSummary summary;
		if(dto.getPost()){
			summary = this.postSummaryService.lambdaQuery()
					.eq(PostSummary::getPostId, dto.getId())
					.one();
		} else {
			summary = this.commentSummaryService.lambdaQuery()
					.eq(CommentSummary::getCommentId, dto.getId())
					.one();
		}
		if(summary == null) {
			throw new BaseException(ExceptionEnum.UNKNOWN_ERROR);
		}

		// vote the same before
		if((dto.getType() == 0 && vote == null) ||
				(dto.getType() == 1 && vote != null && vote.getType()) ||
				(dto.getType() == -1 && vote != null && !vote.getType())){
			throw new BaseException(ExceptionEnum.POST_CANNOT_REPEAT_VOTE);
		}

		// delta
		int upvoteDelta = 0, downvoteDelta = 0;
		if(dto.getType() == 1){ // upvote
			if(vote == null){
				upvoteDelta = 1;
			} else if(!vote.getType()){
				upvoteDelta = 1;
				downvoteDelta = -1;
			}
		} else if(dto.getType() == 0){ // do nothing to the post
			if(vote.getType()){
				upvoteDelta = -1;
			} else {
				downvoteDelta = -1;
			}
		} else if(dto.getType() == -1) { // downvote
			if(vote == null){
				downvoteDelta = 1;
			} else if(vote.getType()){
				downvoteDelta = 1;
				upvoteDelta = -1;
			}
		}

		// vote
		if(upvoteDelta != 0){
			if(dto.getPost()){
				this.postSummaryService.lambdaUpdate()
						.eq(PostSummary::getPostId, dto.getId())
						.setIncrBy(PostSummary::getUpvoteCount, upvoteDelta)
						.update();
			} else {
				this.commentSummaryService.lambdaUpdate()
						.eq(CommentSummary::getCommentId, dto.getId())
						.setIncrBy(CommentSummary::getUpvoteCount, upvoteDelta)
						.update();
			}
		}
		if(downvoteDelta != 0){
			if(dto.getPost()){
				this.postSummaryService.lambdaUpdate()
						.eq(PostSummary::getPostId, dto.getId())
						.setIncrBy(PostSummary::getDownvoteCount, downvoteDelta)
						.update();
			} else {
				this.commentSummaryService.lambdaUpdate()
						.eq(CommentSummary::getCommentId, dto.getId())
						.setIncrBy(CommentSummary::getDownvoteCount, downvoteDelta)
						.update();
			}
		}

		if(dto.getType() == 0){
			if(vote != null){
				this.voteService.removeById(vote.getId());
			}
		} else {
			if(vote != null){
				vote.setType(dto.getType() == 1);
				this.voteService.updateById(vote);
			} else {
				Vote vo = new Vote();
				vo.setUserId(userId);
				if(dto.getPost()){
					vo.setPostId(dto.getId());
				} else {
					vo.setCommentId(dto.getId());
				}
				vo.setType(dto.getType() == 1);
				this.voteService.save(vo);
			}
		}

		// score todo move the mq
		if(dto.getPost() && dto.getType() == 1){
			redisTemplate.opsForZSet().incrementScore(RedisConstant.POST_SCORE, dto.getId().toString(), 1);
		} else if(!dto.getPost() && dto.getType() == 1) {
			Comment comment = this.commentService.lambdaQuery()
					.eq(Comment::getId, dto.getId())
					.select(Comment::getPostId)
					.one();
			Long postId = comment.getPostId();
			redisTemplate.opsForZSet().incrementScore(RedisConstant.POST_SCORE, postId.toString(), 1);
		}

		// ret
		VoteCompleteVo ret = new VoteCompleteVo();
		ret.setUpvoteCount(summary.getUpvoteCount() + upvoteDelta);
		ret.setDownvoteCount(summary.getDownvoteCount() + downvoteDelta);
		ret.setVote(dto.getType());

		return ret;
	}

	// TODO cascade counting (using mq), do it when we do system optimization
	@Override
	public CommentVoV2 createComment(Long postId, Long parentId, String content,
									 MultipartFile image, MultipartFile voice, String compare) {
		Long userId = BaseContext.getUserId();

		Comment comment = new Comment();

		int contentLength = 0;

		comment.setPostId(postId);
		comment.setParentId(parentId);
		comment.setUserId(userId);
		comment.setContent(content);
		contentLength += content.length();

		if(contentLength > Constant.COMMENT_LENGTH_LIMIT){
			throw new BaseException(ExceptionEnum.POST_COMMENT_LENGTH_LIMIT);
		}

		// upload file
		try {
			if(image != null){
				String filename = minioUtils.uploadFile(image, MinioConstant.POST_ATTACHMENT_BUCKET);
				String imageUrl = minioUtils.generateFileUrl(MinioConstant.POST_ATTACHMENT_BUCKET, filename);
				comment.setImage(imageUrl);
			}
		} catch (Exception e) {
			throw new BaseException(ExceptionEnum.POST_IMAGE_UPLOAD_ERROR);
		}
		try {
			if(voice != null){
				String filename = minioUtils.uploadFile(voice, MinioConstant.POST_ATTACHMENT_BUCKET);
				String voiceUrl = minioUtils.generateFileUrl(MinioConstant.POST_ATTACHMENT_BUCKET, filename);
				comment.setVoice(voiceUrl);
			}
		} catch (Exception e) {
			throw new BaseException(ExceptionEnum.POST_VOICE_UPLOAD_ERROR);
		}

		// parse compare
		try {
			if(compare != null && !compare.isEmpty()){
				PostContentCompare compareParsed = JSONUtil.toBean(compare, PostContentCompare.class);
				if (compareParsed.getOldValue() != null) {
					contentLength += compareParsed.getOldValue().length();
				}
				if (compareParsed.getNewValue() != null) {
					contentLength += compareParsed.getNewValue().length();
				}
				comment.setCompare(compare);
			}
		} catch (Exception e) {
			throw new BaseException(ExceptionEnum.POST_PARSE_FAILED);
		}
		if(contentLength > Constant.COMMENT_LENGTH_LIMIT){
			throw new BaseException(ExceptionEnum.POST_COMMENT_LENGTH_LIMIT);
		}

		// insert
		commentService.save(comment);

		// score
		redisTemplate.opsForZSet().incrementScore(RedisConstant.POST_SCORE, postId.toString(), 3);

		// summary
		CommentSummary summary = new CommentSummary();
		summary.setCommentId(comment.getId());
		this.commentSummaryService.save(summary);

		// comment count mq
		rabbitTemplate.convertAndSend(MqConstant.EXCHANGE.POST_TOPIC, MqConstant.KEY.COMMENT_SCORE, parentId);

		// return
		Comment commentInDb = this.commentService.getById(comment.getId());
		CommentVoV2 vo = new CommentVoV2();
		BeanUtils.copyProperties(commentInDb, vo);

		return vo;
	}

	@Override
	public List<CommentVoV2> getCommentList(Boolean post, Long id, Long lastId) {
		List<Comment> comments = this.commentService.lambdaQuery()
				.eq(Comment::getParentId, id)
				.gt(lastId != null, Comment::getId, lastId)
				.orderByAsc(Comment::getId)
				.last("limit 10")
				.list();
		if(comments == null || comments.isEmpty()){
			return new ArrayList<>();
		}

		List<Long> commentIds = comments.stream()
				.map(Comment::getId)
				.toList();

		// summary
		Map<Long, CommentSummary> summaryMap = commentSummaryService.lambdaQuery()
				.in(CommentSummary::getCommentId, commentIds)
				.list()
				.stream()
				.collect(Collectors.toMap(CommentSummary::getCommentId, Function.identity()));

		// vote
		List<Vote> voteList = voteService.lambdaQuery()
				.in(Vote::getCommentId, commentIds)
				.list();
		Map<Long, Vote> voteMap = voteList
				.stream()
				.collect(Collectors.toMap(Vote::getCommentId, Function.identity()));

		// user
		List<Long> userIds = comments.stream().map(Comment::getUserId).toList();
		List<UserVo> users = userService.getUserByIds(userIds);
		Map<Long, UserVo> userMap = users.stream().collect(Collectors.toMap(UserVo::getId, Function.identity()));

		// pack
		List<CommentVoV2> list = comments.stream().sorted(Comment.sortByIdAsc).map(comment -> {
			CommentVoV2 vo = new CommentVoV2();
			vo.setId(comment.getId());
			vo.setParentId(id);
			vo.setCreateTime(comment.getCreateTime());
			vo.setPostId(comment.getPostId());

			vo.setContent(comment.getContent());
			vo.setImage(comment.getImage());
			vo.setVoice(comment.getVoice());
			vo.setCompare(comment.getCompare());

			UserVo user = userMap.get(comment.getUserId());
			if (user != null) {
				vo.setUserId(user.getId());
				vo.setNickname(user.getNickname());
				vo.setUserLanguages(user.getLanguages());
				vo.setAvatar(user.getAvatar());
			}

			CommentSummary summary = summaryMap.get(comment.getId());
			if (summary != null) {
				vo.setUpvote(summary.getUpvoteCount());
				vo.setDownvote(summary.getDownvoteCount());
				vo.setCommentCount(summary.getCommentCount());
			} else {
				// TODO
				vo.setUpvote(0L);
				vo.setDownvote(0L);
				vo.setCommentCount(0L);
			}

			Vote vote = voteMap.get(comment.getId());
			if (vote != null) {
				vo.setVote(vote.getType() ? 1 : -1);
			} else {
				vo.setVote(0);
			}

			return vo;
		}).filter(comment -> comment.getUserId() != null).toList();

		I18NUtil.adjustTimezone(list, "createTime");

		return list;
	}

	/**
	 * this will convert timezone.
	 * @param id id
	 * @return post
	 */
	@Override
	public PostVo getPostById(Long id) {
		// TODO Authentication

		// post
		Post post = this.getById(id);
		if(post == null){
			throw new BaseException(ExceptionEnum.POST_NOT_EXIST);
		}

		// user
		UserVo user = userService.getUserInfo(post.getUserId());
		if(user == null){
			throw new BaseException(ExceptionEnum.POST_NOT_EXIST);
		}

		// summary
		PostSummary summary = postSummaryService.lambdaQuery()
				.eq(PostSummary::getPostId, id)
				.one();
		if(summary == null){
			// todo
			summary = new PostSummary(id, 0L, 0L, 0L);
		}

		// pack
		PostVo vo = new PostVo();
		vo.setId(post.getId());
		vo.setUserId(user.getId());
		vo.setType(post.getType());
		vo.setTitle(post.getTitle());
		vo.setContent(post.getContent());
		vo.setPreviewImage(post.getPreviewImage());
		vo.setCreateTime(I18NUtil.adjustTimezone(post.getCreateTime())); //timezone

		vo.setNickname(user.getNickname());
		vo.setAvatar(user.getAvatar());
		vo.setUserLanguages(user.getLanguages());

		vo.setUpvote(summary.getUpvoteCount());
		vo.setDownvote(summary.getDownvoteCount());
		vo.setCommentCount(summary.getCommentCount());

		return vo;
	}

	/**
	 * this will convert timezone.
	 * @param id id
	 * @return comments
	 */
	@Override
	public CommentVoV2 getCommentById(Long id) {
		// TODO authentication

		// comment
		Comment comment = commentService.getById(id);
		if(comment == null){
			throw new BaseException(ExceptionEnum.POST_COMMENT_NOT_EXIST);
		}

		// user
		UserVo user = userService.getUserInfo(comment.getUserId());
		if(user == null){
			throw new BaseException(ExceptionEnum.POST_NOT_EXIST);
		}

		// summary
		CommentSummary summary = commentSummaryService.lambdaQuery()
				.eq(CommentSummary::getCommentId, id)
				.one();
		if(summary == null){
			// todo
			summary = new CommentSummary(id, 0L, 0L, 0L);
		}

		// pack
		CommentVoV2 vo = new CommentVoV2();
		vo.setId(comment.getId());
		vo.setUserId(user.getId());
		vo.setContent(comment.getContent());
		vo.setParentId(comment.getParentId());
		vo.setImage(comment.getImage());
		vo.setVoice(comment.getVoice());
		vo.setCreateTime(I18NUtil.adjustTimezone(comment.getCreateTime()));

		vo.setNickname(user.getNickname());
		vo.setAvatar(user.getAvatar());
		vo.setUserLanguages(user.getLanguages());

		vo.setUpvote(summary.getUpvoteCount());
		vo.setDownvote(summary.getDownvoteCount());
		vo.setCommentCount(summary.getCommentCount());

		return vo;
	}

	@Override
	public void createBookmark(BookmarkCreateDto dto) {
		Integer type = dto.getType();
		Long referenceId = dto.getReferenceId();
		String content = dto.getContent();
		String note = dto.getNote();

		// validation
		if(type == null || type < 0 || type >= 3){
			throw new BaseException(ExceptionEnum.UNKNOWN_ERROR);
		}
		if(content != null && content.length() > Constant.BOOKMARK_CONTENT_LIMIT){
			throw new BaseException(ExceptionEnum.POST_BOOKMARK_CONTENT_LIMIT);
		}
		if(note != null && note.length() > Constant.BOOKMARK_NOTE_LIMIT){
			throw new BaseException(ExceptionEnum.POST_BOOKMARK_NOTE_LIMIT);
		}

		// pack
		Long userId = BaseContext.getUserId();
		Bookmark bookmark = new Bookmark();
		bookmark.setType(type);
		bookmark.setUserId(userId);
		bookmark.setNote(note);
		if(type.equals(BookmarkType.TEXT)){
			bookmark.setContent(content);
		} else if(type.equals(BookmarkType.POST)){
			Long count = this.lambdaQuery()
					.eq(Post::getId, referenceId)
					.count();
			if(count > 0){
				bookmark.setReferenceId(referenceId);
			}
		} else {
			Long count = this.commentService.lambdaQuery()
					.eq(Comment::getId, referenceId)
					.count();
			if(count > 0){
				bookmark.setReferenceId(referenceId);
			}
		}

		bookmarkService.save(bookmark);
	}

	@Override
	public void deleteBookmark(Long id) {
		Long userId = BaseContext.getUserId();
		Bookmark bookmark = this.bookmarkService.getById(id);
		if(bookmark == null || !bookmark.getUserId().equals(userId)){
			throw new BaseException(ExceptionEnum.POST_BOOKMARK_NOT_EXIST);
		}
		bookmarkService.removeById(id);
	}

	/**
	 * this will convert timezone.
	 * @param lastId lastId
	 * @return bookmarks
	 */
	@Override
	public List<BookmarkVo> getBookmark(Long lastId) {
		Long userId = BaseContext.getUserId();

		// query db
		List<Bookmark> bookmarks = this.bookmarkService.lambdaQuery()
				.eq(Bookmark::getUserId, userId)
				.lt(lastId != null, Bookmark::getId, lastId)
				.last("limit 10")
				.list();

		Map<Integer, List<Bookmark>> bookmarkClassified = bookmarks
				.stream()
				.collect(Collectors.groupingBy(Bookmark::getType));
		// post
		List<Bookmark> bookmarksOfPost = bookmarkClassified.get(BookmarkType.POST);
		List<Long> postIds = bookmarksOfPost.stream()
				.map(Bookmark::getReferenceId)
				.toList();
		List<Post> posts = this.lambdaQuery()
				.in(Post::getId, postIds)
				.list();
		Map<Long, Post> postMap = posts.stream()
				.collect(Collectors.toMap(Post::getId, Function.identity()));
		List<Long> userIds = new ArrayList<>(posts.stream().map(Post::getUserId).toList());
		// comment
		List<Bookmark> bookmarksOfComment = bookmarkClassified.get(BookmarkType.COMMENT);
		List<Long> commentIds = bookmarksOfComment.stream()
				.map(Bookmark::getReferenceId)
				.toList();
		List<Comment> comments = this.commentService.lambdaQuery()
				.in(Comment::getId, commentIds)
				.list();
		Map<Long, Comment> commentMap = comments.stream()
				.collect(Collectors.toMap(Comment::getId, Function.identity()));
		userIds.addAll(comments.stream()
				.map(Comment::getUserId)
				.toList());
		// user
		List<UserVo> users = userService.getUserByIds(userIds);
		Map<Long, UserVo> userMap = users.stream()
				.collect(Collectors.toMap(UserVo::getId, Function.identity()));

		// pack
		List<BookmarkVo> list = bookmarks.stream().map(bookmark -> {
			BookmarkVo vo = new BookmarkVo();
			BeanUtils.copyProperties(bookmark, vo);

			Long referenceUserId = null;
			if (bookmark.getType().equals(BookmarkType.POST)) {
				Long postId = bookmark.getReferenceId();
				Post post = postMap.get(postId);
				if (post == null) {
					return null;
				}
				referenceUserId = post.getUserId();
				vo.setUserId(post.getUserId());
				vo.setContent(post.getPreviewText());
				vo.setContentHasMore(post.getPreviewHasMore());
			} else if (bookmark.getType().equals(BookmarkType.COMMENT)) {
				Long referenceId = bookmark.getReferenceId();
				Comment comment = commentMap.get(referenceId);
				if (comment == null) {
					return null;
				}
				referenceUserId = comment.getUserId();
				vo.setContent(comment.getContent());
			}

			if (!bookmark.getType().equals(BookmarkType.TEXT)) {
				UserVo user = userMap.get(referenceUserId);
				if (user == null) {
					return null;
				}
				vo.setUserId(user.getId());
				vo.setAvatar(user.getAvatar());
				vo.setUserLanguages(user.getLanguages());
				vo.setNickname(user.getNickname());
			}

			return vo;
		}).filter(Objects::nonNull).toList();

		I18NUtil.adjustCreateTimeTimezone(list);

		return list;
	}

	/**
	 * this will convert timezone.
	 * @param userId userId
	 * @param lastId lastId
	 * @return posts
	 */
	@Override
	public List<PostPreview> getUserPost(Long userId, Long lastId) {
		// todo validation
		List<Post> posts = this.lambdaQuery()
				.eq(Post::getUserId, userId)
				.lt(lastId != null, Post::getId, lastId)
				.last("limit 10")
				.list();
		return getPostPreview(posts);
	}

	@Override
	public void updateBookmark(BookmarkUpdateDto dto) {
		Long id = dto.getId();
		String content = dto.getContent();
		String note = dto.getNote();

		Long userId = BaseContext.getUserId();

		Bookmark bookmark = this.bookmarkService.lambdaQuery()
				.eq(Bookmark::getUserId, userId)
				.eq(Bookmark::getId, id)
				.one();
		if(bookmark == null){
			throw new BaseException(ExceptionEnum.POST_BOOKMARK_NOT_EXIST);
		}

		if(content != null && content.length() > Constant.BOOKMARK_CONTENT_LIMIT){
			throw new BaseException(ExceptionEnum.POST_CONTENT_OVER_LIMIT);
		}
		if(note != null && note.length() > Constant.BOOKMARK_NOTE_LIMIT){
			throw new BaseException(ExceptionEnum.POST_CONTENT_OVER_LIMIT);
		}

		this.bookmarkService.lambdaUpdate()
				.eq(Bookmark::getId, id)
				.set(bookmark.getType().equals(BookmarkType.TEXT), Bookmark::getContent, content)
				.set(Bookmark::getNote, note)
				.update();
	}

	/**
	 * This will convert timezone.
	 * @param posts post
	 * @return posts' preview
	 */
	@Override
	public List<PostPreview> getPostPreview(List<Post> posts) {
		if (CollUtil.isEmpty(posts)) {
			return new ArrayList<>();
		}

		List<Long> postIds = posts.stream()
				.map(Post::getId)
				.toList();
		List<Long> userIds = posts.stream()
				.map(Post::getUserId)
				.toList();

		// post summary
		Map<Long, PostSummary> postSummaryMap = this.postSummaryService.lambdaQuery()
				.in(PostSummary::getPostId, postIds)
				.list()
				.stream()
				.collect(Collectors.toMap(PostSummary::getPostId, Function.identity()));

		// users
		Map<Long, UserVo> userMap = this.userService.getUserByIds(userIds)
				.stream()
				.collect(Collectors.toMap(UserVo::getId, Function.identity()));

		// whether I vote or not
		Long userId = BaseContext.getUserId();
		Map<Long, Vote> userVoteMap = voteService.lambdaQuery()
				.eq(Vote::getUserId, userId)
				.in(Vote::getPostId, postIds)
				.list()
				.stream()
				.collect(Collectors.toMap(Vote::getPostId, Function.identity()));

		// pack
		List<PostPreview> list = posts.stream().sorted(Post.compareByIdDesc).map(post -> { // todo sort before map!!!
			if (post == null) {
				return null;
			}

			PostPreview preview = new PostPreview();
			preview.setId(post.getId());
			preview.setType(post.getType());
			preview.setTitle(post.getTitle());
			preview.setContent(post.getPreviewText());
			preview.setImage(post.getPreviewImage());
			preview.setVoice(post.getPreviewVoice());
			preview.setHasMore(post.getPreviewHasMore());
			preview.setCreateTime(post.getCreateTime());

			// user
			UserVo user = userMap.get(post.getUserId());
			if (user != null) {
				preview.setUserId(post.getUserId());
				preview.setNickname(user.getNickname());
				preview.setAvatar(user.getAvatar());
				preview.setUserLanguages(user.getLanguages());
			}

			// summary
			PostSummary summary = postSummaryMap.get(post.getId());
			if (summary != null) {
				preview.setUpvote(summary.getUpvoteCount());
				preview.setDownvote(summary.getDownvoteCount());
				preview.setCommentCount(summary.getCommentCount());
			} else {
				preview.setUpvote(0L);
			}

			// vote
			Vote vote = userVoteMap.get(post.getId());
			if (vote != null) {
				preview.setVote(vote.getType() ? 1 : -1);
			} else {
				preview.setVote(0);
			}

			return preview;
		}).filter(post -> post == null || post.getUserId() != null).toList();

		I18NUtil.adjustTimezone(list, "createTime");

		return list;
	}

	@Override
	public void deletePost(DeleteDto dto) {
		Long userId = BaseContext.getUserId();
		Long postId = dto.getId();
		Post post = this.getById(postId);
		if(post == null){
			throw new BaseException(ExceptionEnum.POST_NOT_EXIST);
		} else if(!post.getUserId().equals(userId)){
			throw new BaseException(ExceptionEnum.POST_NOT_YOURS);
		}

		this.removeById(postId);
		rabbitTemplate.convertAndSend(MqConstant.EXCHANGE.POST_TOPIC, MqConstant.KEY.POST_DELETE, postId);
	}

	@Override
	@Transactional
	public void deleteComment(DeleteDto dto) {
		Long userId = BaseContext.getUserId();
		Long commentId = dto.getId();
		Comment comment = this.commentService.getById(commentId);
		if(comment == null){
			throw new BaseException(ExceptionEnum.POST_COMMENT_NOT_EXIST);
		} else if(!comment.getUserId().equals(userId)){
			throw new BaseException(ExceptionEnum.POST_COMMENT_NOT_YOURS);
		}

		this.commentService.removeById(commentId);
		rabbitTemplate.convertAndSend(MqConstant.EXCHANGE.POST_TOPIC, MqConstant.KEY.COMMENT_DELETE, commentId);
	}
}
