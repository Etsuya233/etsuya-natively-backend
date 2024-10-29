package com.dc.learning.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
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
 * @since 2024-10-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("help")
public class Help implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 帮助ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 帮助类型
     */
    private Integer type;

    /**
     * 帮助标题
     */
    private String title;

    /**
     * 帮助内容
     */
    private String content;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
