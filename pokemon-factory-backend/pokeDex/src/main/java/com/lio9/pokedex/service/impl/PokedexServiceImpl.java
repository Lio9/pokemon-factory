package com.lio9.pokedex.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.common.mapper.*;
import com.lio9.common.model.*;
import com.lio9.common.service.PokedexService;
import com.lio9.common.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 图鉴服务实现类 - 优化版
 * 解决N+1查询问题，提升性能
 */
@Service
public class PokedexServiceImpl implements PokedexService {

    @Autowired
    private PokemonSpeciesMapper speciesMapper;
    
    @Autowired
    private PokemonFormMapper formMapper;
    
    @Autowired
    private PokemonFormTypeMapper formTypeMapper;
    
    @Autowired
    private PokemonFormAbilityMapper formAbilityMapper;
    
    @Autowired
    private PokemonFormStatMapper formStatMapper;
    
    @Autowired
    private TypeMapper typeMapper;
    
    @Autowired
    private AbilityMapper abilityMapper;
    
    @Autowired
    private MoveMapper moveMapper;
    
    @Autowired
    private ItemMapper itemMapper;
    
    @Autowired
    private PokemonEvolutionMapper evolutionMapper;

    @Override
    public Page<PokemonListVO> getPokemonList(int current, int size, Integer typeId, Integer generationId, String keyword) {
        Page<PokemonListVO> result = new Page<>(current, size);
        
        // 基础查询条件
        QueryWrapper<Pokemon> query = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            query.and(w -> w.like("name", keyword).or().like("name_en", keyword));
        }
        if (generationId != null) {
            query.eq("generation_id", generationId);
        }
        query.orderByAsc("id");
        
        // 如果有属性筛选，使用子查询
        if (typeId != null) {
            query.exists(
                "SELECT 1 FROM pokemon_form pf " +
                "INNER JOIN pokemon_form_type pft ON pft.form_id = pf.id " +
                "WHERE pf.species_id = pokemon_species.id AND pf.is_default = 1 AND pft.type_id = " + typeId
            );
        }
        
        // 分页查询物种
        Page<Pokemon> page = new Page<>(current, size);
        page = speciesMapper.selectPage(page, query);
        
        if (page.getRecords().isEmpty()) {
            result.setRecords(Collections.emptyList());
            result.setTotal(0);
            return result;
        }
        
        // 批量获取物种ID
        List<Integer> speciesIds = page.getRecords().stream()
                .map(Pokemon::getId)
                .collect(Collectors.toList());
        
        // 批量查询默认形态
        List<Map<String, Object>> forms = formMapper.selectDefaultFormsBySpeciesIds(speciesIds);
        Map<Integer, Map<String, Object>> formMap = forms.stream()
                .collect(Collectors.toMap(f -> (Integer) f.get("species_id"), f -> f, (a, b) -> a));
        
        // 批量获取形态ID
        List<Integer> formIds = forms.stream()
                .map(f -> (Integer) f.get("id"))
                .collect(Collectors.toList());
        
        // 批量查询形态属性
        Map<Integer, List<TypeVO>> typeMap = new HashMap<>();
        if (!formIds.isEmpty()) {
            List<Map<String, Object>> formTypes = formTypeMapper.selectTypesByFormIds(formIds);
            for (Map<String, Object> ft : formTypes) {
                Integer formId = (Integer) ft.get("form_id");
                TypeVO vo = new TypeVO();
                vo.setId((Integer) ft.get("type_id"));
                vo.setName((String) ft.get("name"));
                vo.setNameEn((String) ft.get("name_en"));
                vo.setColor((String) ft.get("color"));
                typeMap.computeIfAbsent(formId, k -> new ArrayList<>()).add(vo);
            }
        }
        
        // 组装VO
        List<PokemonListVO> voList = page.getRecords().stream().map(species -> {
            PokemonListVO vo = new PokemonListVO();
            vo.setId(species.getId());
            vo.setName(species.getName());
            vo.setNameEn(species.getNameEn());
            vo.setGenus(species.getGenus());
            vo.setIsLegendary(species.getIsLegendary());
            vo.setIsMythical(species.getIsMythical());
            vo.setGenerationId(species.getGenerationId());
            
            Map<String, Object> form = formMap.get(species.getId());
            if (form != null) {
                vo.setDefaultFormId((Integer) form.get("id"));
                vo.setSpriteUrl((String) form.get("sprite_url"));
                vo.setOfficialArtworkUrl((String) form.get("official_artwork_url"));
                
                Integer formId = (Integer) form.get("id");
                List<TypeVO> types = typeMap.getOrDefault(formId, Collections.emptyList());
                vo.setTypes(types);
            }
            
            return vo;
        }).collect(Collectors.toList());
        
        result.setRecords(voList);
        result.setTotal(page.getTotal());
        
        return result;
    }

    @Override
    public PokemonDetailVO getPokemonDetail(Integer speciesId) {
        Pokemon species = speciesMapper.selectById(speciesId);
        if (species == null) {
            return null;
        }
        
        PokemonDetailVO vo = new PokemonDetailVO();
        vo.setId(species.getId());
        vo.setName(species.getName());
        vo.setNameEn(species.getNameEn());
        vo.setNameJp(species.getNameJp());
        vo.setGenus(species.getGenus());
        vo.setDescription(species.getDescription());
        vo.setGenerationId(species.getGenerationId());
        vo.setIsLegendary(species.getIsLegendary());
        vo.setIsMythical(species.getIsMythical());
        vo.setIsBaby(species.getIsBaby());
        vo.setCaptureRate(species.getCaptureRate());
        vo.setBaseHappiness(species.getBaseHappiness());
        vo.setGenderRate(species.getGenderRate());
        vo.setHatchCounter(species.getHatchCounter());
        
        List<PokemonFormDetailVO> forms = getFormsBySpeciesId(speciesId);
        vo.setForms(forms);
        
        List<EvolutionChainVO> evolutionChain = getEvolutionChain(species.getEvolutionChainId(), speciesId);
        vo.setEvolutionChain(evolutionChain);
        
        return vo;
    }

    @Override
    public List<MoveVO> getFormMoves(Integer formId, Integer versionGroupId) {
        List<Map<String, Object>> moves = moveMapper.selectMovesByFormId(formId);
        
        return moves.stream().map(m -> {
            MoveVO vo = new MoveVO();
            vo.setId((Integer) m.get("id"));
            vo.setName((String) m.get("name"));
            vo.setNameEn((String) m.get("name_en"));
            vo.setTypeName((String) m.get("type_name"));
            vo.setTypeColor((String) m.get("type_color"));
            vo.setDamageClass((String) m.get("damage_class_name"));
            vo.setPower((Integer) m.get("power"));
            vo.setAccuracy((Integer) m.get("accuracy"));
            vo.setPp((Integer) m.get("pp"));
            vo.setPriority((Integer) m.get("priority"));
            vo.setDescription((String) m.get("description"));
            vo.setLearnMethod((String) m.get("learn_method"));
            vo.setLevel((Integer) m.get("level"));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<TypeVO> getAllTypes() {
        List<Type> types = typeMapper.selectList(new QueryWrapper<Type>().orderByAsc("id"));
        return types.stream().map(t -> {
            TypeVO vo = new TypeVO();
            vo.setId(t.getId());
            vo.setName(t.getName());
            vo.setNameEn(t.getNameEn());
            vo.setColor(t.getColor());
            vo.setIconUrl(t.getIconUrl());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Page<AbilityVO> getAbilityList(int current, int size, String keyword) {
        Page<Ability> page = new Page<>(current, size);
        QueryWrapper<Ability> query = new QueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            query.and(w -> w.like("name", keyword).or().like("name_en", keyword));
        }
        query.orderByAsc("id");
        
        page = abilityMapper.selectPage(page, query);
        
        Page<AbilityVO> result = new Page<>(current, size);
        result.setTotal(page.getTotal());
        result.setRecords(page.getRecords().stream().map(a -> {
            AbilityVO vo = new AbilityVO();
            vo.setId(a.getId());
            vo.setName(a.getName());
            vo.setNameEn(a.getNameEn());
            vo.setDescription(a.getDescription());
            return vo;
        }).collect(Collectors.toList()));
        
        return result;
    }

    @Override
    public Page<MoveVO> getMoveList(int current, int size, Integer typeId, String keyword) {
        Page<Move> page = new Page<>(current, size);
        QueryWrapper<Move> query = new QueryWrapper<>();
        
        if (typeId != null) {
            query.eq("type_id", typeId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            query.and(w -> w.like("name", keyword).or().like("name_en", keyword));
        }
        query.orderByAsc("id");
        
        page = moveMapper.selectPage(page, query);
        
        Page<MoveVO> result = new Page<>(current, size);
        result.setTotal(page.getTotal());
        
        // 批量获取类型信息
        List<Integer> typeIds = page.getRecords().stream()
                .map(Move::getTypeId)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Integer, Type> typeMap = new HashMap<>();
        if (!typeIds.isEmpty()) {
            QueryWrapper<Type> typeQuery = new QueryWrapper<>();
            typeQuery.in("id", typeIds);
            typeMap = typeMapper.selectList(typeQuery).stream()
                    .collect(Collectors.toMap(Type::getId, t -> t));
        }
        
        final Map<Integer, Type> finalTypeMap = typeMap;
        result.setRecords(page.getRecords().stream().map(m -> {
            MoveVO vo = new MoveVO();
            vo.setId(m.getId());
            vo.setName(m.getName());
            vo.setNameEn(m.getNameEn());
            vo.setPower(m.getPower());
            vo.setAccuracy(m.getAccuracy());
            vo.setPp(m.getPp());
            vo.setPriority(m.getPriority());
            vo.setDescription(m.getDescription());
            
            Type type = finalTypeMap.get(m.getTypeId());
            if (type != null) {
                vo.setTypeName(type.getName());
                vo.setTypeColor(type.getColor());
            }
            
            return vo;
        }).collect(Collectors.toList()));
        
        return result;
    }

    @Override
    public Page<ItemVO> getItemList(int current, int size, Integer categoryId, String keyword) {
        Page<Item> page = new Page<>(current, size);
        QueryWrapper<Item> query = new QueryWrapper<>();
        
        if (categoryId != null) {
            query.eq("category_id", categoryId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            query.and(w -> w.like("name", keyword).or().like("name_en", keyword));
        }
        query.orderByAsc("id");
        
        page = itemMapper.selectPage(page, query);
        
        Page<ItemVO> result = new Page<>(current, size);
        result.setTotal(page.getTotal());
        result.setRecords(page.getRecords().stream().map(i -> {
            ItemVO vo = new ItemVO();
            vo.setId(i.getId());
            vo.setName(i.getName());
            vo.setNameEn(i.getNameEn());
            vo.setCost(i.getCost());
            vo.setDescription(i.getDescription());
            vo.setSpriteUrl(i.getSpriteUrl());
            return vo;
        }).collect(Collectors.toList()));
        
        return result;
    }
    
    // ==================== 私有方法 ====================
    
    private List<PokemonFormDetailVO> getFormsBySpeciesId(Integer speciesId) {
        QueryWrapper<PokemonForm> query = new QueryWrapper<>();
        query.eq("species_id", speciesId).orderByDesc("is_default").orderByAsc("id");
        List<PokemonForm> forms = formMapper.selectList(query);
        
        if (forms.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 批量获取形态ID
        List<Integer> formIds = forms.stream().map(PokemonForm::getId).collect(Collectors.toList());
        
        // 批量查询属性
        Map<Integer, List<TypeVO>> typeMap = new HashMap<>();
        List<Map<String, Object>> formTypes = formTypeMapper.selectTypesByFormIds(formIds);
        for (Map<String, Object> ft : formTypes) {
            Integer formId = (Integer) ft.get("form_id");
            TypeVO vo = new TypeVO();
            vo.setId((Integer) ft.get("type_id"));
            vo.setName((String) ft.get("name"));
            vo.setNameEn((String) ft.get("name_en"));
            vo.setColor((String) ft.get("color"));
            typeMap.computeIfAbsent(formId, k -> new ArrayList<>()).add(vo);
        }
        
        // 批量查询特性
        Map<Integer, List<AbilityVO>> abilityMap = new HashMap<>();
        for (Integer formId : formIds) {
            List<Map<String, Object>> abilities = abilityMapper.selectAbilitiesByFormId(formId);
            List<AbilityVO> abilityList = abilities.stream().map(a -> {
                AbilityVO vo = new AbilityVO();
                vo.setId((Integer) a.get("id"));
                vo.setName((String) a.get("name"));
                vo.setNameEn((String) a.get("name_en"));
                vo.setDescription((String) a.get("description"));
                vo.setIsHidden((Boolean) a.get("is_hidden"));
                vo.setSlot((Integer) a.get("slot"));
                return vo;
            }).collect(Collectors.toList());
            abilityMap.put(formId, abilityList);
        }
        
        // 批量查询种族值
        QueryWrapper<PokemonFormStat> statQuery = new QueryWrapper<>();
        statQuery.in("form_id", formIds);
        List<PokemonFormStat> allStats = formStatMapper.selectList(statQuery);
        Map<Integer, StatVO> statMap = new HashMap<>();
        for (PokemonFormStat stat : allStats) {
            Integer formId = stat.getFormId();
            StatVO vo = statMap.computeIfAbsent(formId, k -> new StatVO());
            int value = stat.getBaseStat();
            
            switch (stat.getStatId()) {
                case 1: vo.setHp(value); break;
                case 2: vo.setAttack(value); break;
                case 3: vo.setDefense(value); break;
                case 4: vo.setSpAttack(value); break;
                case 5: vo.setSpDefense(value); break;
                case 6: vo.setSpeed(value); break;
            }
            vo.setTotal(vo.getTotal() != null ? vo.getTotal() + value : value);
        }
        
        // 组装结果
        final Map<Integer, List<TypeVO>> finalTypeMap = typeMap;
        final Map<Integer, List<AbilityVO>> finalAbilityMap = abilityMap;
        final Map<Integer, StatVO> finalStatMap = statMap;
        
        return forms.stream().map(form -> {
            PokemonFormDetailVO vo = new PokemonFormDetailVO();
            vo.setId(form.getId());
            vo.setFormName(form.getFormNameZh() != null ? form.getFormNameZh() : form.getFormName());
            vo.setIsDefault(form.getIsDefault());
            vo.setIsMega(form.getIsMega());
            vo.setIsGigantamax(form.getIsGigantamax());
            vo.setHeight(form.getHeight() != null ? form.getHeight().doubleValue() : null);
            vo.setWeight(form.getWeight() != null ? form.getWeight().doubleValue() : null);
            vo.setBaseExperience(form.getBaseExperience());
            vo.setSpriteUrl(form.getSpriteUrl());
            vo.setSpriteBackUrl(form.getSpriteBackUrl());
            vo.setSpriteShinyUrl(form.getSpriteShinyUrl());
            vo.setOfficialArtworkUrl(form.getOfficialArtworkUrl());
            
            vo.setTypes(finalTypeMap.getOrDefault(form.getId(), Collections.emptyList()));
            vo.setAbilities(finalAbilityMap.getOrDefault(form.getId(), Collections.emptyList()));
            vo.setStats(finalStatMap.get(form.getId()));
            
            return vo;
        }).collect(Collectors.toList());
    }
    
    private List<EvolutionChainVO> getEvolutionChain(Integer chainId, Integer currentSpeciesId) {
        try {
            if (chainId == null) {
                return Collections.emptyList();
            }
            
            QueryWrapper<Pokemon> query = new QueryWrapper<>();
            query.eq("evolution_chain_id", chainId).orderByAsc("id");
            List<Pokemon> speciesList = speciesMapper.selectList(query);
            
            if (speciesList == null || speciesList.isEmpty()) {
                return Collections.emptyList();
            }
            
            // 批量获取物种ID
            List<Integer> speciesIds = speciesList.stream()
                    .filter(s -> s != null && s.getId() != null)
                    .map(Pokemon::getId)
                    .collect(Collectors.toList());
            
            // 批量查询默认形态
            Map<Integer, String> spriteMap = new HashMap<>();
            try {
                List<Map<String, Object>> forms = formMapper.selectDefaultFormsBySpeciesIds(speciesIds);
                if (forms != null) {
                    spriteMap = forms.stream()
                            .filter(f -> f != null && f.get("species_id") != null)
                            .collect(Collectors.toMap(
                                f -> (Integer) f.get("species_id"),
                                f -> (String) f.get("sprite_url"),
                                (a, b) -> a != null ? a : b
                            ));
                }
            } catch (Exception e) {
                System.err.println("Error loading forms: " + e.getMessage());
            }
            
            // 批量查询进化信息
            Map<Integer, PokemonEvolution> evolutionMap = new HashMap<>();
            try {
                List<Integer> evolvedIds = speciesList.stream()
                        .filter(s -> s != null && s.getEvolvesFromSpeciesId() != null)
                        .map(Pokemon::getId)
                        .collect(Collectors.toList());
                
                if (!evolvedIds.isEmpty()) {
                    QueryWrapper<PokemonEvolution> evQuery = new QueryWrapper<>();
                    evQuery.in("evolved_species_id", evolvedIds);
                    List<PokemonEvolution> evolutions = evolutionMapper.selectList(evQuery);
                    // 添加 null 检查和过滤，避免 NullPointerException
                    if (evolutions != null && !evolutions.isEmpty()) {
                        evolutionMap = evolutions.stream()
                                .filter(e -> e != null && e.getEvolvedSpeciesId() != null)
                                .collect(Collectors.toMap(
                                    PokemonEvolution::getEvolvedSpeciesId,
                                    e -> e,
                                    (a, b) -> a
                                ));
                    }
                }
            } catch (Exception e) {
                System.err.println("Error loading evolutions: " + e.getMessage());
            }
            
            final Map<Integer, String> finalSpriteMap = spriteMap;
            final Map<Integer, PokemonEvolution> finalEvolutionMap = evolutionMap;
            
            return speciesList.stream().map(s -> {
                try {
                    EvolutionChainVO vo = new EvolutionChainVO();
                    vo.setSpeciesId(s.getId());
                    vo.setName(s.getName());
                    vo.setIsCurrent(s.getId().equals(currentSpeciesId));
                    vo.setSpriteUrl(finalSpriteMap.get(s.getId()));
                    
                    if (s.getEvolvesFromSpeciesId() != null) {
                        PokemonEvolution ev = finalEvolutionMap.get(s.getId());
                        if (ev != null) {
                            vo.setTrigger(ev.getEvolutionTriggerId() == 1 ? "升级" : 
                                          ev.getEvolutionTriggerId() == 2 ? "交换" : "使用物品");
                            vo.setMinLevel(ev.getMinLevel());
                        }
                    }
                    
                    return vo;
                } catch (Exception e) {
                    System.err.println("Error processing evolution chain item: " + e.getMessage());
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error in getEvolutionChain: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
