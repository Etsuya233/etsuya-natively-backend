package com.ety.natively.controller;

import com.ety.natively.domain.R;
import com.ety.natively.domain.dto.*;
import com.ety.natively.domain.vo.*;
import com.ety.natively.service.IPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/postV2")
@RequiredArgsConstructor
public class PostController {

	private final IPostService postService;

	@PostMapping
	public R<Long> createPost(@RequestBody PostCreationDto dto){
		Long postID = postService.createPost(dto);
		return R.ok(postID);
	}

	@GetMapping("/postVerification")
	public R<String> getCreatePostVerificationCode(){
		String code = postService.getCreatePostVerificationCode();
		return R.ok(code);
	}

	@GetMapping
	public R<PostVo> getPostById(@RequestParam(value = "id") Long id){
		PostVo ret = postService.getPostById(id);
		return R.ok(ret);
	}

	@PostMapping("/file")
	public R<String> uploadPostAttachment(@RequestParam(name = "file", required = false) MultipartFile file,
										  @RequestParam("code") String verificationCode){
		String name = postService.uploadPostAttachment(file, verificationCode);
		return R.ok(name);
	}

	@GetMapping("/recommendation")
	public R<List<PostPreview>> getPostRecommendation(@RequestParam(value = "lastId", required = false) Long lastId){
		List<PostPreview> ret = postService.getPostRecommendation(lastId);
		return R.ok(ret);
	}

	@GetMapping("/following")
	public R<List<PostPreview>> getPostByFollowing(@RequestParam(value = "lastId", required = false) Long lastId){
		List<PostPreview> ret = postService.getPostByFollowing(lastId);
		return R.ok(ret);
	}

	// optimize this method
	@GetMapping("/trending")
	public R<List<PostPreview>> getPostTrending(@RequestParam(value = "rank", defaultValue = "1") Integer rank){
		List<PostPreview> ret = postService.getPostTrending(rank);
		return R.ok(ret);
	}

	@PostMapping("/vote")
	public R<VoteCompleteVo> vote(@RequestBody VoteDto dto){
		VoteCompleteVo ret = postService.vote(dto);
		return R.ok(ret);
	}

	@PostMapping("/comment")
	public R<CommentVo> createComment(
			@RequestParam(value = "postId", required = false) Long postId,
			@RequestParam(value = "parentId", required = false) Long parentId,
			@RequestParam("content") String content,
			@RequestParam(value = "image", required = false) MultipartFile image,
			@RequestParam(value = "voice", required = false) MultipartFile voice,
			@RequestParam(value = "compare", required = false) String compare){
		CommentVo vo = postService.createComment(postId, parentId, content, image, voice, compare);
		return R.ok(vo);
	}

	@GetMapping("/comments")
	public R<List<CommentVo>> getCommentList(@RequestParam(name = "post", defaultValue = "true") Boolean post,
											 @RequestParam("id") Long id,
											 @RequestParam(value = "lastId", required = false) Long lastId,
											 @RequestParam(value = "sort", defaultValue = "1") Integer sort){
		List<CommentVo> ret = postService.getCommentList(post, id, lastId, sort);
		return R.ok(ret);
	}

	@GetMapping("/comments/hot")
	public R<List<CommentVo>> getCommentListByHot(@RequestParam("id") Long id,
												  @RequestParam(value = "count", defaultValue = "0") Long count){
		List<CommentVo> ret = postService.getCommentListByHot(id, count);
		return R.ok(ret);
	}

	@GetMapping("/comment")
	public R<CommentVo> getCommentById(@RequestParam(value = "id") Long id){
		CommentVo ret = postService.getCommentById(id);
		return R.ok(ret);
	}

	@GetMapping("/comment/parent")
	public R<CommentParentChain> getParentCommentCascade(@RequestParam("id") Long id){
		CommentParentChain ret = postService.getParentCommentCascade(id);
		return R.ok(ret);
	}

	@PostMapping("/bookmark")
	public R<Void> createBookmark(@RequestBody BookmarkCreateDto dto){
		postService.createBookmark(dto);
		return R.ok();
	}

	@GetMapping("/bookmark")
	public R<List<BookmarkVo>> getBookmark(@RequestParam(required = false) Long lastId){
		List<BookmarkVo> ret = postService.getBookmark(lastId);
		return R.ok(ret);
	}

	@PutMapping("/bookmark")
	public R<Void> updateBookmark(@RequestBody BookmarkUpdateDto dto){
		postService.updateBookmark(dto);
		return R.ok();
	}

	@DeleteMapping("/bookmark/{id}")
	public R<Void> deleteBookmark(@PathVariable Long id){
		postService.deleteBookmark(id);
		return R.ok();
	}

	@GetMapping("/user")
	public R<List<PostPreview>> getUserPosts(@RequestParam(required = true) Long userId,
											 @RequestParam(required = false) Long lastId){
		List<PostPreview> ret = postService.getUserPost(userId, lastId);
		return R.ok(ret);
	}

	@DeleteMapping
	public R<Void> deletePost(@RequestBody DeleteDto dto){
		postService.deletePost(dto);
		return R.ok();
	}

	// mq ack
	@DeleteMapping("/comment")
	public R<Void> deleteComment(@RequestBody DeleteDto dto){
		postService.deleteComment(dto);
		return R.ok();
	}
}
