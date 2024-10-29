package com.dc.learning.service.impl;

import com.dc.learning.domain.po.Message;
import com.dc.learning.mapper.MessageMapper;
import com.dc.learning.service.IMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dc.learning.utils.BaseContext;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-10-14
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements IMessageService {


	@Override
	public List<Message> getMessage(Long id) {
		Long userId = BaseContext.getUserId();
		return this.lambdaQuery()
				.eq(Message::getUserId, userId)
				.lt(id != null, Message::getId, id)
				.orderByDesc(Message::getCreateTime)
				.orderByDesc(Message::getId)
				.last("limit 10")
				.list();
	}


}
