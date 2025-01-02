package com.ety.natively.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serial;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * <p>
 * 
 * </p>
 *
 * @author Etsuya
 * @since 2024-11-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_language")
@NoArgsConstructor
@AllArgsConstructor
public class UserLanguage implements Serializable, Comparable<UserLanguage> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户语言ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * ISO
     */
    private String lang;

    /**
     * 精通程度
     */
    private Integer proficiency;


    /**
     * 从好到坏排
     * @param o the object to be compared.
     * @return ？
     */
    @Override
    public int compareTo(@NotNull UserLanguage o) {
        if(!this.proficiency.equals(o.proficiency)) return o.proficiency - this.proficiency;
        return this.lang.compareTo(o.lang);
    }
}
