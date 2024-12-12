package com.ety.natively.service.impl;

import com.ety.natively.domain.po.Contact;
import com.ety.natively.mapper.ContactMapper;
import com.ety.natively.service.IContactService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-12-04
 */
@Service
public class ContactServiceImpl extends ServiceImpl<ContactMapper, Contact> implements IContactService {

}
