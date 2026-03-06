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
 * 图鉴服务实现类
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
        
        QueryWrapper<Pokemon> query = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            query.and(w -> w.like("name", keyword).or().like("name_en", keyword));
        }
        if (generationId != null) {
            query.eq("generation_id", generationId);
        }
        query.orderByAsc("id");
        
        Page<Pokemon> page = new Page<>(current, size);
        page = speciesMapper.selectPage(page, query);
        
        List<PokemonListVO> voList = page.getRecords().stream().map(species -> {
            PokemonListVO vo = new PokemonListVO();
            vo.setId(species.getId());
            vo.setName(species.getName());
            vo.setNameEn(species.getNameEn());
            vo.setGenus(species.getGenus());
            vo.setIsLegendary(species.getIsLegendary());
            vo.setIsMythical(species.getIsMythical());
            vo.setGenerationId(species.getGenerationId());
            
            QueryWrapper<PokemonForm> formQuery = new QueryWrapper<>();
            formQuery.eq("species_id", species.getId()).eq("is_default", 1);
            PokemonForm defaultForm = formMapper.selectOne(formQuery);
            
            if (defaultForm != null) {
                vo.setDefaultFormId(defaultForm.getId());
                vo.setSpriteUrl(defaultForm.getSpriteUrl());
                vo.setOfficialArtworkUrl(defaultForm.getOfficialArtworkUrl());
                
                List<TypeVO> types = getTypesByFormId(defaultForm.getId());
                vo.setTypes(types);
                
                if (typeId != null && types.stream().noneMatch(t -> t.getId().equals(typeId))) {
                    return null;
                }
            }
            
            return vo;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        
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
        
        Map<Integer, Type> typeMap = typeMapper.selectList(null).stream()
                .collect(Collectors.toMap(Type::getId, t -> t));
        
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
            
            Type type = typeMap.get(m.getTypeId());
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
    
    private List<TypeVO> getTypesByFormId(Integer formId) {
        QueryWrapper<PokemonFormType> query = new QueryWrapper<>();
        query.eq("form_id", formId).orderByAsc("slot");
        List<PokemonFormType> formTypes = formTypeMapper.selectList(query);
        
        return formTypes.stream().map(ft -> {
            Type type = typeMapper.selectById(ft.getTypeId());
            if (type != null) {
                TypeVO vo = new TypeVO();
                vo.setId(type.getId());
                vo.setName(type.getName());
                vo.setNameEn(type.getNameEn());
                vo.setColor(type.getColor());
                return vo;
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
    
    private List<PokemonFormDetailVO> getFormsBySpeciesId(Integer speciesId) {
        QueryWrapper<PokemonForm> query = new QueryWrapper<>();
        query.eq("species_id", speciesId).orderByDesc("is_default").orderByAsc("id");
        List<PokemonForm> forms = formMapper.selectList(query);
        
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
            
            vo.setTypes(getTypesByFormId(form.getId()));
            vo.setAbilities(getAbilitiesByFormId(form.getId()));
            vo.setStats(getStatsByFormId(form.getId()));
            
            return vo;
        }).collect(Collectors.toList());
    }
    
    private List<AbilityVO> getAbilitiesByFormId(Integer formId) {
        List<Map<String, Object>> abilities = abilityMapper.selectAbilitiesByFormId(formId);
        
        return abilities.stream().map(a -> {
            AbilityVO vo = new AbilityVO();
            vo.setId((Integer) a.get("id"));
            vo.setName((String) a.get("name"));
            vo.setNameEn((String) a.get("name_en"));
            vo.setDescription((String) a.get("description"));
            vo.setIsHidden((Boolean) a.get("is_hidden"));
            vo.setSlot((Integer) a.get("slot"));
            return vo;
        }).collect(Collectors.toList());
    }
    
    private StatVO getStatsByFormId(Integer formId) {
        QueryWrapper<PokemonFormStat> query = new QueryWrapper<>();
        query.eq("form_id", formId);
        List<PokemonFormStat> stats = formStatMapper.selectList(query);
        
        StatVO vo = new StatVO();
        int total = 0;
        
        for (PokemonFormStat stat : stats) {
            int value = stat.getBaseStat();
            total += value;
            
            switch (stat.getStatId()) {
                case 1: vo.setHp(value); break;
                case 2: vo.setAttack(value); break;
                case 3: vo.setDefense(value); break;
                case 4: vo.setSpAttack(value); break;
                case 5: vo.setSpDefense(value); break;
                case 6: vo.setSpeed(value); break;
            }
        }
        
        vo.setTotal(total);
        return vo;
    }
    
    private List<EvolutionChainVO> getEvolutionChain(Integer chainId, Integer currentSpeciesId) {
        if (chainId == null) {
            return Collections.emptyList();
        }
        
        QueryWrapper<Pokemon> query = new QueryWrapper<>();
        query.eq("evolution_chain_id", chainId).orderByAsc("id");
        List<Pokemon> speciesList = speciesMapper.selectList(query);
        
        return speciesList.stream().map(s -> {
            EvolutionChainVO vo = new EvolutionChainVO();
            vo.setSpeciesId(s.getId());
            vo.setName(s.getName());
            vo.setIsCurrent(s.getId().equals(currentSpeciesId));
            
            QueryWrapper<PokemonForm> formQuery = new QueryWrapper<>();
            formQuery.eq("species_id", s.getId()).eq("is_default", 1);
            PokemonForm form = formMapper.selectOne(formQuery);
            if (form != null) {
                vo.setSpriteUrl(form.getSpriteUrl());
            }
            
            if (s.getEvolvesFromSpeciesId() != null) {
                QueryWrapper<PokemonEvolution> evQuery = new QueryWrapper<>();
                evQuery.eq("evolved_species_id", s.getId());
                PokemonEvolution ev = evolutionMapper.selectOne(evQuery);
                if (ev != null) {
                    vo.setTrigger(ev.getEvolutionTriggerId() == 1 ? "升级" : 
                                  ev.getEvolutionTriggerId() == 2 ? "交换" : "使用物品");
                    vo.setMinLevel(ev.getMinLevel());
                }
            }
            
            return vo;
        }).collect(Collectors.toList());
    }
}