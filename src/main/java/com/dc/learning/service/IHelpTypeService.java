package com.dc.learning.service;

import com.dc.learning.domain.po.HelpType;
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
public interface IHelpTypeService extends IService<HelpType> {

	List<HelpType> getAllTypes();

}
