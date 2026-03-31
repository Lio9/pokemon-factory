package com.lio9.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.common.mapper.TypeMapper;
import com.lio9.common.model.Type;
import com.lio9.common.service.TypeService;
import org.springframework.stereotype.Service;

/**
 * 属性服务实现
 */
@Service
public class TypeServiceImpl extends ServiceImpl<TypeMapper, Type> implements TypeService {
}