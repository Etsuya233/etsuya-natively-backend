package com.dc.learning.service;

import com.dc.learning.domain.dto.HelpQueryDto;
import com.dc.learning.domain.po.Help;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Etsuya
 * @since 2024-10-15
 */
public interface IHelpService extends IService<Help> {

	List<Help> getHelpByType(HelpQueryDto dto);
}
