package com.dc.learning.service;

import com.dc.learning.domain.po.UserAchievement;
import com.baomidou.mybatisplus.extension.service.IService;
import com.dc.learning.domain.vo.UserAchievementVo;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Etsuya
 * @since 2024-10-12
 */
public interface IUserAchievementService extends IService<UserAchievement> {

	List<UserAchievementVo> getUserAchievement(Long id);
}
