package com.ety.natively.controller;


import com.ety.natively.domain.R;
import com.ety.natively.domain.dto.*;
import com.ety.natively.domain.po.Comment;
import com.ety.natively.domain.vo.BookmarkVo;
import com.ety.natively.domain.vo.CommentVo;
import com.ety.natively.domain.vo.PostInfoVo;
import com.ety.natively.domain.vo.PostVo;
import com.ety.natively.service.IPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Etsuya
 * @since 2024-11-10
 */
@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
public class PostController {

	private final IPostService postService;

	@Deprecated
	public R<Long> createPost(@RequestBody PostDto dto){
		Long id = postService.createPost(dto);
		return R.ok(id);
	}

	@PostMapping
	public R<Long> createPostNew(
			@RequestParam(value = "title", required = false) String title,
			@RequestParam("content") String content,
			@RequestParam("type") Integer type,
			@RequestParam(value = "images", required = false) MultipartFile[] images,
			@RequestParam(value = "voice", required = false) MultipartFile voice){
		Long id = postService.createPostNew(title, content, type, images, voice);
		return R.ok(id);
	}

	@GetMapping("/recommendation")
	public R<List<PostInfoVo>> getPosts(@RequestParam(value = "lastId", required = false) Long lastId){
		List<PostInfoVo> ret = postService.getRecommendation(lastId);
		return R.ok(ret);
	}

	@GetMapping
	public R<PostVo> getPost(@RequestParam(value = "id") Long id){
		PostVo vo = postService.getPost(id);
		return R.ok(vo);
	}

	@GetMapping("/comment")
	public R<List<CommentVo>> getPostComments(
			@RequestParam(value = "id") Long id,
			@RequestParam(value = "lastId", required = false) Long lastId){
		List<CommentVo> ret = postService.getPostComment(id, lastId);
		return R.ok(ret);
	}

	@Deprecated
	@PostMapping("/comment")
	public R<CommentVo> addComment(@RequestBody CommentDto dto){
		CommentVo vo = postService.addComment(dto);
		return R.ok(vo);
	}

	@PostMapping("/commentNew")
	public R<CommentVo> addCommentNew(
			@RequestParam("postId") Long postId,
			@RequestParam(value = "parentId", required = false) Long parentId,
			@RequestParam("content") String content,
			@RequestParam(value = "images", required = false) MultipartFile[] images,
			@RequestParam(value = "voice", required = false) MultipartFile voice){
		CommentVo ret = postService.addCommentNew(postId, parentId, content, images, voice);
		return R.ok(ret);
	}

	@PostMapping("/vote")
	public R<Boolean> vote(@RequestBody VoteDto dto){
		boolean ret = postService.vote(dto);
		return R.ok(ret);
	}

	@GetMapping("/user")
	public R<List<PostInfoVo>> getUserPosts(@RequestParam("userId") Long userId,
										@RequestParam(value = "lastId", required = false) Long lastId){
		List<PostInfoVo> ret = postService.getUserPosts(userId, lastId);
		return R.ok(ret);
	}
}
