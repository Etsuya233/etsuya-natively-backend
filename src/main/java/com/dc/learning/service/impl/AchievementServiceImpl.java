package com.dc.learning.service.impl;

import com.dc.learning.domain.po.Achievement;
import com.dc.learning.mapper.AchievementMapper;
import com.dc.learning.service.IAchievementService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-10-12
 */
@Service
public class AchievementServiceImpl extends ServiceImpl<AchievementMapper, Achievement> implements IAchievementService {
	private static final HashMap<Integer, Achievement> achievementCache = new HashMap<>();
	private static final List<Achievement> achievementList = new ArrayList<>();

	@Override
	public Achievement getAchievement(Integer id) {
		Achievement achievement = achievementCache.get(id);
		if (achievement == null) {
			synchronized (achievementCache) {
				if ((achievementCache.get(id)) == null) {
					achievement = this.getById(id);
					achievementCache.put(id, achievement);
				}
			}
		}
		return achievement;
	}

	@Override
	public List<Achievement> getAllAchievements() {
		synchronized (achievementList) {
			if (achievementList.isEmpty()) {
				List<Achievement> achievements = this.list();
				achievementList.addAll(achievements);
			}
			return new ArrayList<>(achievementList);
		}
	}

	@Scheduled(fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
	public void clearCache() {
		synchronized (achievementCache) {
			achievementCache.clear();
		}
		synchronized (achievementList) {
			achievementList.clear();
		}
	}
}
