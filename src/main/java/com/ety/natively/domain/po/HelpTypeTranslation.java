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
@TableName("help_type_translation")
public class HelpTypeTranslation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 帮助类型翻译ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 帮助类型ID
     */
    private Integer helpTypeId;

    /**
     * 语言
     */
    private String language;

    /**
     * 类型翻译
     */
    private String translation;


}
