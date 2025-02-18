package com.ety.natively.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.io.Serial;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.time.ZoneOffset;
import java.util.Comparator;

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
@TableName("comment")
@NoArgsConstructor
@AllArgsConstructor
public class Comment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 关联评论ID
     */
    private Long parentId;

    /**
     * 内容
     */
    private String content;

    private String image;
    private String voice;
    private String compare;

    /**
     * 创建时间UTC
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    public static Comparator<Comment> sortByIdAsc = Comparator.comparing(Comment::getId);

    public static final Comment EMPTY = new Comment();

    static {
        EMPTY.setId(0L);
        EMPTY.setUserId(0L);
        EMPTY.setPostId(0L);
        EMPTY.setParentId(0L);
        EMPTY.setContent("(Deleted)");
        EMPTY.setCreateTime(LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC));
    }

}
