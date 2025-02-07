package com.ety.natively.mapper;

import com.ety.natively.domain.po.Post;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Etsuya
 * @since 2024-11-10
 */
public interface PostMapper extends BaseMapper<Post> {

	List<Post> getUserFollowingFeed(Long userId, Long lastId);

}
