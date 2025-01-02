package com.ety.natively.domain.vo;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.io.Serial;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.time.format.DateTimeFormatter;

import com.ety.natively.domain.po.ChatMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.beans.BeanUtils;

/**
 * <p>
 *
 * </p>
 *
 * @author Etsuya
 * @since 2024-12-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("chat_message")
public class ChatMessageVo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private static DateTimeFormatter  dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	/**
	 * 消息ID
	 */
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	private Long id;

	/**
	 * 发送者ID
	 */
	private Long senderId;

	/**
	 * 接受者ID
	 */
	private Long receiverId;

	/**
	 * 消息类型（1，文本；2，图片；3，Voice；4，其他）
	 */
	private Integer type;

	/**
	 * 消息内容
	 */
	private String content;

	private String date;

	private String time;

	public static ChatMessageVo of(ChatMessage chatMessage) {
		ChatMessageVo chatMessageVo = new ChatMessageVo();
		BeanUtils.copyProperties(chatMessage, chatMessageVo);
		String formatted = dateTimeFormatter.format(chatMessage.getCreateTime());
		chatMessageVo.setDate(formatted.substring(0, 10));
		chatMessageVo.setTime(formatted.substring(11, 18));
		return chatMessageVo;
	}

}
