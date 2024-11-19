package com.ety.natively.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ety.natively.constant.MinioConstant;
import com.ety.natively.constant.PostType;
import com.ety.natively.domain.dto.CommentDto;
import com.ety.natively.domain.dto.PostDto;
import com.ety.natively.domain.dto.VoteDto;
import com.ety.natively.domain.po.*;
import com.ety.natively.domain.vo.AttachmentVo;
import com.ety.natively.domain.vo.CommentVo;
import com.ety.natively.domain.vo.PostInfoVo;
import com.ety.natively.domain.vo.PostVo;
import com.ety.natively.enums.ExceptionEnum;
import com.ety.natively.exception.BaseException;
import com.ety.natively.mapper.PostMapper;
import com.ety.natively.properties.MinioProperties;
import com.ety.natively.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ety.natively.utils.BaseContext;
import com.ety.natively.utils.MinioUtils;
import com.ety.natively.utils.TimezoneUtil;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-11-10
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements IPostService {

	private final IPostSummaryService postSummaryService;
	private final ICommentService commentService;
	private final ICommentSummaryService commentSummaryService;
	private final IVoteService voteService;
	private final IUserService userService;
	private final MinioProperties minioProperties;
	private final MinioClient minioClient;
	private final IAttachmentService attachmentService;
	private final MinioUtils minioUtils;

	@Override
	public Long createPost(PostDto dto) {
		Long userId = BaseContext.getUserId();
		//TODO 标题限制255字符 文章2^16-1 字数检查这样子写不对吧
		//检查内容
		if(dto.getType() == PostType.QUESTION){
			if(StrUtil.isEmpty(dto.getTitle())){
				throw new BaseException(ExceptionEnum.POST_TITLE_EMPTY);
			}
		}
		if(dto.getTitle().length() > 255){
			throw new BaseException(ExceptionEnum.POST_TITLE_RULE);
		}
		if(dto.getContent().length() > 65535){
			throw new BaseException(ExceptionEnum.POST_TITLE_RULE);
		}
		//保存内容
		Post post = new Post();
		post.setTitle(dto.getTitle());
		post.setContent(dto.getContent());
		post.setType(dto.getType());
		post.setUserId(userId);
		this.save(post);
		PostSummary postSummary = new PostSummary();
		postSummary.setPostId(post.getId());
		postSummaryService.save(postSummary);
		return post.getId();
	}

	@Override
	public List<PostInfoVo> getRecommendation(Long lastId) {
		//TODO 推荐算法还没写 缓存也没写
		List<Post> posts = this.lambdaQuery()
				.orderByDesc(Post::getId)
				.lt(lastId != null, Post::getId, lastId)
				.last("limit 10")
				.list();
		if(CollUtil.isEmpty(posts)) return List.of();
		List<Long> postIds = posts.stream()
				.map(Post::getId)
				.toList();
		Map<Long, PostSummary> postSummaryMap = postSummaryService.lambdaQuery()
				.in(PostSummary::getPostId, postIds)
				.list()
				.stream()
				.collect(Collectors.toMap(PostSummary::getPostId, Function.identity()));
		Map<Long, List<Attachment>> attachmentMap = attachmentService.lambdaQuery()
				.in(Attachment::getPostId, postIds)
				.list()
				.stream()
				.collect(Collectors.groupingBy(Attachment::getPostId));
		List<Long> userIds = posts.stream()
				.map(Post::getUserId)
				.toList();
		Map<Long, User> userMap = userService.getUserByIds(userIds)
				.stream()
				.collect(Collectors.toMap(User::getId, Function.identity()));
		TimezoneUtil.adjustCreateTimeTimezone(posts);
		Long userId = BaseContext.getUserId();
		Map<Long, Vote> voteMap = (userId != null)?
				voteService.lambdaQuery()
					.in(Vote::getPostId, postIds)
					.eq(Vote::getUserId, userId)
					.list()
					.stream()
					.collect(Collectors.toMap(Vote::getPostId, Function.identity()))
				: new HashMap<>();
		return posts.stream().map(post -> {

			//basic info
			PostInfoVo dto = new PostInfoVo();
			dto.setId(post.getId());
			dto.setTitle(post.getTitle());
			if(post.getContent().length() > 200){
				dto.setContentHasMore(true);
			}
			dto.setContent(post.getContent().substring(0, Math.min(200, post.getContent().length())));
			dto.setType(post.getType());

			//user info
			if(userMap.containsKey(post.getUserId())){
				dto.setUserId(post.getUserId());
				dto.setNickname(userMap.get(post.getUserId()).getNickname());
				dto.setAvatar(userMap.get(post.getUserId()).getAvatar());
			} else {
				dto.setUserId(null);
			}

			//summary info
			dto.setUpvote(postSummaryMap.get(post.getId()).getUpvoteCount());
			dto.setDownvote(postSummaryMap.get(post.getId()).getDownvoteCount());
			dto.setCommentCount(postSummaryMap.get(post.getId()).getCommentCount());
			dto.setCreateTime(post.getCreateTime());

			//vote
			Vote vote = voteMap.get(post.getId());
			if(vote != null){
				dto.setVote(vote.getType());
			};

			//attachments
			List<Attachment> attachments = attachmentMap.get(post.getId());
			List<AttachmentVo> images = new ArrayList<>();
			if(attachments != null){
				attachments.forEach(attachment -> {
					AttachmentVo attachmentVo = new AttachmentVo();
					attachmentVo.setName(attachment.getNo() + getFileExtension(attachment.getPath()));
					attachmentVo.setSize(attachment.getSize());
					if(attachment.getType() == 1){
						attachmentVo.setUrl(minioUtils.generateFileUrl(MinioConstant.POST_IMAGES_BUCKET, attachment.getPath()));
						images.add(attachmentVo);
					} else {
						String url = minioUtils.generateFileUrl(MinioConstant.POST_VOICES_BUCKET, attachment.getPath());
						attachmentVo.setUrl(url);
						dto.setVoice(attachmentVo);
					}
				});
			}
			dto.setImages(images);

			return dto;
		}).filter(p -> p.getUserId() != null).toList();
	}

	@Override
	public PostVo getPost(Long id) {
		Post post = this.getById(id);
		if(post == null){
			throw new BaseException(ExceptionEnum.POST_NOT_EXIST);
		}

		//basic info
		Long userId = post.getUserId();
		User user = userService.getUserInfo(userId);
		PostSummary summary = postSummaryService.getById(post.getId());
		PostVo postVo = new PostVo();
		postVo.setId(post.getId());
		postVo.setTitle(post.getTitle());
		postVo.setContent(post.getContent());
		postVo.setType(post.getType());
		postVo.setUserId(userId);
		postVo.setNickname(user.getNickname());
		postVo.setAvatar(user.getAvatar());
		postVo.setUpvote(summary.getUpvoteCount());
		postVo.setDownvote(summary.getDownvoteCount());
		postVo.setCommentCount(summary.getCommentCount());
		TimezoneUtil.adjustCreateTimeTimezone(post);
		postVo.setCreateTime(post.getCreateTime());

		//vote TODO 考虑userId为空
		Vote vote = voteService.lambdaQuery()
				.eq(Vote::getPostId, post.getId())
				.eq(Vote::getUserId, userId)
				.one();
		if(vote != null){
			postVo.setVote(vote.getType());
		}

		//attachment
		List<Attachment> attachments = attachmentService.lambdaQuery()
				.eq(Attachment::getPostId, post.getId())
				.list();
		List<AttachmentVo> images = new ArrayList<>();
		if(attachments != null){
			attachments.forEach(attachment -> {
				AttachmentVo attachmentVo = new AttachmentVo();
				attachmentVo.setName(attachment.getNo() + getFileExtension(attachment.getPath()));
				attachmentVo.setSize(attachment.getSize());
				if(attachment.getType() == 1){
					attachmentVo.setUrl(minioUtils.generateFileUrl(MinioConstant.POST_IMAGES_BUCKET, attachment.getPath()));
					images.add(attachmentVo);
				} else {
					String url = minioUtils.generateFileUrl(MinioConstant.POST_VOICES_BUCKET, attachment.getPath());
					attachmentVo.setUrl(url);
					postVo.setVoice(attachmentVo);
				}
			});
		}
		postVo.setImages(images);
		return postVo;
	}

	@Override
	public List<CommentVo> getPostComment(Long id, Long lastId) {
		List<Comment> comments = commentService.lambdaQuery()
				.eq(Comment::getPostId, id)
				.isNull(Comment::getParentId)
				.gt(lastId != null, Comment::getId, lastId)
				.orderByAsc(Comment::getId)
				.last("limit 10")
				.list();
		if(CollUtil.isEmpty(comments)) return List.of();
		TimezoneUtil.adjustCreateTimeTimezone(comments);
		List<Long> ids = comments.stream()
				.map(Comment::getId)
				.toList();
		Map<Long, CommentSummary> summaryMap = commentSummaryService.lambdaQuery()
				.in(CommentSummary::getCommentId, ids)
				.list()
				.stream()
				.collect(Collectors.toMap(CommentSummary::getCommentId, Function.identity()));
		Map<Long, List<Attachment>> attachmentMap = attachmentService.lambdaQuery()
				.in(Attachment::getCommentId, ids)
				.list()
				.stream()
				.collect(Collectors.groupingBy(Attachment::getCommentId));
		List<Long> userIds = comments.stream()
				.map(Comment::getUserId)
				.toList();
		Map<Long, User> userMap = userService.getUserByIds(userIds).stream()
				.collect(Collectors.toMap(User::getId, Function.identity()));
		Long userId = BaseContext.getUserId();
		Map<Long, Vote> voteMap = (userId != null)?
				voteService.lambdaQuery()
						.in(Vote::getCommentId, ids)
						.eq(Vote::getUserId, userId)
						.list()
						.stream()
						.collect(Collectors.toMap(Vote::getCommentId, Function.identity()))
				: new HashMap<>();
		return comments.stream().map(comment -> {
			//basic info
			CommentVo commentVo = new CommentVo();
			commentVo.setId(comment.getId());
			commentVo.setContent(comment.getContent());
			commentVo.setCreateTime(comment.getCreateTime());
			commentVo.setParentId(commentVo.getParentId());

			//user info
			commentVo.setUserId(comment.getUserId());
			if(userMap.containsKey(comment.getUserId())){
				User user = userMap.get(comment.getUserId());
				commentVo.setNickname(user.getNickname());
				commentVo.setAvatar(user.getAvatar());
			}

			//summary
			CommentSummary summary = summaryMap.get(comment.getId());
			commentVo.setUpvote(summary.getUpvoteCount());
			commentVo.setDownvote(summary.getDownvoteCount());
			commentVo.setCommentCount(summary.getCommentCount());

			//vote
			Vote vote = voteMap.get(comment.getId());
			if(vote != null){
				commentVo.setVote(vote.getType());
			}

			//attachment
			List<AttachmentVo> images = new ArrayList<>();
			List<Attachment> attachments = attachmentMap.get(comment.getId());
			if(attachments != null){
				attachments.forEach(attachment -> {
					AttachmentVo attachmentVo = new AttachmentVo();
					attachmentVo.setName(attachment.getNo() + getFileExtension(attachment.getPath()));
					attachmentVo.setSize(attachment.getSize());
					if(attachment.getType() == 1){
						attachmentVo.setUrl(minioUtils.generateFileUrl(MinioConstant.POST_IMAGES_BUCKET, attachment.getPath()));
						images.add(attachmentVo);
					} else {
						String url = minioUtils.generateFileUrl(MinioConstant.POST_VOICES_BUCKET, attachment.getPath());
						attachmentVo.setUrl(url);
						commentVo.setVoice(attachmentVo);
					}
				});
			}
			commentVo.setImages(images);
			return commentVo;
		}).filter(comment -> comment.getUserId() != null).toList();
	}

	@Override
	public CommentVo addComment(CommentDto dto) {
		Long userId = BaseContext.getUserId();
		Comment comment = BeanUtil.toBean(dto, Comment.class);
		comment.setUserId(userId);
		commentService.save(comment);

		CommentSummary summary = new CommentSummary();
		summary.setCommentId(comment.getId());
		commentSummaryService.save(summary);

		CommentVo commentVo = BeanUtil.toBean(comment, CommentVo.class);
		User user = userService.getUserById(userId);
		commentVo.setAvatar(user.getAvatar());
		commentVo.setNickname(user.getNickname());

		postSummaryService.lambdaUpdate()
				.eq(PostSummary::getPostId, comment.getPostId())
				.setIncrBy(PostSummary::getCommentCount, 1)
				.update();
		return commentVo;
	}

	@Override
	public CommentVo addCommentNew(Long postId, Long parentId, String content, MultipartFile[] images, MultipartFile voice) {
		//保存帖子和文字内容
		Long userId = BaseContext.getUserId();
		Comment comment = new Comment();
		comment.setPostId(postId);
		comment.setParentId(parentId);
		comment.setContent(content);
		comment.setUserId(userId);
		commentService.save(comment);

		CommentSummary summary = new CommentSummary();
		summary.setCommentId(comment.getId());
		commentSummaryService.save(summary);

		CommentVo commentVo = BeanUtil.toBean(comment, CommentVo.class);
		User user = userService.getUserById(userId);
		commentVo.setAvatar(user.getAvatar());
		commentVo.setNickname(user.getNickname());

		postSummaryService.lambdaUpdate()
				.eq(PostSummary::getPostId, comment.getPostId())
				.setIncrBy(PostSummary::getCommentCount, 1)
				.update();

		//保存图片
		List<String> imageName = null;
		try {
			if(images != null){
				imageName = new ArrayList<>();
				for(MultipartFile image : images){
					String fileName = minioUtils.uploadFile(image, MinioConstant.POST_IMAGES_BUCKET);
					imageName.add(fileName);
				}
			}
		} catch (Exception e){
			log.error("上传图片时发生错误", e);
			throw new BaseException(ExceptionEnum.POST_IMAGE_UPLOAD_ERROR);
		}

		//保存录音
		String voiceName = null;
		try {
			if(voice != null){
				voiceName = minioUtils.uploadFile(voice, MinioConstant.POST_VOICES_BUCKET);
			}
		} catch (Exception e){
			log.error("上传音频时发生错误", e);
			throw new BaseException(ExceptionEnum.POST_VOICE_UPLOAD_ERROR);
		}

		//保存附件
		List<Attachment> attachments = new ArrayList<>();
		if(images != null){
			for(int i = 0; i < images.length; i++){
				Attachment attachment = new Attachment();
				attachment.setCommentId(comment.getId());
				attachment.setPath(imageName.get(i));
				attachment.setSize(images[i].getSize());
				attachment.setNo(i + 1);
				attachment.setType(1);
				attachments.add(attachment);
			}
		}
		if(voice != null){
			Attachment attachment = new Attachment();
			attachment.setCommentId(comment.getId());
			attachment.setPath(voiceName);
			attachment.setSize(voice.getSize());
			attachment.setNo(1);
			attachment.setType(2);
			attachments.add(attachment);
		}
		attachmentService.saveBatch(attachments);

		List<AttachmentVo> imagesAttachment = new ArrayList<>();
		attachments.forEach(attachment -> {
			AttachmentVo attachmentVo = new AttachmentVo();
			attachmentVo.setName(attachment.getNo() + getFileExtension(attachment.getPath()));
			attachmentVo.setSize(attachment.getSize());
			if(attachment.getType() == 1){
				attachmentVo.setUrl(minioUtils.generateFileUrl(MinioConstant.POST_IMAGES_BUCKET, attachment.getPath()));
				imagesAttachment.add(attachmentVo);
			} else {
				String url = minioUtils.generateFileUrl(MinioConstant.POST_VOICES_BUCKET, attachment.getPath());
				attachmentVo.setUrl(url);
				commentVo.setVoice(attachmentVo);
			}
		});
		commentVo.setImages(imagesAttachment);

		return commentVo;
	}

	@Override
	public Long createPostNew(String title, String content, Integer type, MultipartFile[] images, MultipartFile voice) {
		Long userId = BaseContext.getUserId();
		//TODO 标题限制255字符 文章2^16-1 字数检查这样子写不对吧
		//检查内容
		if(type == PostType.QUESTION){
			if(StrUtil.isEmpty(title)){
				throw new BaseException(ExceptionEnum.POST_TITLE_EMPTY);
			}
		}
		if(title.length() > 255){
			throw new BaseException(ExceptionEnum.POST_TITLE_RULE);
		}
		if(content.length() > 65535){
			throw new BaseException(ExceptionEnum.POST_TITLE_RULE);
		}
		//保存内容
		Post post = new Post();
		post.setTitle(title);
		post.setContent(content);
		post.setType(type);
		post.setUserId(userId);
		this.save(post);
		PostSummary postSummary = new PostSummary();
		postSummary.setPostId(post.getId());
		postSummaryService.save(postSummary);

		//保存图片
		List<String> imageName = null;
		try {
			if(images != null){
				imageName = new ArrayList<>();
				for(MultipartFile image : images){
					String fileName = minioUtils.uploadFile(image, MinioConstant.POST_IMAGES_BUCKET);
					imageName.add(fileName);
				}
			}
		} catch (Exception e){
			log.error("上传图片时发生错误", e);
			throw new BaseException(ExceptionEnum.POST_IMAGE_UPLOAD_ERROR);
		}

		//保存录音
		String voiceName = null;
		try {
			if(voice != null){
				voiceName = minioUtils.uploadFile(voice, MinioConstant.POST_VOICES_BUCKET);
			}
		} catch (Exception e){
			log.error("上传音频时发生错误", e);
			throw new BaseException(ExceptionEnum.POST_VOICE_UPLOAD_ERROR);
		}

		//保存附件
		List<Attachment> attachments = new ArrayList<>();
		if(images != null){
			for(int i = 0; i < images.length; i++){
				Attachment attachment = new Attachment();
				attachment.setPostId(post.getId());
				attachment.setPath(imageName.get(i));
				attachment.setSize(images[i].getSize());
				attachment.setNo(i + 1);
				attachment.setType(1);
				attachments.add(attachment);
			}
		}
		if(voice != null){
			Attachment attachment = new Attachment();
			attachment.setPostId(post.getId());
			attachment.setPath(voiceName);
			attachment.setSize(voice.getSize());
			attachment.setNo(1);
			attachment.setType(2);
			attachments.add(attachment);
		}
		attachmentService.saveBatch(attachments);

		return post.getId();
	}

	@Override
	public boolean vote(VoteDto dto) {
		//update vote where user_id = #{userId} and post_id = #{postId} and type != #{type} set type = type
		Long userId = BaseContext.getUserId();
		Vote vote = voteService.lambdaQuery()
				.eq(Vote::getUserId, userId)
				.eq(dto.getPost(), Vote::getPostId, dto.getId())
				.eq(!dto.getPost(), Vote::getCommentId, dto.getId())
				.one();
		if(vote == null){
			vote = new Vote();
			if(dto.getPost()){
				vote.setPostId(dto.getId());
			} else {
				vote.setCommentId(dto.getId());
			}
			vote.setType(dto.getType());
			vote.setUserId(userId);
			voteService.save(vote);
			if(dto.getPost()){
				postSummaryService.lambdaUpdate()
						.eq(dto.getPost(), PostSummary::getPostId, dto.getId())
						.setIncrBy(dto.getType() == 1, PostSummary::getUpvoteCount, 1)
						.setIncrBy(dto.getType() == -1, PostSummary::getDownvoteCount, 1)
						.update();
			} else {
				commentSummaryService.lambdaUpdate()
						.eq(dto.getPost(), CommentSummary::getCommentId, dto.getId())
						.setIncrBy(dto.getType() == 1, CommentSummary::getUpvoteCount, 1)
						.setIncrBy(dto.getType() == -1, CommentSummary::getDownvoteCount, 1)
						.update();
			}
		} else {
			if(vote.getType().equals(dto.getType())){ //不可重复
				return false;
			}
			vote.setType(dto.getType());
			voteService.updateById(vote);
			if(dto.getPost()){
				postSummaryService.lambdaUpdate()
						.eq(dto.getPost(), PostSummary::getPostId, dto.getId())
						.setIncrBy(dto.getType() == 1, PostSummary::getUpvoteCount, 1)
						.setIncrBy(dto.getType() == 1, PostSummary::getDownvoteCount, -1)
						.setIncrBy(dto.getType() == -1, PostSummary::getUpvoteCount, -1)
						.setIncrBy(dto.getType() == -1, PostSummary::getDownvoteCount, 1)
						.update();
			} else {
				commentSummaryService.lambdaUpdate()
						.eq(dto.getPost(), CommentSummary::getCommentId, dto.getId())
						.setIncrBy(dto.getType() == 1, CommentSummary::getUpvoteCount, 1)
						.setIncrBy(dto.getType() == 1, CommentSummary::getDownvoteCount, -1)
						.setIncrBy(dto.getType() == -1, CommentSummary::getUpvoteCount, -1)
						.setIncrBy(dto.getType() == -1, CommentSummary::getDownvoteCount, 1)
						.update();
			}
		}
		return true;
	}

	private String getFileExtension(String fileName){
		if (fileName != null && fileName.contains(".")) {
			return fileName.substring(fileName.lastIndexOf("."));
		}
		return "";  //那就不要拓展名
	}

}
