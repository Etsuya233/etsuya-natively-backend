package com.ety.natively.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentParentChain {
	private PostVo post;
	private List<CommentVo> comments;
}
