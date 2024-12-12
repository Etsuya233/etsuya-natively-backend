package com.ety.natively.mapper;

import com.ety.natively.domain.po.Conversation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Etsuya
 * @since 2024-12-06
 */
public interface ConversationMapper extends BaseMapper<Conversation> {

	@Insert("insert into conversation (user_a_id, user_b_id, last_id) values (#{userA}, #{userB}, #{lastId}) " +
			"on duplicate key update last_id = #{lastId}")
	void saveOrUpdateLastId(Long userA, Long userB, Long lastId);

}
