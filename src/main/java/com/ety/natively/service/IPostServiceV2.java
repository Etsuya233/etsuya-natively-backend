package com.ety.natively.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ety.natively.domain.dto.BookmarkCreateDto;
import com.ety.natively.domain.dto.BookmarkUpdateDto;
import com.ety.natively.domain.dto.PostCreationDto;
import com.ety.natively.domain.dto.VoteDto;
import com.ety.natively.domain.po.Post;
import com.ety.natively.domain.vo.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IPostServiceV2 extends IService<Post> {

	String getCreatePostVerificationCode();

	String uploadPostAttachment(MultipartFile file, String verificationCode);

	Long createPost(PostCreationDto dto);

	List<PostPreview> getPostRecommendation(Long lastId);

	List<PostPreview> getPostByFollowing(Long lastId);

	List<PostPreview> getPostTrending(Integer rank);

	VoteCompleteVo vote(VoteDto dto);

	Long createComment(Long postId, Long parentId, String content, MultipartFile image, MultipartFile voice, String compare);

	List<CommentVoV2> getCommentList(Boolean post, Long id, Long lastId);

	PostVo getPostById(Long id);

	CommentVoV2 getCommentById(Long id);

	List<BookmarkVo> getBookmark(Long lastId);

	List<PostPreview> getUserPost(Long userId, Long lastId);

	void updateBookmark(BookmarkUpdateDto dto);

	void createBookmark(BookmarkCreateDto dto);

	void deleteBookmark(Long id);

	List<PostPreview> getPostPreview(List<Post> posts);
}
