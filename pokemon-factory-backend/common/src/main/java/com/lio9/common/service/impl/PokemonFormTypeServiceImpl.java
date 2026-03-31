package com.lio9.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.common.mapper.PokemonFormTypeMapper;
import com.lio9.common.model.PokemonFormType;
import com.lio9.common.service.PokemonFormTypeService;
import org.springframework.stereotype.Service;

/**
 * 宝可梦形态属性服务实现
 */
@Service
public class PokemonFormTypeServiceImpl extends ServiceImpl<PokemonFormTypeMapper, PokemonFormType> implements PokemonFormTypeService {
}