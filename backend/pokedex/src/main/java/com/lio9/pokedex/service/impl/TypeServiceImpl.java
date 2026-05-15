package com.lio9.pokedex.service.impl;



import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.pokedex.model.Type;
import com.lio9.pokedex.mapper.TypeMapper;
import com.lio9.pokedex.service.TypeService;
import org.springframework.stereotype.Service;

/**
 * 属性服务实现
 */
@Service
public class TypeServiceImpl extends ServiceImpl<TypeMapper, Type> implements TypeService {
}
