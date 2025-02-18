package com.ety.natively.mapper;

import com.ety.natively.domain.mybatis.CommentParentId;
import com.ety.natively.domain.po.Comment;
import com.ety.natively.domain.po.Post;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ety.natively.domain.vo.CommentVo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Etsuya
 * @since 2024-11-10
 */
public interface PostMapper extends BaseMapper<Post> {

	List<Post> getUserFollowingFeed(Long userId, Long lastId);

	@Delete("delete from comment where post_id = #{postId} limit 100")
	Integer deletePostComments(Long postId);

	@Delete("""
	WITH RECURSIVE comment_tree AS (
		 -- 先找到根评论
		 SELECT id, 1 AS level FROM comment WHERE id = #{parentId}
		 UNION ALL
		 -- 递归查找所有子评论
		 SELECT c.id, level + 1 FROM comment c
		 INNER JOIN comment_tree ct ON c.parent_id = ct.id
		 WHERE level < 50
	)
	DELETE FROM comment WHERE id IN (SELECT id FROM comment_tree);
	""")
	void deleteCommentsRecursively(Long parentId);


	@Select("""
	WITH RECURSIVE comment_tree AS (
		 -- 先找到根评论
		 SELECT #{commentId} AS id, 1 AS level
		 UNION ALL
		 -- 递归查找所有子评论
		 SELECT c.id, level + 1 FROM comment c
		 INNER JOIN comment_tree ct ON c.parent_id = ct.id
		 WHERE level < 50
	)
	SELECT id FROM comment_tree;
	""")
	List<Long> getCommentChildrenId(Long commentId);

	@Update("""
	WITH RECURSIVE parent_chain AS (
		-- 基础查询，获取起始评论
		SELECT id, parent_id, 1 AS level
		from comment
		WHERE id = #{commentId}
		UNION ALL
		-- 递归查询父评论：根据当前记录的 parent_id 找到父记录
		SELECT c.id, c.parent_id, level + 1
		FROM comment c
		INNER JOIN parent_chain pc ON pc.parent_id = c.id
		WHERE pc.level < 50
	)
	UPDATE comment_summary
	SET comment_count = comment_count + 1
	WHERE comment_id IN (SELECT id FROM parent_chain) AND comment_id != #{commentId};
	""")
	void addCommentCount(Long commentId);

	List<Long> getRecommendedPostId(Long userId, Long lastId);

	@Select("""
	WITH RECURSIVE parent_chain AS (
		-- 基础查询，获取起始评论
		SELECT id, parent_id, 1 AS level
		from comment
		WHERE id = #{id}
		UNION ALL
		-- 递归查询父评论：根据当前记录的 parent_id 找到父记录
		SELECT c.id, c.parent_id, level + 1
		FROM comment c
		INNER JOIN parent_chain pc ON pc.parent_id = c.id
		WHERE pc.level < 50
	)
	SELECT *
	FROM comment
	WHERE id in (SELECT pc.id FROM parent_chain pc)
	ORDER BY id;
	""")
	List<Comment> getParentCommentCascade(Long id);

	@Select("""
	SELECT *, cs.upvote_count AS upvote, cs.downvote_count AS downvote
	FROM comment c
	INNER JOIN comment_summary cs ON c.id = cs.comment_id
	WHERE parent_id = #{id}
	ORDER BY cs.upvote_count DESC, c.id
	LIMIT 10 OFFSET #{count}
	""")
	List<CommentVo> getCommentListByHot(Long id, Long count);
}
