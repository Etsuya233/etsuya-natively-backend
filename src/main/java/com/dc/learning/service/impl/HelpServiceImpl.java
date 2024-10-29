package com.dc.learning.service.impl;

import com.dc.learning.domain.dto.HelpQueryDto;
import com.dc.learning.domain.po.Help;
import com.dc.learning.mapper.HelpMapper;
import com.dc.learning.service.IHelpService;
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
public class HelpServiceImpl extends ServiceImpl<HelpMapper, Help> implements IHelpService {

	@Override
	public List<Help> getHelpByType(HelpQueryDto dto) {
		return this.lambdaQuery()
				.eq(Help::getType, dto.getId())
				.gt(dto.getLastId() != null, Help::getId, dto.getLastId())
				.orderByAsc(Help::getId)
				.last("limit 10")
				.list();
	}
}
