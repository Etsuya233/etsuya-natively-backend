package com.dc.learning.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.io.Serial;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.util.Comparator;

import lombok.*;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author Etsuya
 * @since 2024-09-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ai_record")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 询问ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户对话ID
     */
    private Long conversationId;

    /**
     * 用户发送信息
     */
    private String message;

    /**
     * AI返回信息
     */
    private String result;

    /**
     * 文件链接如果有
     */
    private String file;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    public static Comparator<AiRecord> compareByTimeAsc = Comparator.comparing(AiRecord::getCreateTime);

}
