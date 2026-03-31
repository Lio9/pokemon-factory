package com.lio9.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.common.mapper.GrowthRateMapper;
import com.lio9.common.model.GrowthRate;
import com.lio9.common.service.GrowthRateService;
import org.springframework.stereotype.Service;

/**
 * 成长率服务实现
 */
@Service
public class GrowthRateServiceImpl extends ServiceImpl<GrowthRateMapper, GrowthRate> implements GrowthRateService {
}