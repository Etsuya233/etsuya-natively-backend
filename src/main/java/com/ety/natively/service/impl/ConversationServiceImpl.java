package com.ety.natively.service.impl;

import com.ety.natively.domain.po.Conversation;
import com.ety.natively.mapper.ConversationMapper;
import com.ety.natively.service.IConversationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-12-06
 */
@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation> implements IConversationService {

}
