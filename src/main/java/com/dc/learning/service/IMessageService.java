package com.dc.learning.service;

import com.dc.learning.domain.po.Message;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Etsuya
 * @since 2024-10-14
 */
public interface IMessageService extends IService<Message> {


	List<Message> getMessage(Long id);
}
