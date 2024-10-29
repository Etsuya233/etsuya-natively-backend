package com.dc.learning.controller;


import com.dc.learning.domain.R;
import com.dc.learning.domain.vo.CheckInVo;
import com.dc.learning.service.ICheckInService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Etsuya
 * @since 2024-10-09
 */
@RestController
@RequestMapping("/check-in")
@RequiredArgsConstructor
public class CheckInController {

	private final ICheckInService checkInService;

	@PostMapping
	public R<CheckInVo> checkIn(){
		CheckInVo vo = checkInService.checkIn();
		return R.ok(vo);
	}

	@GetMapping
	public R<List<String>> getCheckedInDate(@RequestParam(required = false) String date){
		List<String> ret = checkInService.getCheckInDate(date);
		return R.ok(ret);
	}

	@GetMapping("/consecutive")
	public R<Integer> getConsecutiveCount(){
		int count = checkInService.getConsecutiveCount();
		return R.ok(count);
	}

}
