package com.dc.learning.task;

import com.dc.learning.constant.RedisConstant;
import com.dc.learning.domain.po.CheckIn;
import com.dc.learning.service.ICheckInService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckInTask {

	private final ICheckInService checkInService;
	private final StringRedisTemplate redisTemplate;

	/**
	 * 将Redis中的签到数据同步至数据库
	 */
	@Scheduled(cron = "0 0 22 L * *")
	public void syncRedis2Db(){
		long start = System.currentTimeMillis();
		log.info("--- 开始执行任务syncRedis2Db ---");

		LocalDateTime now = LocalDateTime.now();
		String date = DateTimeFormatter.ofPattern("yyyyMM").format(now);

		int cursorCount = 30;
		int prefixLength = RedisConstant.CHECK_IN_RECORD_PREFIX.length();

		ScanOptions scanOptions = ScanOptions.scanOptions()
				.count(cursorCount)
				.match(RedisConstant.CHECK_IN_RECORD_PREFIX + "*")
				.build();

		//遍历所有符合条件的key
		try (Cursor<String> cursor = redisTemplate.scan(scanOptions)){
			//每次最多获取cursorCount个
			String[] keys = new String[cursorCount];
			while(cursor.hasNext()){
				int cnt;
				for(cnt = 0; cnt < cursorCount && cursor.hasNext(); cnt++){
					String key = cursor.next();
					keys[cnt] = key;
				}
				final int finalCnt = cnt;
				List<Object> pipelined = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
					//获取每个数据
					for(int i = 0; i < finalCnt; i++){
						connection.stringCommands().bitField(
								keys[i].getBytes(StandardCharsets.UTF_8),
								BitFieldSubCommands.create(
										BitFieldSubCommands.BitFieldGet.create(
												BitFieldSubCommands.BitFieldType.INT_32,
												BitFieldSubCommands.Offset.offset(0))));
					}
					//删除每个数据
					for(int i = 0; i < finalCnt; i++){
						connection.keyCommands().del(keys[i].getBytes(StandardCharsets.UTF_8));
					}
					return null;
				});
				ArrayList<CheckIn> checkIns = new ArrayList<>(cnt);
				for(int i = 0; i < cnt; i++){
					String userId = keys[i].substring(prefixLength);
					ArrayList<Long> res = (ArrayList<Long>) pipelined.get(i);
					Long record = res.get(0);
					checkIns.add(new CheckIn(null, Long.parseLong(userId), date, record.intValue()));
				}
				checkInService.saveBatch(checkIns);
			}
		}

		long time = System.currentTimeMillis() - start;
		log.info("--- 已完成任务syncRedis2Db，共耗时{}ms ---", time);
	}

}
