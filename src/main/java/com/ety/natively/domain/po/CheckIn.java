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
 * @since 2024-11-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("check_in")
public class CheckIn implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 月签到记录ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 时间：YYYYmm
     */
    private String date;

    /**
     * 签到记录，格式：第31位二进制位为第一天
     */
    private Integer record;

    /**
     * 创建时间UTC
     */
    private LocalDateTime createTime;


}
