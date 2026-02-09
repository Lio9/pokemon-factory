package com.lio9.pokedex.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.common.mapper.PokemonFormMapper;
import com.lio9.common.model.PokemonForm;
import com.lio9.common.service.PokemonFormService;
import org.springframework.stereotype.Service;

/**
 * 宝可梦形态服务实现类
 * 创建人: Lio9
 */
@Service
public class PokemonFormServiceImpl extends ServiceImpl<PokemonFormMapper, PokemonForm> implements PokemonFormService {
}