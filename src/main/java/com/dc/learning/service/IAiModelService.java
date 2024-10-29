package com.dc.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dc.learning.domain.po.AiModel;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Etsuya
 * @since 2024-09-30
 */
public interface IAiModelService extends IService<AiModel> {

	AiModel getAiModel(Integer id);

	List<AiModel> getAllModels();
}
