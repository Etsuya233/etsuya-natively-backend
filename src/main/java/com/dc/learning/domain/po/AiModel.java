package com.dc.learning.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
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
 * @since 2024-09-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ai_model")
public class AiModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * AI大模型ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * AI扮演的角色名称
     */
    private String name;

    /**
     * AI大模型名称
     */
    private String modelName;

    /**
     * 描述
     */
    private String description;

    /**
     * 提示Prompt
     */
    private String prompt;

    /**
     * 是否手动生成标题
     */
    private Boolean generateTitle;

    /**
     * 携带的历史消息数
     */
    private Integer attachCount;

    /**
     * 参数 temperature
     */
    private Double temperature;

    /**
     * 参数 top_p
     */
    private Double topP;

    /**
     * 参数 max_tokens
     */
    private Integer maxTokens;

    /**
     * 参数 presence_penalty
     */
    private Double presencePenalty;

    /**
     * 参数 frequency_penalty
     */
    private Double frequencyPenalty;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
