package com.ety.natively.service.impl;

import com.ety.natively.domain.po.Vote;
import com.ety.natively.mapper.VoteMapper;
import com.ety.natively.service.IVoteService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-11-10
 */
@Service
public class VoteServiceImpl extends ServiceImpl<VoteMapper, Vote> implements IVoteService {

}
