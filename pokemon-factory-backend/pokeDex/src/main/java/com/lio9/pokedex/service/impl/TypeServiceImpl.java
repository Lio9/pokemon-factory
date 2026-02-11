package com.lio9.pokedex.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.common.mapper.TypeMapper;
import com.lio9.common.model.Type;
import com.lio9.common.service.TypeService;
import org.springframework.stereotype.Service;

/**
 * 属性服务实现类
 * 提供宝可梦属性数据的管理功能
 * 继承MyBatis-Plus的ServiceImpl，实现TypeService接口
 * 
 * @author Lio9
 * @version 1.0
 * @since 2024-01-01
 */
@Service
public class TypeServiceImpl extends ServiceImpl<TypeMapper, Type> implements TypeService {
}