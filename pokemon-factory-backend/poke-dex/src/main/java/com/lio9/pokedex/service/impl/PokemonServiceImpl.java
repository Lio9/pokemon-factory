package com.lio9.pokedex.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.pokedex.model.Ability;
import com.lio9.pokedex.model.EvolutionChain;
import com.lio9.pokedex.model.GrowthRate;
import com.lio9.pokedex.model.Move;
import com.lio9.pokedex.model.Pokemon;
import com.lio9.pokedex.model.PokemonEggGroup;
import com.lio9.pokedex.model.PokemonForm;
import com.lio9.pokedex.model.PokemonFormAbility;
import com.lio9.pokedex.model.PokemonFormStat;
import com.lio9.pokedex.model.PokemonMove;
import com.lio9.pokedex.mapper.AbilityMapper;
import com.lio9.pokedex.mapper.EggGroupMapper;
import com.lio9.pokedex.mapper.EvolutionChainMapper;
import com.lio9.pokedex.mapper.GrowthRateMapper;
import com.lio9.pokedex.mapper.MoveMapper;
import com.lio9.pokedex.mapper.PokemonEggGroupMapper;
import com.lio9.pokedex.mapper.PokemonFormAbilityMapper;
import com.lio9.pokedex.mapper.PokemonFormMapper;
import com.lio9.pokedex.mapper.PokemonFormStatMapper;
import com.lio9.pokedex.mapper.PokemonFormTypeMapper;
import com.lio9.pokedex.mapper.PokemonMapper;
import com.lio9.pokedex.mapper.PokemonMoveMapper;
import com.lio9.pokedex.service.PokemonService;
import com.lio9.pokedex.vo.AbilityVO;
import com.lio9.pokedex.vo.EvolutionVO;
import com.lio9.pokedex.vo.PokemonDetailVO;
import com.lio9.pokedex.vo.PokemonFormDetailVO;
import com.lio9.pokedex.vo.StatVO;
import com.lio9.pokedex.vo.TypeVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 宝可梦服务实现
 */
@Service
public class PokemonServiceImpl extends ServiceImpl<PokemonMapper, Pokemon> implements PokemonService {
    private final PokemonFormMapper pokemonFormMapper;
    private final PokemonFormTypeMapper pokemonFormTypeMapper;
    private final PokemonFormAbilityMapper pokemonFormAbilityMapper;
    private final PokemonFormStatMapper pokemonFormStatMapper;
    private final PokemonEggGroupMapper pokemonEggGroupMapper;
    private final EggGroupMapper eggGroupMapper;
    private final GrowthRateMapper growthRateMapper;
    private final AbilityMapper abilityMapper;
    private final EvolutionChainMapper evolutionChainMapper;
    private final PokemonMoveMapper pokemonMoveMapper;
    private final MoveMapper moveMapper;

    public PokemonServiceImpl(
        PokemonFormMapper pokemonFormMapper,
        PokemonFormTypeMapper pokemonFormTypeMapper,
        PokemonFormAbilityMapper pokemonFormAbilityMapper,
        PokemonFormStatMapper pokemonFormStatMapper,
        PokemonEggGroupMapper pokemonEggGroupMapper,
        EggGroupMapper eggGroupMapper,
        GrowthRateMapper growthRateMapper,
        AbilityMapper abilityMapper,
        EvolutionChainMapper evolutionChainMapper,
        PokemonMoveMapper pokemonMoveMapper,
        MoveMapper moveMapper
    ) {
        this.pokemonFormMapper = pokemonFormMapper;
        this.pokemonFormTypeMapper = pokemonFormTypeMapper;
        this.pokemonFormAbilityMapper = pokemonFormAbilityMapper;
        this.pokemonFormStatMapper = pokemonFormStatMapper;
        this.pokemonEggGroupMapper = pokemonEggGroupMapper;
        this.eggGroupMapper = eggGroupMapper;
        this.growthRateMapper = growthRateMapper;
        this.abilityMapper = abilityMapper;
        this.evolutionChainMapper = evolutionChainMapper;
        this.pokemonMoveMapper = pokemonMoveMapper;
        this.moveMapper = moveMapper;
    }

    @Override
    public long count() {
        return super.count();
    }

    @Override
    public void removeAll() {
        remove(null);
    }

    @Override
    public PokemonDetailVO getDetailById(Long id) {
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
        
        if (pokemon.getGrowthRateId() != null) {
            GrowthRate growthRate = growthRateMapper.selectById(pokemon.getGrowthRateId());
            if (growthRate != null) {
                detailVO.setGrowthRate(growthRate.getName());
            }
        }
        
        QueryWrapper<PokemonEggGroup> eggGroupWrapper = new QueryWrapper<>();
        eggGroupWrapper.eq("pokemon_id", id);
        List<PokemonEggGroup> pokemonEggGroups = pokemonEggGroupMapper.selectList(eggGroupWrapper);
        if (!pokemonEggGroups.isEmpty()) {
            List<Long> eggGroupIds = pokemonEggGroups.stream()
                    .map(PokemonEggGroup::getEggGroupId)
                    .collect(Collectors.toList());
            QueryWrapper<com.lio9.pokedex.model.EggGroup> egWrapper = new QueryWrapper<>();
            egWrapper.in("id", eggGroupIds);
            List<com.lio9.pokedex.model.EggGroup> eggGroups = eggGroupMapper.selectList(egWrapper);
            detailVO.setEggGroups(eggGroups.stream()
                    .map(com.lio9.pokedex.model.EggGroup::getName)
                    .collect(Collectors.toList()));
        }
        
        List<PokemonForm> forms = pokemonFormMapper.selectBySpeciesId(id.intValue());
        if (!forms.isEmpty()) {
            List<PokemonFormDetailVO> formVOs = new ArrayList<>();
            List<Integer> formIds = forms.stream()
                    .map(PokemonForm::getId)
                    .collect(Collectors.toList());
            
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
                    
                    formTypesMap.computeIfAbsent(formId, key -> new ArrayList<>()).add(typeVO);
                }
            }
            
            Map<Integer, List<AbilityVO>> formAbilitiesMap = new HashMap<>();
            if (!formIds.isEmpty()) {
                QueryWrapper<PokemonFormAbility> abilityWrapper = new QueryWrapper<>();
                abilityWrapper.in("form_id", formIds);
                List<PokemonFormAbility> formAbilities = pokemonFormAbilityMapper.selectList(abilityWrapper);
                
                Set<Integer> abilityIds = formAbilities.stream()
                        .map(PokemonFormAbility::getAbilityId)
                        .collect(Collectors.toSet());
                
                Map<Integer, Ability> abilityMap = new HashMap<>();
                if (!abilityIds.isEmpty()) {
                    List<Ability> abilities = abilityMapper.selectByIds(abilityIds);
                    abilityMap = abilities.stream()
                            .collect(Collectors.toMap(Ability::getId, ability -> ability));
                }
                
                for (PokemonFormAbility formAbility : formAbilities) {
                    Integer formId = formAbility.getFormId();
                    Ability ability = abilityMap.get(formAbility.getAbilityId());
                    if (ability != null) {
                        AbilityVO abilityVO = new AbilityVO();
                        abilityVO.setId(ability.getId());
                        abilityVO.setName(ability.getName());
                        abilityVO.setNameEn(ability.getNameEn());
                        abilityVO.setDescription(ability.getDescription());
                        abilityVO.setIsHidden(formAbility.getIsHidden());
                        abilityVO.setSlot(formAbility.getSlot());
                        
                        formAbilitiesMap.computeIfAbsent(formId, key -> new ArrayList<>()).add(abilityVO);
                    }
                }
            }
            
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
                            default: break;
                        }
                    }
                    formStatsMap.put(formId, statVO);
                }
            }
            
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
                formVO.setTypes(formTypesMap.getOrDefault(form.getId(), new ArrayList<>()));
                formVO.setAbilities(formAbilitiesMap.getOrDefault(form.getId(), new ArrayList<>()));
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
            wrapper.and(query -> query.like("name", keyword).or().like("name_en", keyword));
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
        Pokemon pokemon = getById(pokemonId);
        if (pokemon == null || pokemon.getEvolutionChainId() == null) {
            return new ArrayList<>();
        }
        
        QueryWrapper<Pokemon> wrapper = new QueryWrapper<>();
        wrapper.eq("evolution_chain_id", pokemon.getEvolutionChainId());
        wrapper.orderByAsc("id");
        List<Pokemon> speciesList = list(wrapper);
        
        if (speciesList.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<EvolutionVO> evolutionVOs = new ArrayList<>();
        
        for (Pokemon species : speciesList) {
            EvolutionVO vo = new EvolutionVO();
            vo.setSpeciesId(species.getId());
            vo.setPokemonName(species.getName());
            vo.setIsCurrent(species.getId().equals(pokemonId.intValue()));
            
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
        QueryWrapper<PokemonMove> wrapper = new QueryWrapper<>();
        wrapper.eq("pokemon_id", pokemonId);
        List<PokemonMove> pokemonMoves = pokemonMoveMapper.selectList(wrapper);
        
        if (pokemonMoves.isEmpty()) {
            return new ArrayList<>();
        }
        
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
