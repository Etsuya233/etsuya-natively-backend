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
@TableName("achievement")
public class Achievement implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 成就ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 成就名
     */
    private String name;

    /**
     * 成就描述
     */
    private String description;

    /**
     * 成就符号
     */
    private String icon;

    /**
     * 成就图像 和 符号二选一
     */
    private String image;


}
