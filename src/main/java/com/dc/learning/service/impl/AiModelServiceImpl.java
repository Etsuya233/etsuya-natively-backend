package com.dc.learning.service.impl;

import com.dc.learning.domain.po.AiModel;
import com.dc.learning.mapper.AiModelMapper;
import com.dc.learning.service.IAiModelService;
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
 * @since 2024-09-30
 */
@Service
public class AiModelServiceImpl extends ServiceImpl<AiModelMapper, AiModel> implements IAiModelService {

	private static final HashMap<Integer, AiModel> aiModelCache = new HashMap<>();
	private static final List<AiModel> aiModelList = new ArrayList<>();

	@Override
	public AiModel getAiModel(Integer id) {
		AiModel aiModel = aiModelCache.get(id);
		if(aiModel == null){
			synchronized(aiModelCache){
				if((aiModelCache.get(id)) == null){
					aiModel = this.getById(id);
					aiModelCache.put(id, aiModel);
				}
			}
		}
		return aiModel;
	}

	@Override
	public List<AiModel> getAllModels(){
		synchronized (aiModelList){
			if(aiModelList.isEmpty()){
				List<AiModel> aiModels = this.list();
				aiModelList.addAll(aiModels);
			}
			return new ArrayList<>(aiModelList);
		}
	}

	@Scheduled(fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
	public void clearCache(){
		synchronized(aiModelCache){
			aiModelCache.clear();
		}
		synchronized(aiModelList){
			aiModelList.clear();
		}
	}

}
