package com.ety.natively.mapper;

import com.ety.natively.domain.po.UserRelationship;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Etsuya
 * @since 2025-01-01
 */
public interface UserRelationshipMapper extends BaseMapper<UserRelationship> {

	List<UserRelationship> getFollowings(Long userId, Long lastId);

	List<UserRelationship> getFollowers(Long userId, Long lastId);

}
