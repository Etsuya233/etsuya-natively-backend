package com.ety.natively.mapper;

import com.ety.natively.domain.dto.PostLanguageCountDto;
import com.ety.natively.domain.po.PostLanguage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Etsuya
 * @since 2025-01-19
 */
public interface PostLanguageMapper extends BaseMapper<PostLanguage> {

	List<PostLanguageCountDto> selectPostLanguageCountByIds(List<Long> ids);

}
