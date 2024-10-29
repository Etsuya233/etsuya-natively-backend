package com.dc.learning.service.impl;

import com.dc.learning.domain.po.HelpType;
import com.dc.learning.mapper.HelpTypeMapper;
import com.dc.learning.service.IHelpTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-10-15
 */
@Service
public class HelpTypeServiceImpl extends ServiceImpl<HelpTypeMapper, HelpType> implements IHelpTypeService {

	@Override
	public List<HelpType> getAllTypes() {
		return this.list();
	}

}
