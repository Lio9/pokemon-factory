package com.lio9.pokedex.service.impl;

import com.lio9.common.mapper.*;
import com.lio9.common.service.DamageCalculatorService;
import com.lio9.common.vo.DamageCalculationRequest;
import com.lio9.common.vo.DamageResultVO;
import com.lio9.common.vo.TypeEfficacyVO;
import com.lio9.pokedex.service.impl.EffectCalculatorService.BattleContext;
import com.lio9.pokedex.service.impl.EffectCalculatorService.EffectResult;
import com.lio9.pokedex.util.DamageCalculatorUtil;
import com.lio9.pokedex.util.MoveEffects;
import com.lio9.pokedex.util.MoveEffects.MoveEffect;
import com.lio9.pokedex.util.MoveEffects.MoveEffectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.lio9.pokedex.util.DamageCalculatorUtil.*;

/**
 * 伤害计算器服务实现类
 * 基于宝可梦标准伤害公式计算
 * 
 * 公式: Damage = ((2 * Level / 5 + 2) * Power * Attack / Defense / 50 + 2) 
 *        * Modifiers * Random(0.85-1.0)
 */
@Service
public class DamageCalculatorServiceImpl implements DamageCalculatorService {

    @Autowired
    private TypeEfficacyMapper typeEfficacyMapper;
    
    @Autowired
    private MoveMapper moveMapper;
    
    @Autowired
    private PokemonFormTypeMapper formTypeMapper;
    
    @Autowired
    private PokemonFormStatMapper formStatMapper;
    
    @Autowired
    private MoveFlagMapper moveFlagMapper;
    
    @Autowired
    private EffectCalculatorService effectCalculator;
    
    // 缓存属性相性矩阵
    private Map<Integer, Map<Integer, Integer>> efficacyMatrix;
    
    @Override
    public DamageResultVO calculateDamage(DamageCalculationRequest request) {
        DamageResultVO result = new DamageResultVO();
        List<DamageResultVO.CalculationStep> steps = new ArrayList<>();
        Map<String, Double> allMultipliers = new LinkedHashMap<>();
        Map<String, Object> debugInfo = new HashMap<>();
        
        // ==================== 1. 获取技能信息 ====================
        Map<String, Object> moveData = moveMapper.selectMoveDetailById(request.getMoveId());
        if (moveData == null) {
            throw new IllegalArgumentException("技能不存在");
        }
        
        Integer movePower = (Integer) moveData.get("power");
        Integer moveTypeId = (Integer) moveData.get("type_id");
        String damageClass = (String) moveData.get("damage_class_name");
        String moveTypeName = (String) moveData.get("type_name");
        
        result.setMovePower(movePower);
        result.setDamageClass(damageClass);
        result.setMoveTypeName(moveTypeName);
        
        debugInfo.put("moveData", moveData);
        
        // 变化技能不造成伤害
        if (movePower == null || movePower == 0) {
            result.setMinDamage(0);
            result.setMaxDamage(0);
            result.setAvgDamage(0.0);
            result.setTypeEffectiveness(0.0);
            result.setEffectivenessDesc("变化技能无伤害");
            return result;
        }
        
        // ==================== 1.5 检查技能特殊效果 ====================
        List<MoveEffect> moveEffects = MoveEffects.getEffects(request.getMoveId());
        debugInfo.put("moveEffects", moveEffects.stream().map(e -> e.description).toList());
        
        // 检查固定伤害技能
        MoveEffect fixedDamageEffect = MoveEffects.getEffectByType(request.getMoveId(), MoveEffectType.FIXED_DAMAGE);
        if (fixedDamageEffect != null) {
            int fixedDamage = (int) fixedDamageEffect.baseValue;
            result.setMinDamage(fixedDamage);
            result.setMaxDamage(fixedDamage);
            result.setAvgDamage((double) fixedDamage);
            result.setBaseDamage(fixedDamage);
            result.setTypeEffectiveness(1.0);
            result.setEffectivenessDesc("固定伤害技能");
            addStep(steps, "固定伤害", null, (double) fixedDamage, fixedDamageEffect.description, "special");
            return result;
        }
        
        // 检查等级相关伤害
        MoveEffect levelDamageEffect = MoveEffects.getEffectByType(request.getMoveId(), MoveEffectType.LEVEL_DAMAGE);
        if (levelDamageEffect != null) {
            int level = request.getAttackerLevel() != null ? request.getAttackerLevel() : 50;
            result.setMinDamage(level);
            result.setMaxDamage(level);
            result.setAvgDamage((double) level);
            result.setBaseDamage(level);
            result.setTypeEffectiveness(1.0);
            result.setEffectivenessDesc("等级相关伤害");
            addStep(steps, "等级伤害", "等级 " + level, (double) level, levelDamageEffect.description, "special");
            return result;
        }
        
        boolean isPhysical = "物理".equals(damageClass) || "physical".equalsIgnoreCase(damageClass);
        
        // ==================== 2. 构建战斗上下文 ====================
        BattleContext ctx = buildBattleContext(request, moveData);
        
        // ==================== 3. 计算属性相性 ====================
        Double typeEffectiveness = effectCalculator.calculateTypeEffectiveness(
            moveTypeId, ctx.defenderTypeIds, getTypeEfficacyMatrix());
        ctx.typeEffectiveness = typeEffectiveness;
        
        // 检查技能属性相性覆盖（如冷冻干燥对水属性效果绝佳）
        MoveEffect typeOverrideEffect = MoveEffects.getEffectByType(request.getMoveId(), MoveEffectType.TYPE_EFFECTIVENESS_OVERRIDE);
        if (typeOverrideEffect != null && typeOverrideEffect.targetTypeIds != null) {
            for (Integer targetTypeId : typeOverrideEffect.targetTypeIds) {
                if (ctx.defenderTypeIds.contains(targetTypeId)) {
                    // 覆盖对该属性的相性
                    typeEffectiveness = typeOverrideEffect.multiplier;
                    ctx.typeEffectiveness = typeEffectiveness;
                    addStep(steps, "技能特效", null, typeOverrideEffect.multiplier, 
                        typeOverrideEffect.description, "move");
                    break;
                }
            }
        }
        
        result.setTypeEffectiveness(typeEffectiveness);
        result.setEffectivenessDesc(getEffectivenessDesc(typeEffectiveness));
        
        addStep(steps, "属性相性", null, typeEffectiveness, 
                typeEffectiveness == 0 ? "无效" : typeEffectiveness + "x", "type");
        allMultipliers.put("属性相性", typeEffectiveness);
        
        // 无效攻击直接返回
        if (typeEffectiveness == 0) {
            result.setMinDamage(0);
            result.setMaxDamage(0);
            result.setAvgDamage(0.0);
            result.setBaseDamage(0);
            result.setAllMultipliers(allMultipliers);
            result.setDebugInfo(debugInfo);
            return result;
        }
        
        // ==================== 4. 计算本系加成(STAB) ====================
        boolean isStab = ctx.attackerTypeIds.contains(moveTypeId);
        ctx.typeEffectiveness = typeEffectiveness;
        
        // ==================== 5. 计算所有效果 ====================
        EffectResult effectResult = effectCalculator.calculateAllEffects(ctx);
        
        // 更新本系加成
        double stabMultiplier = isStab ? effectResult.stabMultiplier : 1.0;
        result.setIsStab(isStab);
        result.setStabMultiplier(stabMultiplier);
        
        String stabDesc = isStab ? 
            (effectResult.stabMultiplier == 2.0 ? "适应力特性: 本系加成2x" : "本系技能: 威力提升50%") : 
            "非本系技能";
        addStep(steps, "本系加成", null, stabMultiplier, stabDesc, "stab");
        allMultipliers.put("本系加成", stabMultiplier);
        
        // ==================== 6. 计算能力值 ====================
        int level = request.getAttackerLevel() != null ? request.getAttackerLevel() : 50;
        
        int baseAttack, baseDefense;
        int attackBoost, defenseBoost;
        
        // 检查是否使用防御值计算特殊伤害（如精神冲击）
        boolean useDefenseStat = MoveEffects.usesDefenseStat(request.getMoveId());
        MoveEffect defenseStatEffect = MoveEffects.getEffectByType(request.getMoveId(), MoveEffectType.USE_DEFENSE_STAT);
        
        if (isPhysical) {
            baseAttack = request.getAttackerAttack() != null ? 
                request.getAttackerAttack() : getFormStat(request.getAttackerFormId(), 2);
            // 特殊技能：特殊攻击使用目标防御值
            if (useDefenseStat && defenseStatEffect != null && "use_defense".equals(defenseStatEffect.condition)) {
                baseDefense = request.getDefenderDefense() != null ? 
                    request.getDefenderDefense() : getFormStat(request.getDefenderFormId(), 3);
                addStep(steps, "特殊计算", null, 1.0, "使用目标防御值计算伤害", "special");
            } else {
                baseDefense = request.getDefenderDefense() != null ? 
                    request.getDefenderDefense() : getFormStat(request.getDefenderFormId(), 3);
            }
            attackBoost = request.getAttackerAttackBoost() != null ? request.getAttackerAttackBoost() : 0;
            defenseBoost = request.getDefenderDefenseBoost() != null ? request.getDefenderDefenseBoost() : 0;
            result.setUsedAttackType("attack");
        } else {
            baseAttack = request.getAttackerSpAttack() != null ? 
                request.getAttackerSpAttack() : getFormStat(request.getAttackerFormId(), 4);
            // 特殊技能：特殊攻击使用目标防御值
            if (useDefenseStat && defenseStatEffect != null && "use_defense".equals(defenseStatEffect.condition)) {
                baseDefense = request.getDefenderDefense() != null ? 
                    request.getDefenderDefense() : getFormStat(request.getDefenderFormId(), 3);
                addStep(steps, "特殊计算", null, 1.0, "使用目标防御值而非特防值计算伤害", "special");
            } else {
                baseDefense = request.getDefenderSpDefense() != null ? 
                    request.getDefenderSpDefense() : getFormStat(request.getDefenderFormId(), 5);
            }
            attackBoost = request.getAttackerSpAttackBoost() != null ? request.getAttackerSpAttackBoost() : 0;
            defenseBoost = request.getDefenderSpDefenseBoost() != null ? request.getDefenderSpDefenseBoost() : 0;
            result.setUsedAttackType("spAttack");
        }
        
        // 应用能力等级修正
        double attackBoostMultiplier = getStatBoostMultiplier(attackBoost);
        double defenseBoostMultiplier = getStatBoostMultiplier(defenseBoost);
        
        // 天真特性忽略防御提升
        if (effectResult.ignoreDefenseBoost && defenseBoost > 0) {
            defenseBoostMultiplier = 1.0;
            addStep(steps, "能力等级", "天真特性忽略防御提升", 1.0, "天真特性：忽略防御等级提升", "boost");
        }
        
        // 暴击时忽略防御方正面的防御等级提升
        if (Boolean.TRUE.equals(request.getIsCritical()) && defenseBoost > 0) {
            defenseBoostMultiplier = 1.0;
            addStep(steps, "能力等级", "暴击忽略防御提升", 1.0, "暴击忽略防御方能力等级提升", "boost");
        }
        
        // 应用特性攻击修正
        double abilityAttackMultiplier = isPhysical ? effectResult.attackMultiplier : effectResult.spAttackMultiplier;
        double abilityDefenseMultiplier = isPhysical ? effectResult.defenseMultiplier : effectResult.spDefenseMultiplier;
        
        // 最终能力值
        int effectiveAttack = (int) Math.floor(baseAttack * attackBoostMultiplier * abilityAttackMultiplier);
        int effectiveDefense = (int) Math.floor(baseDefense * defenseBoostMultiplier * abilityDefenseMultiplier);
        
        result.setUsedAttackStat(effectiveAttack);
        result.setUsedDefenseStat(effectiveDefense);
        result.setBoostMultiplier(attackBoostMultiplier);
        
        if (attackBoost != 0) {
            addStep(steps, "攻击等级", 
                String.format("%d × %.2f", baseAttack, attackBoostMultiplier),
                (double) effectiveAttack, 
                attackBoost > 0 ? "攻击+" + attackBoost : "攻击" + attackBoost, 
                "boost");
        }
        
        debugInfo.put("baseAttack", baseAttack);
        debugInfo.put("baseDefense", baseDefense);
        debugInfo.put("effectiveAttack", effectiveAttack);
        debugInfo.put("effectiveDefense", effectiveDefense);
        debugInfo.put("abilityAttackMultiplier", abilityAttackMultiplier);
        debugInfo.put("abilityDefenseMultiplier", abilityDefenseMultiplier);
        
        // ==================== 6.5 技能威力变化 ====================
        double movePowerMultiplier = 1.0;
        int effectiveMovePower = movePower;
        
        // 检查对异常状态目标威力提升的技能（如祸不单行）
        MoveEffect statusBoostEffect = MoveEffects.getEffectByType(request.getMoveId(), MoveEffectType.POWER_BOOST_STATUS);
        if (statusBoostEffect != null) {
            boolean defenderHasStatus = Boolean.TRUE.equals(request.getDefenderBurned()) ||
                                       Boolean.TRUE.equals(request.getDefenderPoisoned()) ||
                                       Boolean.TRUE.equals(request.getDefenderParalyzed());
            if (defenderHasStatus) {
                movePowerMultiplier *= statusBoostEffect.multiplier;
                effectiveMovePower = (int) Math.floor(movePower * movePowerMultiplier);
                addStep(steps, "技能效果", null, statusBoostEffect.multiplier, 
                    statusBoostEffect.description, "move");
            }
        }
        
        // 检查低血量威力提升的技能（如硬撑）
        MoveEffect lowHpEffect = MoveEffects.getEffectByType(request.getMoveId(), MoveEffectType.POWER_BOOST_LOW_HP);
        if (lowHpEffect != null) {
            int attackerHpPercent = request.getAttackerHpPercent() != null ? request.getAttackerHpPercent() : 100;
            if (attackerHpPercent <= 33) {  // HP <= 1/3
                movePowerMultiplier *= lowHpEffect.multiplier;
                effectiveMovePower = (int) Math.floor(movePower * movePowerMultiplier);
                addStep(steps, "技能效果", null, lowHpEffect.multiplier, 
                    "低血量: " + lowHpEffect.description, "move");
            }
        }
        
        allMultipliers.put("技能威力", movePowerMultiplier);
        
        // ==================== 7. 计算基础伤害 ====================
        int baseDamage = calculateBaseDamage(level, effectiveMovePower, effectiveAttack, effectiveDefense);
        result.setBaseDamage(baseDamage);
        
        addStep(steps, "基础伤害", 
            String.format("((2×%d÷5+2)×%d×%d÷%d÷50+2)", level, movePower, effectiveAttack, effectiveDefense),
            (double) baseDamage, null, "base");
        
        // ==================== 8. 天气修正 ====================
        Double weatherMultiplier = calculateWeatherMultiplier(request.getWeather(), moveTypeId);
        result.setWeatherMultiplier(weatherMultiplier);
        allMultipliers.put("天气", weatherMultiplier);
        
        if (weatherMultiplier != 1.0) {
            String weatherDesc = request.getWeather() + 
                (weatherMultiplier > 1 ? ": 威力提升" : ": 威力降低");
            addStep(steps, "天气修正", null, weatherMultiplier, weatherDesc, "weather");
        }
        
        // ==================== 9. 场地修正 ====================
        Double terrainMultiplier = calculateTerrainMultiplier(request.getTerrain(), moveTypeId);
        allMultipliers.put("场地", terrainMultiplier);
        
        if (terrainMultiplier != 1.0) {
            addStep(steps, "场地修正", null, terrainMultiplier, 
                request.getTerrain() + ": 威力提升", "terrain");
        }
        
        // ==================== 10. 特性/道具效果 ====================
        // 汇总特性效果
        double abilityPowerMultiplier = effectResult.powerMultiplier;
        double abilityDamageMultiplier = effectResult.damageMultiplier;
        
        result.setAbilityMultiplier(abilityPowerMultiplier * abilityDamageMultiplier);
        
        // 添加特性效果描述
        if (!effectResult.attackerEffects.isEmpty()) {
            result.setAttackerAbilityEffect(String.join("; ", effectResult.attackerEffects));
            for (String effect : effectResult.attackerEffects) {
                addStep(steps, "攻击方特性", null, abilityPowerMultiplier, effect, "ability");
            }
        }
        if (!effectResult.defenderEffects.isEmpty()) {
            result.setDefenderAbilityEffect(String.join("; ", effectResult.defenderEffects));
            for (String effect : effectResult.defenderEffects) {
                addStep(steps, "防御方特性", null, abilityDamageMultiplier, effect, "ability");
            }
        }
        
        allMultipliers.put("特性威力", abilityPowerMultiplier);
        allMultipliers.put("特性防御", abilityDamageMultiplier);
        
        // 汇总道具效果
        if (!effectResult.itemEffects.isEmpty()) {
            result.setAttackerItemEffect(String.join("; ", effectResult.itemEffects));
            for (String effect : effectResult.itemEffects) {
                addStep(steps, "道具效果", null, effectResult.damageMultiplier, effect, "item");
            }
        }
        
        allMultipliers.put("道具", effectResult.damageMultiplier);
        
        // ==================== 11. 暴击修正 ====================
        Double criticalMultiplier = effectResult.criticalMultiplier;
        result.setCriticalMultiplier(criticalMultiplier);
        
        if (criticalMultiplier > 1.0) {
            addStep(steps, "暴击修正", null, 1.5, "暴击: 伤害提升50%", "critical");
        }
        allMultipliers.put("暴击", criticalMultiplier);
        
        // ==================== 12. 屏幕修正 ====================
        Double screenMultiplier = calculateScreenMultiplier(
            isPhysical,
            Boolean.TRUE.equals(request.getReflectActive()),
            Boolean.TRUE.equals(request.getLightScreenActive()),
            Boolean.TRUE.equals(request.getAuroraVeilActive()),
            Boolean.TRUE.equals(request.getIsDoubleBattle()),
            Boolean.TRUE.equals(request.getIsCritical())
        );
        result.setScreenMultiplier(screenMultiplier);
        
        if (screenMultiplier != 1.0) {
            String screenDesc = "";
            if (Boolean.TRUE.equals(request.getAuroraVeilActive())) {
                screenDesc = "极光幕: 伤害减少" + (int)((1-screenMultiplier)*100) + "%";
            } else if (isPhysical && Boolean.TRUE.equals(request.getReflectActive())) {
                screenDesc = "反射壁: 物理伤害减少" + (int)((1-screenMultiplier)*100) + "%";
            } else if (!isPhysical && Boolean.TRUE.equals(request.getLightScreenActive())) {
                screenDesc = "光墙: 特殊伤害减少" + (int)((1-screenMultiplier)*100) + "%";
            }
            addStep(steps, "屏幕修正", null, screenMultiplier, screenDesc, "screen");
        }
        allMultipliers.put("屏幕", screenMultiplier);
        
        // ==================== 13. 灼伤修正 ====================
        Double burnMultiplier = calculateBurnMultiplier(
            isPhysical, 
            Boolean.TRUE.equals(request.getAttackerBurned()),
            request.getAttackerAbilityId()
        );
        result.setBurnMultiplier(burnMultiplier);
        
        if (burnMultiplier != 1.0) {
            addStep(steps, "灼伤修正", null, 0.5, "灼伤: 物理攻击减半", "status");
        }
        allMultipliers.put("灼伤", burnMultiplier);
        
        // ==================== 14. 计算最终伤害 ====================
        result.setRandomRange("85% - 100%");
        
        // 计算总修正倍率
        double totalMultiplier = stabMultiplier * weatherMultiplier * terrainMultiplier *
            abilityPowerMultiplier * abilityDamageMultiplier * criticalMultiplier * 
            screenMultiplier * burnMultiplier * typeEffectiveness;
        
        debugInfo.put("totalMultiplier", totalMultiplier);
        
        // 计算伤害范围
        double modifiedDamage = baseDamage * totalMultiplier;
        
        int minDamage = Math.max(1, (int) Math.floor(modifiedDamage * 0.85));
        int maxDamage = Math.max(1, (int) Math.floor(modifiedDamage * 1.0));
        double avgDamage = modifiedDamage * 0.925;
        
        result.setMinDamage(minDamage);
        result.setMaxDamage(maxDamage);
        result.setAvgDamage(avgDamage);
        
        addStep(steps, "最终伤害", 
            String.format("%.0f × 0.85~1.0", modifiedDamage),
            avgDamage, String.format("%d ~ %d", minDamage, maxDamage), "final");
        
        // ==================== 15. 计算击杀预估 ====================
        Integer defenderHp = request.getDefenderHp();
        if (defenderHp == null) {
            defenderHp = getFormStat(request.getDefenderFormId(), 1);
        }
        
        DamageResultVO.KOsEstimate koEstimate = calculateKOEstimate(
            defenderHp, minDamage, maxDamage, avgDamage);
        result.setKoEstimate(koEstimate);
        
        // 设置结果
        result.setCalculationSteps(steps);
        result.setAllMultipliers(allMultipliers);
        result.setDebugInfo(debugInfo);
        
        return result;
    }
    
    /**
     * 构建战斗上下文
     */
    private BattleContext buildBattleContext(DamageCalculationRequest request, Map<String, Object> moveData) {
        BattleContext ctx = new BattleContext();
        
        // 攻击方信息
        ctx.attackerAbilityId = request.getAttackerAbilityId();
        ctx.attackerItemId = request.getAttackerItemId();
        ctx.attackerFormId = request.getAttackerFormId();
        ctx.attackerLevel = request.getAttackerLevel() != null ? request.getAttackerLevel() : 50;
        ctx.attackerHpPercent = request.getAttackerHpPercent() != null ? request.getAttackerHpPercent() : 100;
        ctx.attackerBurned = Boolean.TRUE.equals(request.getAttackerBurned());
        ctx.attackerPoisoned = Boolean.TRUE.equals(request.getAttackerPoisoned());
        ctx.attackerParalyzed = Boolean.TRUE.equals(request.getAttackerParalyzed());
        ctx.attackerAttackBoost = request.getAttackerAttackBoost() != null ? request.getAttackerAttackBoost() : 0;
        ctx.attackerSpAttackBoost = request.getAttackerSpAttackBoost() != null ? request.getAttackerSpAttackBoost() : 0;
        
        // 防御方信息
        ctx.defenderAbilityId = request.getDefenderAbilityId();
        ctx.defenderItemId = request.getDefenderItemId();
        ctx.defenderFormId = request.getDefenderFormId();
        ctx.defenderHpPercent = request.getDefenderHpPercent() != null ? request.getDefenderHpPercent() : 100;
        ctx.defenderDefenseBoost = request.getDefenderDefenseBoost() != null ? request.getDefenderDefenseBoost() : 0;
        ctx.defenderSpDefenseBoost = request.getDefenderSpDefenseBoost() != null ? request.getDefenderSpDefenseBoost() : 0;
        
        // 获取攻击方属性
        List<Map<String, Object>> attackerTypes = formTypeMapper.selectTypesByFormIds(
            Collections.singletonList(request.getAttackerFormId()));
        for (Map<String, Object> type : attackerTypes) {
            ctx.attackerTypeIds.add((Integer) type.get("type_id"));
        }
        
        // 获取防御方属性
        List<Map<String, Object>> defenderTypes = formTypeMapper.selectTypesByFormIds(
            Collections.singletonList(request.getDefenderFormId()));
        for (Map<String, Object> type : defenderTypes) {
            ctx.defenderTypeIds.add((Integer) type.get("type_id"));
        }
        
        // 技能信息
        ctx.moveId = request.getMoveId();
        ctx.moveTypeId = (Integer) moveData.get("type_id");
        ctx.movePower = (Integer) moveData.get("power");
        ctx.damageClass = (String) moveData.get("damage_class_name");
        
        // 获取技能标记（接触、拳击、咬、声音等）
        try {
            List<Integer> flagIds = moveFlagMapper.selectFlagIdsByMoveId(request.getMoveId());
            if (flagIds != null) {
                ctx.isContactMove = flagIds.contains(MoveFlagMapper.FLAG_CONTACT);
                ctx.isPunchingMove = flagIds.contains(MoveFlagMapper.FLAG_PUNCH);
                ctx.isBitingMove = flagIds.contains(MoveFlagMapper.FLAG_BITE);
                ctx.isSoundMove = flagIds.contains(MoveFlagMapper.FLAG_SOUND);
                ctx.isPulseMove = flagIds.contains(MoveFlagMapper.FLAG_PULSE);
                ctx.isBulletMove = flagIds.contains(MoveFlagMapper.FLAG_BALLISTICS);
            }
        } catch (Exception e) {
            // 表可能不存在，使用默认值
        }
        
        // 战斗状态
        ctx.weather = request.getWeather();
        ctx.terrain = request.getTerrain();
        ctx.isDoubleBattle = Boolean.TRUE.equals(request.getIsDoubleBattle());
        ctx.isCritical = Boolean.TRUE.equals(request.getIsCritical());
        ctx.reflectActive = Boolean.TRUE.equals(request.getReflectActive());
        ctx.lightScreenActive = Boolean.TRUE.equals(request.getLightScreenActive());
        ctx.auroraVeilActive = Boolean.TRUE.equals(request.getAuroraVeilActive());
        
        return ctx;
    }
    
    @Override
    public Map<Integer, Map<Integer, Integer>> getTypeEfficacyMatrix() {
        if (efficacyMatrix != null) {
            return efficacyMatrix;
        }
        
        efficacyMatrix = new HashMap<>();
        List<Map<String, Object>> allEfficacy = typeEfficacyMapper.selectAllTypeEfficacy();
        
        for (Map<String, Object> e : allEfficacy) {
            Integer damageTypeId = (Integer) e.get("damage_type_id");
            Integer targetTypeId = (Integer) e.get("target_type_id");
            Integer damageFactor = (Integer) e.get("damage_factor");
            
            efficacyMatrix.computeIfAbsent(damageTypeId, k -> new HashMap<>())
                    .put(targetTypeId, damageFactor);
        }
        
        return efficacyMatrix;
    }
    
    @Override
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
        }).collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    public Double calculateTypeEffectiveness(Integer moveTypeId, List<Integer> targetTypeIds) {
        Map<Integer, Map<Integer, Integer>> matrix = getTypeEfficacyMatrix();
        
        double effectiveness = 1.0;
        Map<Integer, Integer> moveEfficacy = matrix.get(moveTypeId);
        
        if (moveEfficacy == null) {
            return effectiveness;
        }
        
        for (Integer targetTypeId : targetTypeIds) {
            Integer factor = moveEfficacy.get(targetTypeId);
            if (factor != null) {
                effectiveness *= factor / 100.0;
            }
        }
        
        return effectiveness;
    }
    
    // ==================== 私有辅助方法 ====================
    
    private Integer getFormStat(Integer formId, Integer statId) {
        Map<String, Object> stat = formStatMapper.selectByFormIdAndStatId(formId, statId);
        if (stat != null && stat.get("base_stat") != null) {
            return (Integer) stat.get("base_stat");
        }
        return 100;
    }
    
    private void addStep(List<DamageResultVO.CalculationStep> steps, 
                         String name, String formula, Double value, 
                         String description, String category) {
        DamageResultVO.CalculationStep step = new DamageResultVO.CalculationStep();
        step.setName(name);
        step.setFormula(formula);
        step.setValue(value);
        step.setDescription(description);
        step.setCategory(category);
        steps.add(step);
    }
    
    private DamageResultVO.KOsEstimate calculateKOEstimate(
            Integer defenderHp, Integer minDamage, Integer maxDamage, Double avgDamage) {
        
        DamageResultVO.KOsEstimate estimate = new DamageResultVO.KOsEstimate();
        estimate.setDefenderHp(defenderHp);
        
        if (avgDamage <= 0) {
            estimate.setMinHits(Integer.MAX_VALUE);
            estimate.setMaxHits(Integer.MAX_VALUE);
            estimate.setAvgHits(Double.MAX_VALUE);
            estimate.setKoChance(0.0);
            estimate.setKoPercentRange("无法击杀");
            return estimate;
        }
        
        int minHits = (int) Math.ceil(defenderHp / (double) maxDamage);
        int maxHits = (int) Math.ceil(defenderHp / (double) minDamage);
        double avgHits = defenderHp / avgDamage;
        
        estimate.setMinHits(minHits);
        estimate.setMaxHits(maxHits);
        estimate.setAvgHits(avgHits);
        
        // 计算伤害百分比范围
        double minPercent = (minDamage * 100.0) / defenderHp;
        double maxPercent = (maxDamage * 100.0) / defenderHp;
        estimate.setKoPercentRange(String.format("%.1f%% - %.1f%%", minPercent, maxPercent));
        
        // 计算一击击杀概率
        double koChance = maxDamage >= defenderHp ? 100.0 : 0.0;
        estimate.setKoChance(koChance);
        
        return estimate;
    }
}
