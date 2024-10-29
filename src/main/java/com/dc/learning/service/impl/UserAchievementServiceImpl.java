package com.dc.learning.service.impl;

import com.dc.learning.domain.po.Achievement;
import com.dc.learning.domain.po.UserAchievement;
import com.dc.learning.domain.vo.UserAchievementVo;
import com.dc.learning.mapper.AchievementMapper;
import com.dc.learning.mapper.UserAchievementMapper;
import com.dc.learning.service.IAchievementService;
import com.dc.learning.service.IUserAchievementService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dc.learning.utils.BaseContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-10-12
 */
@Service
@RequiredArgsConstructor
public class UserAchievementServiceImpl extends ServiceImpl<UserAchievementMapper, UserAchievement> implements IUserAchievementService {

	private final IAchievementService achievementService;

	@Override
	public List<UserAchievementVo> getUserAchievement(Long id) {
		if(id == null){
			id = BaseContext.getUserId();
		}
		//查询用户成就列表
		List<UserAchievement> userAchievements = this.lambdaQuery()
				.eq(UserAchievement::getUserId, id)
				.list();
		//查询成就信息
		return userAchievements.stream().map(u -> {
			Achievement achievement = achievementService.getAchievement(u.getAchievementId());
			return new UserAchievementVo(u.getId(), u.getAchievementId(), achievement.getName(),
					achievement.getIcon(), achievement.getDescription(), u.getCreateTime());
		}).collect(Collectors.toList());
	}
}
