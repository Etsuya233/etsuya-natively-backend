package com.ety.natively.controller;

import com.ety.natively.domain.R;
import com.ety.natively.domain.dto.CommentCreationDto;
import com.ety.natively.domain.dto.PostCreationDto;
import com.ety.natively.domain.dto.VoteDto;
import com.ety.natively.domain.vo.*;
import com.ety.natively.service.IPostServiceV2;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/postV2")
@RequiredArgsConstructor
public class PostControllerV2 {

	private final IPostServiceV2 postService;

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

	@PostMapping("/vote")
	public R<VoteCompleteVo> vote(@RequestBody VoteDto dto){
		VoteCompleteVo ret = postService.vote(dto);
		return R.ok(ret);
	}

	@PostMapping("/comment")
	public R<Long> createComment(
			@RequestParam("postId") Long postId,
			@RequestParam(value = "parentId", required = false) Long parentId,
			@RequestParam("content") String content,
			@RequestParam(value = "image", required = false) MultipartFile image,
			@RequestParam(value = "voice", required = false) MultipartFile voice,
			@RequestParam(value = "compare", required = false) String compare){
		Long commentId = postService.createComment(postId, parentId, content, image, voice, compare);
		return R.ok(commentId);
	}

	@GetMapping("/comments")
	public R<List<CommentVoV2>> getCommentList(@RequestParam(name = "post", defaultValue = "true") Boolean post,
								  @RequestParam("id") Long id,
								  @RequestParam(value = "lastId", required = false) Long lastId){
		List<CommentVoV2> ret = postService.getCommentList(post, id, lastId);
		return R.ok(ret);
	}

	@GetMapping("/comment")
	public R<CommentVoV2> getCommentById(@RequestParam(value = "id") Long id){
		CommentVoV2 ret = postService.getCommentById(id);
		return R.ok(ret);
	}
}
