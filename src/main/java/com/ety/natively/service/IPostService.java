package com.ety.natively.service;

import com.ety.natively.domain.dto.BookmarkNewDto;
import com.ety.natively.domain.dto.CommentDto;
import com.ety.natively.domain.dto.PostDto;
import com.ety.natively.domain.dto.VoteDto;
import com.ety.natively.domain.po.Post;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ety.natively.domain.vo.BookmarkVo;
import com.ety.natively.domain.vo.CommentVo;
import com.ety.natively.domain.vo.PostInfoVo;
import com.ety.natively.domain.vo.PostVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Etsuya
 * @since 2024-11-10
 */
public interface IPostService extends IService<Post> {

	Long createPost(PostDto dto);

	List<PostInfoVo> getRecommendation(Long lastId);

	PostVo getPost(Long id);

	List<CommentVo> getPostComment(Long id, Long lastId);

	CommentVo addComment(CommentDto dto);

	CommentVo addCommentNew(Long postId, Long parentId, String content, MultipartFile[] images, MultipartFile voice);

	Long createPostNew(String postId, String content, Integer type, MultipartFile[] images, MultipartFile voice);

	boolean vote(VoteDto dto);

	List<PostInfoVo> getUserPosts(Long userId, Long lastId);

	Boolean bookmark(BookmarkNewDto dto);

	List<BookmarkVo> getBookmarks(Long lastId);

	Boolean removeBookmark(BookmarkNewDto dto);
}
