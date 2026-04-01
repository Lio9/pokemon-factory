package com.lio9.common.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lio9.common.mapper.*;
import com.lio9.common.model.*;
import com.lio9.common.service.DamageCalculatorService;
import com.lio9.common.util.DamageCalculatorUtil;
import com.lio9.common.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 伤害计算器服务实现（第9世代标准）
 */
@Service
public class DamageCalculatorServiceImpl implements DamageCalculatorService {

    @Autowired
    private TypeEfficacyMapper typeEfficacyMapper;
    
    @Autowired
    private PokemonFormMapper pokemonFormMapper;
    
    @Autowired
    private PokemonFormStatMapper pokemonFormStatMapper;
    
    @Autowired
    private PokemonFormTypeMapper pokemonFormTypeMapper;
    
    @Autowired
    private MoveMapper moveMapper;
    
    @Autowired
    private AbilityMapper abilityMapper;
    
    @Autowired
    private ItemMapper itemMapper;

    @Override
    public DamageResultVO calculateDamage(DamageCalculationRequest request) {
        DamageResultVO result = new DamageResultVO();
        
        try {
            // 1. 获取数据
            PokemonForm attacker = pokemonFormMapper.selectById(request.getAttackerFormId());
            PokemonForm defender = pokemonFormMapper.selectById(request.getDefenderFormId());
            Move move = moveMapper.selectById(request.getMoveId());
            
            if (attacker == null || defender == null || move == null) {
                result.setMinDamage(0);
                result.setMaxDamage(0);
                result.setAvgDamage(0.0);
                return result;
            }
            
            // 2. 获取技能属性
            Integer moveTypeId = move.getTypeId();
            
            // 3. 获取攻击方属性列表
            List<PokemonFormType> attackerTypes = pokemonFormTypeMapper.selectList(
                new QueryWrapper<PokemonFormType>().eq("form_id", request.getAttackerFormId())
            );
            List<Integer> attackerTypeIds = attackerTypes.stream()
                .map(PokemonFormType::getTypeId)
                .collect(Collectors.toList());
            
            // 4. 获取防御方属性列表
            List<PokemonFormType> defenderTypes = pokemonFormTypeMapper.selectList(
                new QueryWrapper<PokemonFormType>().eq("form_id", request.getDefenderFormId())
            );
            List<Integer> defenderTypeIds = defenderTypes.stream()
                .map(PokemonFormType::getTypeId)
                .collect(Collectors.toList());
            
            // 5. 获取能力值
            int attack = getAttackStat(request.getAttackerFormId(), move.getDamageClassId(), request);
            int defense = getDefenseStat(request.getDefenderFormId(), move.getDamageClassId(), request);
            
            // 6. 计算能力等级修正
            double attackBoost = DamageCalculatorUtil.getStageMultiplier(request.getAttackerAttackBoost());
            double defenseBoost = DamageCalculatorUtil.getStageMultiplier(request.getDefenderDefenseBoost());
            
            int effectiveAttack = (int) Math.floor(attack * attackBoost);
            int effectiveDefense = (int) Math.floor(defense * defenseBoost);
            
            // 7. 计算基础伤害
            int baseDamage = DamageCalculatorUtil.calculateBaseDamage(
                request.getAttackerLevel(),
                move.getPower() != null ? move.getPower() : 0,
                effectiveAttack,
                effectiveDefense
            );
            
            result.setBaseDamage(baseDamage);
            
            // 8. 计算属性相性
            double typeEffectiveness = calculateTypeEffectiveness(moveTypeId, defenderTypeIds);
            result.setTypeEffectiveness(typeEffectiveness);
            result.setEffectivenessDesc(getEffectivenessDesc(typeEffectiveness));
            
            // 9. 计算STAB（考虑太晶化）
            boolean isStab = attackerTypeIds.contains(moveTypeId);
            boolean isTeraStab = false;
            
            // 检查太晶化STAB
            if (request.getAttackerTerastalType() != null) {
                isTeraStab = request.getAttackerTerastalType().equals(moveTypeId);
                // 太晶化时，如果技能属性与太晶属性相同，视为STAB
                if (isTeraStab) {
                    isStab = true;
                }
            }
            
            result.setIsStab(isStab);
            
            // 计算STAB倍率
            double stabMultiplier = 1.0;
            if (isStab) {
                if (isTeraStab) {
                    // 太晶STAB始终是2.0倍
                    stabMultiplier = 2.0;
                } else {
                    // 普通STAB
                    stabMultiplier = DamageCalculatorUtil.STAB_MULTIPLIER;
                    
                    // 检查适应力特性
                    if (request.getAttackerAbilityId() != null) {
                        Ability ability = abilityMapper.selectById(request.getAttackerAbilityId());
                        if (ability != null && "adaptability".equalsIgnoreCase(ability.getNameEn())) {
                            stabMultiplier = DamageCalculatorUtil.ADAPTABILITY_STAB_MULTIPLIER;
                        }
                    }
                }
            }
            result.setStabMultiplier(stabMultiplier);
            
            // 10. 计算天气修正
            double weatherMultiplier = calculateWeatherModifier(move, request, attacker, defender);
            result.setWeatherMultiplier(weatherMultiplier);
            
            // 11. 计算暴击倍率
            double criticalMultiplier = request.getIsCritical() ? DamageCalculatorUtil.CRITICAL_MULTIPLIER : 1.0;
            result.setCriticalMultiplier(criticalMultiplier);
            
            // 12. 计算灼伤修正（物理伤害且攻击方灼伤）
            double burnMultiplier = 1.0;
            if (move.getDamageClassId() == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL 
                && request.getAttackerBurned()) {
                burnMultiplier = DamageCalculatorUtil.BURN_MULTIPLIER;
            }
            result.setBurnMultiplier(burnMultiplier);
            
            // 13. 计算反射壁/光墙/极光幕修正
            double screenMultiplier = calculateScreenModifier(move, request);
            result.setScreenMultiplier(screenMultiplier);
            
            // 14. 计算多目标修正
            double multiTargetMultiplier = 1.0;
            if (request.getIsDoubleBattle()) {
                multiTargetMultiplier = DamageCalculatorUtil.MULTI_TARGET_MULTIPLIER;
            }
            result.setMultiTargetMultiplier(multiTargetMultiplier);
            
            // 15. 计算特性影响
            double abilityMultiplier = calculateAbilityModifier(move, request, attacker, defender);
            result.setAbilityMultiplier(abilityMultiplier);
            
            // 16. 计算道具影响
            double itemMultiplier = calculateItemModifier(move, request, attacker, defender);
            result.setItemMultiplier(itemMultiplier);
            
            // 17. 计算场地影响
            double terrainMultiplier = calculateTerrainModifier(move, request, attacker, defender);
            
            // 18. 计算总修正倍率
            double totalModifier = stabMultiplier 
                * typeEffectiveness 
                * weatherMultiplier 
                * criticalMultiplier 
                * burnMultiplier 
                * screenMultiplier 
                * multiTargetMultiplier 
                * abilityMultiplier 
                * itemMultiplier 
                * terrainMultiplier;
            
            // 19. 计算伤害范围
            int[] damageRange = DamageCalculatorUtil.calculateDamageRange(baseDamage, totalModifier);
            
            result.setMinDamage(damageRange[0]);
            result.setMaxDamage(damageRange[1]);
            result.setAvgDamage((damageRange[0] + damageRange[1]) / 2.0);
            
            // 20. 设置技能信息
            result.setMovePower(move.getPower());
            result.setEffectivePower(move.getPower());
            result.setDamageClass(getDamageClassName(move.getDamageClassId()));
            result.setUsedAttackStat(effectiveAttack);
            result.setUsedDefenseStat(effectiveDefense);
            result.setUsedAttackType(move.getDamageClassId() == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL ? "attack" : "spAttack");
            
            // 设置技能优先度
            result.setPriority(move.getPriority() != null ? move.getPriority() : 0);
            
            // 设置连续攻击次数
            int hits = getHits(move);
            result.setHits(hits);
            
            // 设置是否接触技能
            result.setIsContact(isContactMove(move));
            
            // 21. 计算命中率
            calculateAccuracy(move, request, result);
            
            // 22. 计算击杀预估
            if (request.getDefenderHp() != null && request.getDefenderHp() > 0) {
                calculateKoEstimate(damageRange, request.getDefenderHp(), result);
            }
            
            // 23. 构建修正因子汇总
            Map<String, Double> multipliers = new HashMap<>();
            multipliers.put("STAB", stabMultiplier);
            multipliers.put("Type", typeEffectiveness);
            multipliers.put("Weather", weatherMultiplier);
            multipliers.put("Critical", criticalMultiplier);
            multipliers.put("Burn", burnMultiplier);
            multipliers.put("Screen", screenMultiplier);
            multipliers.put("Multi-Target", multiTargetMultiplier);
            multipliers.put("Ability", abilityMultiplier);
            multipliers.put("Item", itemMultiplier);
            multipliers.put("Terrain", terrainMultiplier);
            multipliers.put("Total", totalModifier);
            result.setAllMultipliers(multipliers);
            
        } catch (Exception e) {
            // 错误处理
            result.setMinDamage(0);
            result.setMaxDamage(0);
            result.setAvgDamage(0.0);
        }
        
        return result;
    }

    @Override
    @Cacheable(value = "typeEfficacyMatrix", key = "'all'")
    public Map<Integer, Map<Integer, Integer>> getTypeEfficacyMatrix() {
        List<Map<String, Object>> efficacyList = typeEfficacyMapper.selectAllTypeEfficacy();
        
        Map<Integer, Map<Integer, Integer>> matrix = new HashMap<>();
        
        for (Map<String, Object> efficacy : efficacyList) {
            Integer attackType = (Integer) efficacy.get("damage_type_id");
            Integer defendType = (Integer) efficacy.get("target_type_id");
            Integer factor = (Integer) efficacy.get("damage_factor");
            
            matrix.computeIfAbsent(attackType, k -> new HashMap<>()).put(defendType, factor);
        }
        
        return matrix;
    }

    @Override
    @Cacheable(value = "typeEfficacyByType", key = "#damageTypeId")
    public List<TypeEfficacyVO> getTypeEfficacyByDamageType(Integer damageTypeId) {
        List<Map<String, Object>> efficacyList = typeEfficacyMapper.selectByDamageTypeId(damageTypeId);
        
        return efficacyList.stream().map(e -> {
            TypeEfficacyVO vo = new TypeEfficacyVO();
            vo.setDamageTypeId((Integer) e.get("damage_type_id"));
            vo.setDamageTypeName((String) e.get("damage_type_name"));
            vo.setTargetTypeId((Integer) e.get("target_type_id"));
            vo.setTargetTypeName((String) e.get("target_type_name"));
            vo.setDamageFactor((Integer) e.get("damage_factor"));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Double calculateTypeEffectiveness(Integer moveTypeId, List<Integer> targetTypeIds) {
        if (targetTypeIds == null || targetTypeIds.isEmpty()) {
            return 1.0;
        }
        
        Double multiplier = 1.0;
        
        for (Integer targetTypeId : targetTypeIds) {
            Integer factor = typeEfficacyMapper.selectDamageFactor(moveTypeId, targetTypeId);
            if (factor != null) {
                multiplier *= (factor / 100.0);
            }
        }
        
        return multiplier;
    }
    
    /**
     * 获取攻击方能力值
     */
    private int getAttackStat(Integer formId, Integer damageClassId, DamageCalculationRequest request) {
        if (request.getAttackerAttack() != null && damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL) {
            return request.getAttackerAttack();
        }
        if (request.getAttackerSpAttack() != null && damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL) {
            return request.getAttackerSpAttack();
        }
        
        // 从数据库获取
        int statId = damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL ? 2 : 4; // 2=攻击, 4=特攻
        QueryWrapper<PokemonFormStat> wrapper = new QueryWrapper<>();
        wrapper.eq("form_id", formId).eq("stat_id", statId);
        PokemonFormStat formStat = pokemonFormStatMapper.selectOne(wrapper);
        
        return formStat != null ? formStat.getBaseStat() : 100;
    }
    
    /**
     * 获取防御方能力值
     */
    private int getDefenseStat(Integer formId, Integer damageClassId, DamageCalculationRequest request) {
        if (request.getDefenderDefense() != null && damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL) {
            return request.getDefenderDefense();
        }
        if (request.getDefenderSpDefense() != null && damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL) {
            return request.getDefenderSpDefense();
        }
        
        // 从数据库获取
        int statId = damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL ? 3 : 5; // 3=防御, 5=特防
        QueryWrapper<PokemonFormStat> wrapper = new QueryWrapper<>();
        wrapper.eq("form_id", formId).eq("stat_id", statId);
        PokemonFormStat formStat = pokemonFormStatMapper.selectOne(wrapper);
        
        return formStat != null ? formStat.getBaseStat() : 100;
    }
    
    /**
     * 计算天气修正
     */
    private double calculateWeatherModifier(Move move, DamageCalculationRequest request, 
                                           PokemonForm attacker, PokemonForm defender) {
        if (request.getWeather() == null || request.getWeather().equals(DamageCalculatorUtil.WEATHER_CLEAR)) {
            return 1.0;
        }
        
        Integer moveTypeId = move.getTypeId();
        String weather = request.getWeather();
        
        // 火系技能在晴天增强
        if (weather.equals(DamageCalculatorUtil.WEATHER_SUN) && moveTypeId == DamageCalculatorUtil.TYPE_FIRE) {
            return 1.5;
        }
        // 水系技能在晴天减弱
        if (weather.equals(DamageCalculatorUtil.WEATHER_SUN) && moveTypeId == DamageCalculatorUtil.TYPE_WATER) {
            return 0.5;
        }
        // 水系技能在雨天增强
        if (weather.equals(DamageCalculatorUtil.WEATHER_RAIN) && moveTypeId == DamageCalculatorUtil.TYPE_WATER) {
            return 1.5;
        }
        // 火系技能在雨天减弱
        if (weather.equals(DamageCalculatorUtil.WEATHER_RAIN) && moveTypeId == DamageCalculatorUtil.TYPE_FIRE) {
            return 0.5;
        }
        
        return 1.0;
    }
    
    /**
     * 计算反射壁/光墙/极光幕修正
     */
    private double calculateScreenModifier(Move move, DamageCalculationRequest request) {
        if (move.getDamageClassId() == DamageCalculatorUtil.DAMAGE_CLASS_STATUS) {
            return 1.0;
        }
        
        double multiplier = 1.0;
        boolean isDoubleBattle = request.getIsDoubleBattle();
        
        // 极光幕
        if (request.getAuroraVeilActive()) {
            multiplier = isDoubleBattle ? DamageCalculatorUtil.AURORA_VEIL_MULTIPLIER_DOUBLE 
                                        : DamageCalculatorUtil.AURORA_VEIL_MULTIPLIER_SINGLE;
        }
        // 反射壁（物理）
        else if (request.getReflectActive() && move.getDamageClassId() == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL) {
            multiplier = isDoubleBattle ? DamageCalculatorUtil.SCREEN_MULTIPLIER_DOUBLE 
                                        : DamageCalculatorUtil.SCREEN_MULTIPLIER_SINGLE;
        }
        // 光墙（特殊）
        else if (request.getLightScreenActive() && move.getDamageClassId() == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL) {
            multiplier = isDoubleBattle ? DamageCalculatorUtil.SCREEN_MULTIPLIER_DOUBLE 
                                        : DamageCalculatorUtil.SCREEN_MULTIPLIER_SINGLE;
        }
        
        return multiplier;
    }
    
    /**
     * 计算特性影响
     */
    private double calculateAbilityModifier(Move move, DamageCalculationRequest request,
                                           PokemonForm attacker, PokemonForm defender) {
        double multiplier = 1.0;
        
        if (request.getAttackerAbilityId() != null) {
            Ability attackerAbility = abilityMapper.selectById(request.getAttackerAbilityId());
            if (attackerAbility != null) {
                String abilityEn = attackerAbility.getNameEn().toLowerCase();
                
                // 适应力（STAB增强）
                // 已在STAB计算中处理
                
                // 超能力（Psychic Surge）- 超能力场地
                if ("psychic-surge".equals(abilityEn) && request.getTerrain() == null) {
                    request.setTerrain(DamageCalculatorUtil.TERRAIN_PSYCHIC);
                }
                
                // 草地制造者（Grassy Surge）- 草地场地
                if ("grassy-surge".equals(abilityEn) && request.getTerrain() == null) {
                    request.setTerrain(DamageCalculatorUtil.TERRAIN_GRASSY);
                }
                
                // 电气制造者（Electric Surge）- 电气场地
                if ("electric-surge".equals(abilityEn) && request.getTerrain() == null) {
                    request.setTerrain(DamageCalculatorUtil.TERRAIN_ELECTRIC);
                }
                
                // 薄雾制造者（Misty Surge）- 薄雾场地
                if ("misty-surge".equals(abilityEn) && request.getTerrain() == null) {
                    request.setTerrain(DamageCalculatorUtil.TERRAIN_MISTY);
                }
                
                // 猛火（Blaze）- 火系技能增强（HP低于1/3时）
                if ("blaze".equals(abilityEn) && move.getTypeId() == DamageCalculatorUtil.TYPE_FIRE) {
                    if (request.getAttackerHpPercent() != null && request.getAttackerHpPercent() <= 33) {
                        multiplier *= 1.5;
                        request.setAttackerAbilityEffect("猛火：火系技能威力提升50%");
                    }
                }
                
                // 激流（Torrent）- 水系技能增强（HP低于1/3时）
                if ("torrent".equals(abilityEn) && move.getTypeId() == DamageCalculatorUtil.TYPE_WATER) {
                    if (request.getAttackerHpPercent() != null && request.getAttackerHpPercent() <= 33) {
                        multiplier *= 1.5;
                        request.setAttackerAbilityEffect("激流：水系技能威力提升50%");
                    }
                }
                
                // 虫之预感（Swarm）- 虫系技能增强（HP低于1/3时）
                if ("swarm".equals(abilityEn) && move.getTypeId() == DamageCalculatorUtil.TYPE_BUG) {
                    if (request.getAttackerHpPercent() != null && request.getAttackerHpPercent() <= 33) {
                        multiplier *= 1.5;
                        request.setAttackerAbilityEffect("虫之预感：虫系技能威力提升50%");
                    }
                }
                
                // 茂盛（Overgrow）- 草系技能增强（HP低于1/3时）
                if ("overgrow".equals(abilityEn) && move.getTypeId() == DamageCalculatorUtil.TYPE_GRASS) {
                    if (request.getAttackerHpPercent() != null && request.getAttackerHpPercent() <= 33) {
                        multiplier *= 1.5;
                        request.setAttackerAbilityEffect("茂盛：草系技能威力提升50%");
                    }
                }
                
                // 硬爪（Sharp Claws）- 接触技能增强30%
                if ("sharp-claws".equals(abilityEn) && isContactMove(move)) {
                    multiplier *= 1.3;
                    request.setAttackerAbilityEffect("硬爪：接触技能威力提升30%");
                }
                
                // 铁拳（Iron Fist）- 拳类技能增强
                if ("iron-fist".equals(abilityEn) && isPunchMove(move)) {
                    multiplier *= 1.2;
                    request.setAttackerAbilityEffect("铁拳：拳类技能威力提升20%");
                }
                
                // 突击背心（Assault Vest）- 特防1.5倍（在防御计算中处理）
                
                // 巨力（Sheer Force）- 削减追加效果但提升30%威力
                
                // 技师（Technician）- 威力60以下的技能提升50%
                if ("technician".equals(abilityEn) && move.getPower() != null && move.getPower() <= 60) {
                    multiplier *= 1.5;
                    request.setAttackerAbilityEffect("技师：低威力技能威力提升50%");
                }
            }
        }
        
        if (request.getDefenderAbilityId() != null) {
            Ability defenderAbility = abilityMapper.selectById(request.getDefenderAbilityId());
            if (defenderAbility != null) {
                String abilityEn = defenderAbility.getNameEn().toLowerCase();
                
                // 厚脂肪（Thick Fat）- 减弱火和冰属性技能
                if ("thick-fat".equals(abilityEn)) {
                    if (move.getTypeId() == DamageCalculatorUtil.TYPE_FIRE || 
                        move.getTypeId() == DamageCalculatorUtil.TYPE_ICE) {
                        multiplier *= 0.5;
                        request.setDefenderAbilityEffect("厚脂肪：火/冰系伤害减半");
                    }
                }
                
                // 多鳞（Multiscale）- 满HP时受到的伤害减半
                if ("multiscale".equals(abilityEn)) {
                    if (request.getDefenderHpPercent() != null && request.getDefenderHpPercent() >= 100) {
                        multiplier *= 0.5;
                        request.setDefenderAbilityEffect("多鳞：满HP时受到伤害减半");
                    }
                }
                
                // 柔软（Fur Coat）- 物理伤害减半
                if ("fur-coat".equals(abilityEn) && move.getDamageClassId() == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL) {
                    multiplier *= 0.5;
                    request.setDefenderAbilityEffect("柔软：物理伤害减半");
                }
                
                // 食草（Sap Sipper）- 免疫草系技能并提升攻击（在特殊处理中）
                
                // 引火（Flash Fire）- 免疫火系技能并提升火系威力（在特殊处理中）
                
                // 蓄电（Volt Absorb）- 免疫电系技能并回复HP（在特殊处理中）
                
                // 储水（Water Absorb）- 免疫水系技能并回复HP（在特殊处理中）
                
                // 奇异守护（Wonder Guard）- 只受弱点伤害（在特殊处理中）
                
                // 悖谬（Well-Baked Body）- 免疫火系技能并提升防御（在特殊处理中）
            }
        }
        
        return multiplier;
    }
    
    /**
     * 计算道具影响
     */
    private double calculateItemModifier(Move move, DamageCalculationRequest request,
                                        PokemonForm attacker, PokemonForm defender) {
        double multiplier = 1.0;
        
        if (request.getAttackerItemId() != null) {
            Item attackerItem = itemMapper.selectById(request.getAttackerItemId());
            if (attackerItem != null) {
                String itemNameEn = attackerItem.getNameEn().toLowerCase();
                
                // 生命宝珠（Life Orb）- 所有技能威力提升30%，但消耗10%HP
                if ("life-orb".equals(itemNameEn)) {
                    multiplier *= 1.3;
                    request.setAttackerItemEffect("生命宝珠：技能威力提升30%（消耗10%HP）");
                }
                
                // 讲究眼镜（Choice Specs）- 特攻提升50%，但只能使用一个技能
                if ("choice-specs".equals(itemNameEn) && move.getDamageClassId() == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL) {
                    multiplier *= 1.5;
                    request.setAttackerItemEffect("讲究眼镜：特攻技能威力提升50%");
                }
                
                // 讲究围巾（Choice Scarf）- 速度提升50%，但只能使用一个技能（不影响伤害）
                
                // 讲究头带（Choice Band）- 攻击提升50%，但只能使用一个技能
                if ("choice-band".equals(itemNameEn) && move.getDamageClassId() == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL) {
                    multiplier *= 1.5;
                    request.setAttackerItemEffect("讲究头带：物理技能威力提升50%");
                }
                
                // 达人带（Expert Belt）- 效果拔群时威力提升20%
                // 已在属性相性计算中处理
                
                // 柔软沙子（Soft Sand）- 地面系技能威力提升20%
                if ("soft-sand".equals(itemNameEn) && move.getTypeId() == DamageCalculatorUtil.TYPE_GROUND) {
                    multiplier *= 1.2;
                    request.setAttackerItemEffect("柔软沙子：地面系技能威力提升20%");
                }
                
                // 奇异之石（Odd Incense）- 超能力系技能威力提升20%
                if ("odd-incense".equals(itemNameEn) && move.getTypeId() == DamageCalculatorUtil.TYPE_PSYCHIC) {
                    multiplier *= 1.2;
                    request.setAttackerItemEffect("奇异之石：超能力系技能威力提升20%");
                }
                
                // 黑铁球（Iron Ball）- 撒菱类技能威力提升（不影响普通伤害）
                
                // 剧毒珠（Toxic Orb）- 使携带者进入剧毒状态（不影响伤害）
                
                // 火焰珠（Flame Orb）- 使携带者进入灼伤状态（影响物理伤害）
                
                // 银粉（Silver Powder）- 虫系技能威力提升20%
                if ("silver-powder".equals(itemNameEn) && move.getTypeId() == DamageCalculatorUtil.TYPE_BUG) {
                    multiplier *= 1.2;
                    request.setAttackerItemEffect("银粉：虫系技能威力提升20%");
                }
                
                // 龙之牙（Dragon Fang）- 龙系技能威力提升20%
                if ("dragon-fang".equals(itemNameEn) && move.getTypeId() == DamageCalculatorUtil.TYPE_DRAGON) {
                    multiplier *= 1.2;
                    request.setAttackerItemEffect("龙之牙：龙系技能威力提升20%");
                }
                
                // 魔法镜（Magic Powder）- 超能力系技能威力提升20%
                if ("magic-powder".equals(itemNameEn) && move.getTypeId() == DamageCalculatorUtil.TYPE_PSYCHIC) {
                    multiplier *= 1.2;
                    request.setAttackerItemEffect("魔法镜：超能力系技能威力提升20%");
                }
                
                // 黑带（Black Belt）- 格斗系技能威力提升20%
                if ("black-belt".equals(itemNameEn) && move.getTypeId() == DamageCalculatorUtil.TYPE_FIGHTING) {
                    multiplier *= 1.2;
                    request.setAttackerItemEffect("黑带：格斗系技能威力提升20%");
                }
                
                // 磁铁（Magnet）- 电系技能威力提升20%
                if ("magnet".equals(itemNameEn) && move.getTypeId() == DamageCalculatorUtil.TYPE_ELECTRIC) {
                    multiplier *= 1.2;
                    request.setAttackerItemEffect("磁铁：电系技能威力提升20%");
                }
                
                // 神秘水滴（Mystic Water）- 水系技能威力提升20%
                if ("mystic-water".equals(itemNameEn) && move.getTypeId() == DamageCalculatorUtil.TYPE_WATER) {
                    multiplier *= 1.2;
                    request.setAttackerItemEffect("神秘水滴：水系技能威力提升20%");
                }
                
                // 锐利鸟嘴（Sharp Beak）- 飞行系技能威力提升20%
                if ("sharp-beak".equals(itemNameEn) && move.getTypeId() == DamageCalculatorUtil.TYPE_FLYING) {
                    multiplier *= 1.2;
                    request.setAttackerItemEffect("锐利鸟嘴：飞行系技能威力提升20%");
                }
                
                // 黑眼镜（Black Glasses）- 恶系技能威力提升20%
                if ("black-glasses".equals(itemNameEn) && move.getTypeId() == DamageCalculatorUtil.TYPE_DARK) {
                    multiplier *= 1.2;
                    request.setAttackerItemEffect("黑眼镜：恶系技能威力提升20%");
                }
                
                // 不融之冰（Never-Melt Ice）- 冰系技能威力提升20%
                if ("never-melt-ice".equals(itemNameEn) && move.getTypeId() == DamageCalculatorUtil.TYPE_ICE) {
                    multiplier *= 1.2;
                    request.setAttackerItemEffect("不融之冰：冰系技能威力提升20%");
                }
                
                // 毒针（Poison Barb）- 毒系技能威力提升20%
                if ("poison-barb".equals(itemNameEn) && move.getTypeId() == DamageCalculatorUtil.TYPE_POISON) {
                    multiplier *= 1.2;
                    request.setAttackerItemEffect("毒针：毒系技能威力提升20%");
                }
                
                // 神秘鳞片（Mystery Berry）- 水系技能威力提升20%
                if ("mystery-berry".equals(itemNameEn) && move.getTypeId() == DamageCalculatorUtil.TYPE_WATER) {
                    multiplier *= 1.2;
                    request.setAttackerItemEffect("神秘鳞片：水系技能威力提升20%");
                }
                
                // 木炭（Charcoal）- 火系技能威力提升20%
                if ("charcoal".equals(itemNameEn) && move.getTypeId() == DamageCalculatorUtil.TYPE_FIRE) {
                    multiplier *= 1.2;
                    request.setAttackerItemEffect("木炭：火系技能威力提升20%");
                }
                
                // 奇异叶子（Miracle Seed）- 草系技能威力提升20%
                if ("miracle-seed".equals(itemNameEn) && move.getTypeId() == DamageCalculatorUtil.TYPE_GRASS) {
                    multiplier *= 1.2;
                    request.setAttackerItemEffect("奇异叶子：草系技能威力提升20%");
                }
                
                // 岩石宝石（Rock Gem）- 岩石系技能威力提升30%
                if ("rock-gem".equals(itemNameEn) && move.getTypeId() == DamageCalculatorUtil.TYPE_ROCK) {
                    multiplier *= 1.3;
                    request.setAttackerItemEffect("岩石宝石：岩石系技能威力提升30%");
                }
                
                // 空贝（Sea Incense）- 水系技能威力提升20%
                if ("sea-incense".equals(itemNameEn) && move.getTypeId() == DamageCalculatorUtil.TYPE_WATER) {
                    multiplier *= 1.2;
                    request.setAttackerItemEffect("空贝：水系技能威力提升20%");
                }
                
                // 诅咒之符（Spell Tag）- 幽灵系技能威力提升20%
                if ("spell-tag".equals(itemNameEn) && move.getTypeId() == DamageCalculatorUtil.TYPE_GHOST) {
                    multiplier *= 1.2;
                    request.setAttackerItemEffect("诅咒之符：幽灵系技能威力提升20%");
                }
                
                // 光之石（Light Ball）- 皮卡丘特有，攻击和特攻翻倍（特殊处理）
                
                // 厚底靴（Heavy-Duty Boots）- 免疫撒菱等场地效果（不影响伤害）
            }
        }
        
        if (request.getDefenderItemId() != null) {
            Item defenderItem = itemMapper.selectById(request.getDefenderItemId());
            if (defenderItem != null) {
                String itemNameEn = defenderItem.getNameEn().toLowerCase();
                
                // 吃剩的东西（Leftovers）- 每回合回复1/16HP（不影响伤害）
                
                // 进化奇石（Eviolite）- 未进化的宝可梦防御和特防提升50%（在防御计算中处理）
                
                // 凸凸头盔（Rocky Helmet）- 受到接触伤害时对攻击方造成1/6HP伤害（副作用）
                
                // 蓄水头盔（Assault Vest）- 特防提升50%（在防御计算中处理）
                
                // 银粉（Silver Powder）- 减弱虫系技能（在防御计算中处理）
            }
        }
        
        return multiplier;
    }
    
    /**
     * 计算场地影响
     */
    private double calculateTerrainModifier(Move move, DamageCalculationRequest request,
                                           PokemonForm attacker, PokemonForm defender) {
        if (request.getTerrain() == null || request.getTerrain().equals(DamageCalculatorUtil.TERRAIN_NONE)) {
            return 1.0;
        }
        
        Integer moveTypeId = move.getTypeId();
        String terrain = request.getTerrain();
        
        // 电气场地增强电系技能
        if (terrain.equals(DamageCalculatorUtil.TERRAIN_ELECTRIC) && moveTypeId == DamageCalculatorUtil.TYPE_ELECTRIC) {
            return 1.3;
        }
        // 草地场地增强草系技能
        if (terrain.equals(DamageCalculatorUtil.TERRAIN_GRASSY) && moveTypeId == DamageCalculatorUtil.TYPE_GRASS) {
            return 1.3;
        }
        // 超能力场地增强超能力系技能
        if (terrain.equals(DamageCalculatorUtil.TERRAIN_PSYCHIC) && moveTypeId == DamageCalculatorUtil.TYPE_PSYCHIC) {
            return 1.3;
        }
        
        return 1.0;
    }
    
    /**
     * 计算命中率
     */
    private void calculateAccuracy(Move move, DamageCalculationRequest request, DamageResultVO result) {
        int baseAccuracy = move.getAccuracy() != null ? move.getAccuracy() : 100;
        result.setBaseAccuracy(baseAccuracy);
        
        // 计算能力等级修正
        double accuracyBoost = DamageCalculatorUtil.getStageMultiplier(request.getAttackerAccuracyBoost());
        double evasionBoost = DamageCalculatorUtil.getStageMultiplier(request.getDefenderEvasionBoost());
        
        double finalAccuracy = (baseAccuracy / 100.0) * accuracyBoost / evasionBoost;
        
        // 天气影响（沙暴降低非岩石系命中率）
        if (request.getWeather() != null && request.getWeather().equals(DamageCalculatorUtil.WEATHER_SANDSTORM)) {
            // 这里需要检查防御方是否为岩石系
            finalAccuracy *= 0.8;
        }
        
        finalAccuracy = Math.min(1.0, Math.max(0.0, finalAccuracy));
        
        result.setFinalAccuracy(finalAccuracy);
        result.setAccuracyDesc(String.format("%.1f%%", finalAccuracy * 100));
    }
    
    /**
     * 计算击杀预估（优化版，考虑随机性）
     */
    private void calculateKoEstimate(int[] damageRange, int defenderHp, DamageResultVO result) {
        DamageResultVO.KOsEstimate koEstimate = new DamageResultVO.KOsEstimate();
        koEstimate.setDefenderHp(defenderHp);
        
        int minDamage = damageRange[0];
        int maxDamage = damageRange[1];
        
        // 最小伤害需要的攻击次数（最坏情况）
        int minHits = (int) Math.ceil((double) defenderHp / maxDamage);
        // 最大伤害需要的攻击次数（最好情况）
        int maxHits = (int) Math.ceil((double) defenderHp / minDamage);
        // 平均伤害需要的攻击次数
        double avgDamage = (minDamage + maxDamage) / 2.0;
        double avgHits = Math.ceil(defenderHp / avgDamage);
        
        koEstimate.setMinHits(minHits);
        koEstimate.setMaxHits(maxHits);
        koEstimate.setAvgHits(avgHits);
        
        // 使用蒙特卡洛模拟计算击杀概率
        double koChance = calculateKoChanceMonteCarlo(minDamage, maxDamage, defenderHp, 10000);
        
        koEstimate.setKoChance(koChance);
        
        // 计算百分比范围
        double koChanceMin = calculateKoChanceMonteCarlo(minDamage, maxDamage, defenderHp, 10000);
        double koChanceMax = calculateKoChanceMonteCarlo(minDamage, maxDamage, defenderHp, 10000);
        
        koEstimate.setKoPercentRange(String.format("%.1f%% - %.1f%%", koChanceMin * 100, koChanceMax * 100));
        
        result.setKoEstimate(koEstimate);
    }
    
    /**
     * 使用蒙特卡洛模拟计算击杀概率
     */
    private double calculateKoChanceMonteCarlo(int minDamage, int maxDamage, int defenderHp, 
                                              int simulations) {
        int koCount = 0;
        
        for (int i = 0; i < simulations; i++) {
            int totalDamage = 0;
            int hits = 0;
            
            while (totalDamage < defenderHp && hits < 4) { // 最多4次攻击
                // 生成随机伤害
                double randomDamage = minDamage + Math.random() * (maxDamage - minDamage);
                totalDamage += (int) Math.floor(randomDamage);
                hits++;
            }
            
            if (totalDamage >= defenderHp) {
                koCount++;
            }
        }
        
        return (double) koCount / simulations;
    }
    
    /**
     * 获取属性相性描述
     */
    private String getEffectivenessDesc(double effectiveness) {
        if (effectiveness >= 4.0) return "4x";
        if (effectiveness >= 2.0) return "2x";
        if (effectiveness >= 1.0) return "1x";
        if (effectiveness >= 0.5) return "0.5x";
        if (effectiveness >= 0.25) return "0.25x";
        return "0x";
    }
    
    /**
     * 获取伤害类型名称
     */
    private String getDamageClassName(Integer damageClassId) {
        if (damageClassId == null) return "Unknown";
        switch (damageClassId) {
            case DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL: return "Physical";
            case DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL: return "Special";
            case DamageCalculatorUtil.DAMAGE_CLASS_STATUS: return "Status";
            default: return "Unknown";
        }
    }
    
    /**
     * 判断是否是接触技能
     */
    private boolean isContactMove(Move move) {
        // 这里需要从数据库获取技能的接触属性
        // 简化实现，常见接触技能
        if (move.getNameEn() == null) return false;
        
        String nameEn = move.getNameEn().toLowerCase();
        
        // 常见接触技能列表
        String[] contactMoves = {
            "tackle", "scratch", "pound", "slam", "headbutt", "tackle",
            "body-slam", "take-down", "double-edge", "submission", "seismic-toss",
            "quick-attack", "mach-punch", "bullet-punch", "shadow-punch", "ice-punch",
            "fire-punch", "thunder-punch", "drain-punch", "dynamic-punch", "sky-uppercut",
            "iron-head", "zen-headbutt", "wild-charge", "flare-blitz", "volt-tackle",
            "brave-bird", "double-edge", "wood-hammer", "leaf-blade", "night-slash",
            "cross-chop", "close-combat", "superpower", "giga-impact", "hyper-beam",
            "explosion", "self-destruct", "head-smash", "stone-edge", "rock-slide"
        };
        
        for (String contactMove : contactMoves) {
            if (nameEn.equals(contactMove)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 判断是否是拳类技能
     */
    private boolean isPunchMove(Move move) {
        if (move.getNameEn() == null) return false;
        
        String nameEn = move.getNameEn().toLowerCase();
        
        return nameEn.contains("punch") || nameEn.equals("mach-punch") || 
               nameEn.equals("bullet-punch") || nameEn.equals("shadow-punch") ||
               nameEn.equals("ice-punch") || nameEn.equals("fire-punch") ||
               nameEn.equals("thunder-punch") || nameEn.equals("drain-punch") ||
               nameEn.equals("dynamic-punch") || nameEn.equals("sky-uppercut") ||
               nameEn.equals("hammer-arm") || nameEn.equals("arm-thrust");
    }
    
    /**
     * 判断是否是连续攻击技能
     */
    private int getHits(Move move) {
        if (move.getNameEn() == null) return 1;
        
        String nameEn = move.getNameEn().toLowerCase();
        
        switch (nameEn) {
            case "double-slap":
            case "comet-punch":
            case "arm-thrust":
            case "pin-missile":
            case "bullet-seed":
                return 2;
            case "rock-blast":
            case "icicle-spear":
                return 3;
            case "spike-cannon":
            case "sludge-bomb":
                return 5;
            case "fury-attack":
            case "fury-swipes":
            case "bone-rush":
                return 5;
            case "double-hit":
                return 2;
            case "triple-axel":
                return 3;
            case "multi-hit":
                return 3;
            case "population-bomb":
                return 10;
            default:
                return 1;
        }
    }
    
    /**
     * 计算太晶化效果（第9世代）
     */
    private double calculateTerastalizationModifier(Move move, DamageCalculationRequest request,
                                                   PokemonForm attacker, PokemonForm defender) {
        if (request.getAttackerTerastalType() == null) {
            return 1.0;
        }
        
        Integer teraType = request.getAttackerTerastalType();
        
        // 太晶化后技能变为太晶属性时的STAB计算
        if (move.getTypeId().equals(teraType)) {
            // 太晶STAB始终是2.0倍（即使不是本系）
            return 2.0 / DamageCalculatorUtil.STAB_MULTIPLIER; // 返回修正倍率
        }
        
        return 1.0;
    }
}