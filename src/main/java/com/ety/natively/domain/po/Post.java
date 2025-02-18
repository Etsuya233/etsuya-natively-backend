package com.ety.natively.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.io.Serial;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.time.ZoneOffset;
import java.util.Comparator;

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
@TableName("post")
public class Post implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 帖子ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    private String previewText;
    private String previewImage;
    private String previewVoice;

    private Boolean previewHasMore;

    /**
     * 创建时间UTC
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    public static Comparator<Post> compareByIdDesc = (o1, o2) -> o2.getId().compareTo(o1.getId());

    public static final Post EMPTY = new Post();

    static {
        EMPTY.setId(0L);
        EMPTY.setUserId(0L);
        EMPTY.setTitle("(Deleted)");
        EMPTY.setContent("(Deleted)");
        EMPTY.setPreviewText("(Deleted)");
        EMPTY.setCreateTime(LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC));
        EMPTY.setPreviewHasMore(false);

    }
}
