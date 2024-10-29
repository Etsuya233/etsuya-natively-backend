package com.dc.learning.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dc.learning.constant.RedisConstant;
import com.dc.learning.domain.po.CheckIn;
import com.dc.learning.domain.vo.CheckInVo;
import com.dc.learning.enums.ExceptionEnums;
import com.dc.learning.exception.BaseException;
import com.dc.learning.mapper.CheckInMapper;
import com.dc.learning.service.ICheckInService;
import com.dc.learning.utils.BaseContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-10-09
 */
@Service
@RequiredArgsConstructor
public class CheckInServiceImpl extends ServiceImpl<CheckInMapper, CheckIn> implements ICheckInService {

	private final StringRedisTemplate redisTemplate;

	private final Object[] lockPool = new Object[512];

	{
		for (int i = 0; i < lockPool.length; i++) {
			lockPool[i] = new Object();
		}
	}

	@Override
	public CheckInVo checkIn() {
		Long userId = BaseContext.getUserId();

		int consecutive = 1;
		synchronized (lockPool[(int) (userId % 512)]) {
			//进行签到
			LocalDateTime today = LocalDateTime.now();
			int day = today.getDayOfMonth();
			Boolean res = redisTemplate.opsForValue().setBit(RedisConstant.CHECK_IN_RECORD_PREFIX + userId, day, true);
			if (Boolean.TRUE.equals(res)) {
				throw new BaseException(ExceptionEnums.CHECK_IN_FAILED);
			}

			//更新连续签到
			if (isConsecutiveSignIn(userId, today)) {
				Long incremented = redisTemplate.opsForValue().increment(RedisConstant.CHECK_IN_CONSECUTIVE_PREFIX + userId);
				if (incremented != null) consecutive = Math.toIntExact(incremented);
			} else {
				redisTemplate.opsForValue().set(RedisConstant.CHECK_IN_CONSECUTIVE_PREFIX + userId,
						String.valueOf(1), RedisConstant.CHECK_IN_CONSECUTIVE_TTL);
			}
		}

		CheckInVo vo = new CheckInVo();
		vo.setConsecutiveCount(consecutive);
		return vo;
	}

	@Override
	public List<String> getCheckInDate(String date) {
		Long userId = BaseContext.getUserId();
		LocalDateTime now = LocalDateTime.now();
		int year;
		int month;
		if (date != null) {
			year = Integer.parseInt(date.substring(0, 4));
			month = Integer.parseInt(date.substring(5, 7));
		} else {
			year = now.getYear();
			month = now.getMonthValue();
		}

		//排除未来日期
		if (year > now.getYear()) return List.of();
		else if (year == now.getYear() && month > now.getMonthValue()) return List.of();

		//查询，如果是当月就去Redis找否则去数据库
		List<String> ret = new ArrayList<>();
		Long record = null;
		if (year == now.getYear() && month == now.getMonthValue()) {
			List<Long> list = redisTemplate.opsForValue().bitField(RedisConstant.CHECK_IN_RECORD_PREFIX + userId, BitFieldSubCommands.create(
					BitFieldSubCommands.BitFieldGet.create(BitFieldSubCommands.BitFieldType.INT_32, BitFieldSubCommands.Offset.offset(0))
			));
			if (CollUtil.isNotEmpty(list)) {
				record = list.get(0);
			}
		} else {
			date = year + ((month >= 10) ? "" + month : ("0" + month));
			CheckIn checkIn = this.lambdaQuery()
					.eq(CheckIn::getUserId, userId)
					.eq(CheckIn::getDate, date)
					.one();
			if (checkIn != null) {
				record = Long.valueOf(checkIn.getRecord());
			}
		}
		if (record != null) {
			for (int i = 1; i <= 31; i++) {
				if ((record & (1 << (31 - i))) != 0) {
					ret.add(year + "-" + ((month >= 10) ? "" + month : ("0" + month)) + "-" + ((i >= 10) ? "" + i : ("0" + i)));
				}
			}
		}
		return ret;
	}

	@Override
	public int getConsecutiveCount() {
		Long userId = BaseContext.getUserId();
		LocalDateTime now = LocalDateTime.now();
		//如果今天之前是连续签到，则直接读redis
		if (isConsecutiveSignIn(userId, now)) {
			String day = redisTemplate.opsForValue().get(RedisConstant.CHECK_IN_CONSECUTIVE_PREFIX + userId);
			if (day != null) {
				return Integer.parseInt(day);
			}
		}
		//如果不是就看看今天有没有签到即可
		Boolean bit = redisTemplate.opsForValue().getBit(RedisConstant.CHECK_IN_RECORD_PREFIX + userId, now.getDayOfMonth());
		if (Boolean.TRUE.equals(bit)) {
			return 1;
		}
		return 0;
	}

	private boolean isConsecutiveSignIn(Long userId, LocalDateTime today) {
		int day = today.getDayOfMonth();
		if (day > 1) {
			Boolean bit = redisTemplate.opsForValue().getBit(RedisConstant.CHECK_IN_RECORD_PREFIX + userId, day - 1);
			return !Boolean.FALSE.equals(bit);
		} else {
			LocalDateTime yesterday = today.minusDays(1);
			int y = yesterday.getYear();
			int m = yesterday.getMonthValue();
			int d = yesterday.getDayOfMonth();
			String date = y + ((m >= 10) ? "" + m : ("0" + m));
			CheckIn checkIn = this.lambdaQuery()
					.eq(CheckIn::getUserId, userId)
					.eq(CheckIn::getDate, date)
					.one();
			return checkIn != null && ((checkIn.getRecord() & (1 << (31 - d)))) != 0;
		}
	}
}
