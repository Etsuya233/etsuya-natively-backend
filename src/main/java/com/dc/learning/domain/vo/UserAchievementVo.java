package com.dc.learning.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAchievementVo {
	private Long id;

	/**
	 * 成就ID
	 */
	private Integer achievementId;

	private String achievementName;

	private String achievementIcon;

	private String achievementDescription;

	/**
	 * 创建时间
	 */
	private LocalDateTime createTime;
}
