package com.dc.learning.service;

import com.dc.learning.domain.po.CheckIn;
import com.baomidou.mybatisplus.extension.service.IService;
import com.dc.learning.domain.vo.CheckInVo;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Etsuya
 * @since 2024-10-09
 */
public interface ICheckInService extends IService<CheckIn> {

	CheckInVo checkIn();

	List<String> getCheckInDate(String date);

	int getConsecutiveCount();

}
