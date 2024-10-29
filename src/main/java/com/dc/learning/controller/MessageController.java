package com.dc.learning.controller;


import com.dc.learning.domain.R;
import com.dc.learning.domain.po.Message;
import com.dc.learning.service.IMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Etsuya
 * @since 2024-10-14
 */
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

	private final IMessageService messageService;

	/**
	 * 获取消息
	 * @param id 最后一个ID
	 * @return 消息
	 */
	@PostMapping
	public R<List<Message>> getMessage(@RequestBody(required = false) Long id){
		List<Message> msg = messageService.getMessage(id);
		return R.ok(msg);
	}

}
