package com.ety.natively.mapper;

import com.ety.natively.domain.po.ChatUnread;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Etsuya
 * @since 2024-12-06
 */
public interface ChatUnreadMapper extends BaseMapper<ChatUnread> {

	@Insert("insert into chat_unread (sender_id, receiver_id, count) values (#{senderId}, #{receiverId}, 1) " +
			"on duplicate key update count = count + 1")
	void unreadCountPlusOne(long receiverId, long senderId);
}
