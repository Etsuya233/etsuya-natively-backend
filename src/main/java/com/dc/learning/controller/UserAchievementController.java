package com.dc.learning.controller;


import com.dc.learning.domain.R;
import com.dc.learning.domain.po.UserAchievement;
import com.dc.learning.domain.vo.UserAchievementVo;
import com.dc.learning.service.IUserAchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Etsuya
 * @since 2024-10-12
 */
@RestController
@RequestMapping("/user-achievement")
@RequiredArgsConstructor
public class UserAchievementController {

	private final IUserAchievementService userAchievementService;

	@GetMapping
	public R<List<UserAchievementVo>> getUserAchievement(@RequestParam(required = false) Long id) {
		List<UserAchievementVo> ret = userAchievementService.getUserAchievement(id);
		return R.ok(ret);
	}

}
