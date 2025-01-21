package com.ety.natively.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.io.Serial;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
@TableName("vote")
public class Vote implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 投票ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 投票人ID
     */
    private Long userId;

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 评论ID
     */
    private Long commentId;

    /**
     * 1 点赞 -1 down vote
     */
    private Boolean type;

    /**
     * 创建时间UTC
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
