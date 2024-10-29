package com.dc.learning.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dc.learning.domain.po.Message;
import com.dc.learning.service.IMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;

@Component
@Slf4j
@RequiredArgsConstructor
public class MessageTask {

	private final IMessageService messageService;

	@Scheduled(cron = "0 0 4 * * *")
	public void deleteMessageOverSevenDays() {
		long start = System.currentTimeMillis();
		log.info("--- 开始执行任务deleteMessageOverSevenDays ---");

		LocalDateTime time = LocalDateTime.now().minusDays(7);
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				boolean remove = messageService.remove(new LambdaQueryWrapper<Message>()
						.lt(Message::getCreateTime, time)
						.last("limit 500"));
				if(!remove) timer.cancel();
			}
		}, 0, 5000);

		long consume = System.currentTimeMillis() - start;
		log.info("--- 已完成任务deleteMessageOverSevenDays，共耗时{}ms ---", consume);
	}

}
