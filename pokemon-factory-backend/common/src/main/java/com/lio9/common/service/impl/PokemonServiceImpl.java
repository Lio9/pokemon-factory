package com.lio9.common.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.common.mapper.*;
import com.lio9.common.model.*;
import com.lio9.common.model.Ability;
import com.lio9.common.model.Type;
import com.lio9.common.service.PokemonService;
import com.lio9.common.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 宝可梦服务实现
 */
@Service
public class PokemonServiceImpl extends ServiceImpl<PokemonMapper, Pokemon> implements PokemonService {

    @Autowired
    private PokemonMapper pokemonMapper;
    
    @Autowired
    private PokemonFormMapper pokemonFormMapper;
    
    @Autowired
    private PokemonFormTypeMapper pokemonFormTypeMapper;
    
    @Autowired
    private PokemonFormAbilityMapper pokemonFormAbilityMapper;
    
    @Autowired
    private PokemonFormStatMapper pokemonFormStatMapper;
    
    @Autowired
    private PokemonEggGroupMapper pokemonEggGroupMapper;
    
    @Autowired
    private EggGroupMapper eggGroupMapper;
    
    @Autowired
    private GrowthRateMapper growthRateMapper;
    
    @Autowired
    private AbilityMapper abilityMapper;
    
    @Autowired
    private TypeMapper typeMapper;
    
    @Autowired
    private EvolutionChainMapper evolutionChainMapper;
    
    @Autowired
    private PokemonMoveMapper pokemonMoveMapper;
    
    @Autowired
    private MoveMapper moveMapper;

    @Override
    public long count() {
        return count();
    }

    @Override
    public void removeAll() {
        remove(null);
    }

    @Override
    public PokemonDetailVO getDetailById(Long id) {
        // 1. 获取物种基本信息
        Pokemon pokemon = getById(id);
        if (pokemon == null) {
            return null;
        }
        
        PokemonDetailVO detailVO = new PokemonDetailVO();
        detailVO.setId(pokemon.getId());
        detailVO.setName(pokemon.getName());
        detailVO.setNameEn(pokemon.getNameEn());
        detailVO.setNameJp(pokemon.getNameJp());
        detailVO.setGenus(pokemon.getGenus());
        detailVO.setDescription(pokemon.getDescription());
        detailVO.setGenerationId(pokemon.getGenerationId());
        detailVO.setIsLegendary(pokemon.getIsLegendary());
        detailVO.setIsMythical(pokemon.getIsMythical());
        detailVO.setIsBaby(pokemon.getIsBaby());
        detailVO.setCaptureRate(pokemon.getCaptureRate());
        detailVO.setBaseHappiness(pokemon.getBaseHappiness());
        detailVO.setGenderRate(pokemon.getGenderRate());
        detailVO.setHatchCounter(pokemon.getHatchCounter());
        
        // 2. 获取成长类型
        if (pokemon.getGrowthRateId() != null) {
            GrowthRate growthRate = growthRateMapper.selectById(pokemon.getGrowthRateId());
            if (growthRate != null) {
                detailVO.setGrowthRate(growthRate.getName());
            }
        }
        
        // 3. 获取蛋群
        QueryWrapper<PokemonEggGroup> eggGroupWrapper = new QueryWrapper<>();
        eggGroupWrapper.eq("pokemon_id", id);
        List<PokemonEggGroup> pokemonEggGroups = pokemonEggGroupMapper.selectList(eggGroupWrapper);
        if (!pokemonEggGroups.isEmpty()) {
            List<Long> eggGroupIds = pokemonEggGroups.stream()
                    .map(PokemonEggGroup::getEggGroupId)
                    .collect(Collectors.toList());
            QueryWrapper<com.lio9.common.model.EggGroup> egWrapper = new QueryWrapper<>();
            egWrapper.in("id", eggGroupIds);
            List<com.lio9.common.model.EggGroup> eggGroups = eggGroupMapper.selectList(egWrapper);
            detailVO.setEggGroups(eggGroups.stream()
                    .map(com.lio9.common.model.EggGroup::getName)
                    .collect(Collectors.toList()));
        }
        
        // 4. 获取形态列表
        List<PokemonForm> forms = pokemonFormMapper.selectBySpeciesId(id.intValue());
        if (!forms.isEmpty()) {
            List<PokemonFormDetailVO> formVOs = new ArrayList<>();
            List<Integer> formIds = forms.stream()
                    .map(PokemonForm::getId)
                    .collect(Collectors.toList());
            
            // 批量获取形态属性
            Map<Integer, List<TypeVO>> formTypesMap = new HashMap<>();
            if (!formIds.isEmpty()) {
                List<Map<String, Object>> typesData = pokemonFormTypeMapper.selectTypesByFormIds(formIds);
                for (Map<String, Object> typeData : typesData) {
                    Integer formId = (Integer) typeData.get("form_id");
                    TypeVO typeVO = new TypeVO();
                    typeVO.setId((Integer) typeData.get("type_id"));
                    typeVO.setName((String) typeData.get("name"));
                    typeVO.setNameEn((String) typeData.get("name_en"));
                    typeVO.setColor((String) typeData.get("color"));
                    
                    formTypesMap.computeIfAbsent(formId, k -> new ArrayList<>()).add(typeVO);
                }
            }
            
            // 批量获取形态特性
            Map<Integer, List<AbilityVO>> formAbilitiesMap = new HashMap<>();
            if (!formIds.isEmpty()) {
                QueryWrapper<PokemonFormAbility> abilityWrapper = new QueryWrapper<>();
                abilityWrapper.in("form_id", formIds);
                List<PokemonFormAbility> formAbilities = pokemonFormAbilityMapper.selectList(abilityWrapper);
                
                for (PokemonFormAbility formAbility : formAbilities) {
                    Integer formId = formAbility.getFormId();
                    Ability ability = abilityMapper.selectById(formAbility.getAbilityId());
                    if (ability != null) {
                        AbilityVO abilityVO = new AbilityVO();
                        abilityVO.setId(ability.getId());
                        abilityVO.setName(ability.getName());
                        abilityVO.setNameEn(ability.getNameEn());
                        abilityVO.setDescription(ability.getDescription());
                        abilityVO.setIsHidden(formAbility.getIsHidden());
                        abilityVO.setSlot(formAbility.getSlot());
                        
                        formAbilitiesMap.computeIfAbsent(formId, k -> new ArrayList<>()).add(abilityVO);
                    }
                }
            }
            
            // 获取每个形态的种族值
            Map<Integer, StatVO> formStatsMap = new HashMap<>();
            if (!formIds.isEmpty()) {
                QueryWrapper<PokemonFormStat> statWrapper = new QueryWrapper<>();
                statWrapper.in("form_id", formIds);
                List<PokemonFormStat> formStats = pokemonFormStatMapper.selectList(statWrapper);
                
                Map<Integer, List<PokemonFormStat>> statsByForm = formStats.stream()
                        .collect(Collectors.groupingBy(PokemonFormStat::getFormId));
                
                for (Map.Entry<Integer, List<PokemonFormStat>> entry : statsByForm.entrySet()) {
                    Integer formId = entry.getKey();
                    List<PokemonFormStat> stats = entry.getValue();
                    
                    StatVO statVO = new StatVO();
                    for (PokemonFormStat stat : stats) {
                        switch (stat.getStatId()) {
                            case 1: statVO.setHp(stat.getBaseStat()); break;
                            case 2: statVO.setAttack(stat.getBaseStat()); break;
                            case 3: statVO.setDefense(stat.getBaseStat()); break;
                            case 4: statVO.setSpAttack(stat.getBaseStat()); break;
                            case 5: statVO.setSpDefense(stat.getBaseStat()); break;
                            case 6: statVO.setSpeed(stat.getBaseStat()); break;
                        }
                    }
                    formStatsMap.put(formId, statVO);
                }
            }
            
            // 构建形态VO列表
            for (PokemonForm form : forms) {
                PokemonFormDetailVO formVO = new PokemonFormDetailVO();
                formVO.setId(form.getId());
                formVO.setFormName(form.getFormNameZh() != null ? form.getFormNameZh() : form.getFormName());
                formVO.setIsDefault(form.getIsDefault());
                formVO.setIsMega(form.getIsMega());
                formVO.setIsGigantamax(form.getIsGigantamax());
                formVO.setHeight(form.getHeight() != null ? form.getHeight().doubleValue() : null);
                formVO.setWeight(form.getWeight() != null ? form.getWeight().doubleValue() : null);
                formVO.setBaseExperience(form.getBaseExperience());
                formVO.setSpriteUrl(form.getSpriteUrl());
                formVO.setSpriteBackUrl(form.getSpriteBackUrl());
                formVO.setSpriteShinyUrl(form.getSpriteShinyUrl());
                formVO.setOfficialArtworkUrl(form.getOfficialArtworkUrl());
                
                // 设置属性
                formVO.setTypes(formTypesMap.getOrDefault(form.getId(), new ArrayList<>()));
                
                // 设置特性
                formVO.setAbilities(formAbilitiesMap.getOrDefault(form.getId(), new ArrayList<>()));
                
                // 设置种族值
                formVO.setStats(formStatsMap.get(form.getId()));
                
                formVOs.add(formVO);
            }
            
            detailVO.setForms(formVOs);
        }
        
        return detailVO;
    }

    @Override
    public Page<Pokemon> searchPokemon(String keyword, Page<Pokemon> page) {
        QueryWrapper<Pokemon> wrapper = new QueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("name", keyword).or().like("name_en", keyword));
        }
        
        wrapper.orderByAsc("id");
        return page(page, wrapper);
    }

    @Override
    public Pokemon getByIndexNumber(String indexNumber) {
        QueryWrapper<Pokemon> wrapper = new QueryWrapper<>();
        wrapper.eq("id", indexNumber);
        return getOne(wrapper);
    }

    @Override
    public List<EvolutionVO> getEvolutionChain(Long pokemonId) {
        // 1. 获取宝可梦物种信息
        Pokemon pokemon = getById(pokemonId);
        if (pokemon == null || pokemon.getEvolutionChainId() == null) {
            return new ArrayList<>();
        }
        
        // 2. 查询同一进化链的所有宝可梦
        QueryWrapper<Pokemon> wrapper = new QueryWrapper<>();
        wrapper.eq("evolution_chain_id", pokemon.getEvolutionChainId());
        wrapper.orderByAsc("id");
        List<Pokemon> speciesList = list(wrapper);
        
        if (speciesList.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 3. 构建进化链VO列表
        List<EvolutionVO> evolutionVOs = new ArrayList<>();
        
        for (Pokemon species : speciesList) {
            EvolutionVO vo = new EvolutionVO();
            vo.setSpeciesId(species.getId());
            vo.setPokemonName(species.getName());
            vo.setIsCurrent(species.getId().equals(pokemonId.intValue()));
            
            // 获取进化触发条件
            if (species.getEvolvesFromSpeciesId() != null) {
                QueryWrapper<EvolutionChain> evWrapper = new QueryWrapper<>();
                evWrapper.eq("pokemon_id", species.getId());
                EvolutionChain evolutionChain = evolutionChainMapper.selectOne(evWrapper);
                
                if (evolutionChain != null) {
                    vo.setTrigger(evolutionChain.getEvolutionMethod());
                    vo.setMinLevel(parseEvolutionLevel(evolutionChain.getEvolutionValue()));
                    vo.setItem(evolutionChain.getEvolutionParameter());
                }
            }
            
            // 获取默认形态的图片
            QueryWrapper<PokemonForm> formWrapper = new QueryWrapper<>();
            formWrapper.eq("species_id", species.getId());
            formWrapper.eq("is_default", 1);
            PokemonForm defaultForm = pokemonFormMapper.selectOne(formWrapper);
            if (defaultForm != null) {
                vo.setSpriteUrl(defaultForm.getSpriteUrl());
            }
            
            evolutionVOs.add(vo);
        }
        
        return evolutionVOs;
    }
    
    /**
     * 解析进化等级
     */
    private Integer parseEvolutionLevel(String evolutionValue) {
        if (evolutionValue == null) {
            return null;
        }
        try {
            return Integer.parseInt(evolutionValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public List<Move> getMoves(Long pokemonId) {
        // 1. 获取宝可梦的技能关联
        QueryWrapper<PokemonMove> wrapper = new QueryWrapper<>();
        wrapper.eq("pokemon_id", pokemonId);
        List<PokemonMove> pokemonMoves = pokemonMoveMapper.selectList(wrapper);
        
        if (pokemonMoves.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 2. 获取技能列表
        List<Long> moveIds = pokemonMoves.stream()
                .map(PokemonMove::getMoveId)
                .distinct()
                .collect(Collectors.toList());
        
        if (moveIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        QueryWrapper<Move> moveWrapper = new QueryWrapper<>();
        moveWrapper.in("id", moveIds);
        moveWrapper.orderByAsc("id");
        
        return moveMapper.selectList(moveWrapper);
    }
}