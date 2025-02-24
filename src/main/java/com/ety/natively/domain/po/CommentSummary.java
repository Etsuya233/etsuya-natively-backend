package com.ety.natively.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serial;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author Etsuya
 * @since 2024-11-10
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("comment_summary")
@NoArgsConstructor
@AllArgsConstructor
public class CommentSummary implements Serializable, VoteSummary {

	@Serial
    private static final long serialVersionUID = 1L;

    /**
     * 帖子ID
     */
    @TableId(value = "comment_id", type = IdType.INPUT)
    private Long commentId;

    /**
     * 点赞数
     */
    private Long upvoteCount;

    /**
     * 差评数
     */
    private Long downvoteCount;

    /**
     * 评论总数
     */
    private Long commentCount;


    public static final CommentSummary EMPTY = new CommentSummary();

    static {
        EMPTY.setCommentId(0L);
        EMPTY.setUpvoteCount(0L);
        EMPTY.setDownvoteCount(0L);
        EMPTY.setCommentCount(0L);
    }

}
