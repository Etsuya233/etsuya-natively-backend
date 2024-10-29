package com.dc.learning.service;

import com.dc.learning.domain.po.Achievement;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Etsuya
 * @since 2024-10-12
 */
public interface IAchievementService extends IService<Achievement> {

	Achievement getAchievement(Integer id);

	List<Achievement> getAllAchievements();
}
