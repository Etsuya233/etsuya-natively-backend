package com.ety.natively.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ety.natively.domain.dto.*;
import com.ety.natively.domain.po.Post;
import com.ety.natively.domain.vo.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IPostService extends IService<Post> {

	String getCreatePostVerificationCode();

	String uploadPostAttachment(MultipartFile file, String verificationCode);

	Long createPost(PostCreationDto dto);

	List<PostPreview> getPostRecommendation(Long lastId);

	List<PostPreview> getPostByFollowing(Long lastId);

	List<PostPreview> getPostTrending(Integer rank);

	VoteCompleteVo vote(VoteDto dto);

	CommentVo createComment(Long postId, Long parentId, String content, MultipartFile image, MultipartFile voice, String compare);

	List<CommentVo> getCommentList(Boolean post, Long id, Long lastId, Integer sort);

	PostVo getPostById(Long id);

	CommentVo getCommentById(Long id);

	List<BookmarkVo> getBookmark(Long lastId);

	List<PostPreview> getUserPost(Long userId, Long lastId);

	void updateBookmark(BookmarkUpdateDto dto);

	void createBookmark(BookmarkCreateDto dto);

	void deleteBookmark(Long id);

	List<PostPreview> getPostPreview(List<Post> posts);

	void deletePost(DeleteDto dto);

	void deleteComment(DeleteDto dto);

	CommentParentChain getParentCommentCascade(Long id);

	List<CommentVo> getCommentListByHot(Long id, Long count);
}
