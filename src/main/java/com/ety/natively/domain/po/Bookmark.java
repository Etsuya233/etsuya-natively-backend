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
 * @since 2025-01-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("bookmark")
public class Bookmark implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Bookmark ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    /**
     * 收藏内容ID（可选）
     */
    private Long referenceId;

    /**
     * 收藏内容（可选）
     */
    private String content;

    /**
     * 类型：0 文本 1 帖子 2 评论
     */
    private Integer type;

    /**
     * 收藏备注
     */
    private String note;

    /**
     * 创建时间UTC
     */
    private LocalDateTime createTime;


}
