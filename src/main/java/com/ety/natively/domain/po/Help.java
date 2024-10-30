package com.ety.natively.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serial;
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
 * @since 2024-10-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("help")
public class Help implements Serializable {

    @Serial
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
     * 帮助语言
     */
    private String language;

    private Long createTime;


}
