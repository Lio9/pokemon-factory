package com.lio9.pokedex.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.common.mapper.PokemonSpeciesMapper;
import com.lio9.common.mapper.PokemonFormMapper;
import com.lio9.common.mapper.PokemonFormTypeMapper;
import com.lio9.common.mapper.PokemonFormAbilityMapper;
import com.lio9.common.mapper.PokemonFormStatMapper;
import com.lio9.common.mapper.AbilityMapper;
import com.lio9.common.mapper.TypeMapper;
import com.lio9.common.mapper.MoveMapper;
import com.lio9.common.mapper.PokemonEvolutionMapper;
import com.lio9.common.model.*;
import com.lio9.common.service.PokemonService;
import com.lio9.common.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 宝可梦服务实现类
 */
@Service
public class PokemonServiceImpl implements PokemonService {

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
    private AbilityMapper abilityMapper;
    
    @Autowired
    private TypeMapper typeMapper;
    
    @Autowired
    private MoveMapper moveMapper;
    
    @Autowired
    private PokemonEvolutionMapper evolutionMapper;

    @Override
    public long count() {
        return speciesMapper.selectCount(null);
    }
    
    @Override
    public void removeAll() {
        speciesMapper.delete(null);
    }

    @Override
    public PokemonDetailVO getDetailById(Long id) {
        Pokemon species = speciesMapper.selectById(id.intValue());
        if (species == null) {
            return null;
        }
        
        PokemonDetailVO detail = new PokemonDetailVO();
        detail.setId(species.getId());
        detail.setName(species.getName());
        detail.setNameEn(species.getNameEn());
        detail.setNameJp(species.getNameJp());
        detail.setGenus(species.getGenus());
        detail.setDescription(species.getDescription());
        detail.setGenerationId(species.getGenerationId());
        detail.setIsLegendary(species.getIsLegendary());
        detail.setIsMythical(species.getIsMythical());
        detail.setIsBaby(species.getIsBaby());
        detail.setCaptureRate(species.getCaptureRate());
        detail.setBaseHappiness(species.getBaseHappiness());
        detail.setGenderRate(species.getGenderRate());
        detail.setHatchCounter(species.getHatchCounter());
        
        // 获取形态列表
        QueryWrapper<PokemonForm> formQuery = new QueryWrapper<>();
        formQuery.eq("species_id", id).orderByDesc("is_default").orderByAsc("id");
        List<PokemonForm> forms = formMapper.selectList(formQuery);
        
        List<PokemonFormDetailVO> formVOs = forms.stream().map(form -> {
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
            
            formVO.setTypes(getTypesByFormId(form.getId()));
            formVO.setAbilities(getAbilitiesByFormId(form.getId()));
            formVO.setStats(getStatsByFormId(form.getId()));
            
            return formVO;
        }).collect(Collectors.toList());
        
        detail.setForms(formVOs);
        detail.setEvolutionChain(getEvolutionChainVO(id));
        
        return detail;
    }
    
    @Override
    public Page<Pokemon> searchPokemon(String keyword, Page<Pokemon> page) {
        QueryWrapper<Pokemon> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("name", keyword)
                   .or()
                   .like("name_en", keyword)
                   .orderByAsc("id");
        
        List<Pokemon> list = speciesMapper.selectList(queryWrapper);
        page.setRecords(list);
        page.setTotal(list.size());
        return page;
    }
    
    @Override
    public Pokemon getByIndexNumber(String indexNumber) {
        try {
            Integer id = Integer.parseInt(indexNumber);
            return speciesMapper.selectById(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    @Override
    public List<EvolutionVO> getEvolutionChain(Long pokemonId) {
        Pokemon species = speciesMapper.selectById(pokemonId.intValue());
        if (species == null || species.getEvolutionChainId() == null) {
            return Collections.emptyList();
        }
        
        QueryWrapper<Pokemon> query = new QueryWrapper<>();
        query.eq("evolution_chain_id", species.getEvolutionChainId()).orderByAsc("id");
        List<Pokemon> speciesList = speciesMapper.selectList(query);
        
        return speciesList.stream().map(s -> {
            EvolutionVO vo = new EvolutionVO();
            vo.setSpeciesId(s.getId());
            vo.setPokemonId(s.getId().longValue());
            vo.setPokemonName(s.getName());
            vo.setPokemonIndexNumber(String.valueOf(s.getId()));
            vo.setIsCurrent(s.getId().equals(pokemonId.intValue()));
            
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
                    vo.setEvolutionMethod(ev.getEvolutionTriggerId() == 1 ? "升级" : 
                                          ev.getEvolutionTriggerId() == 2 ? "交换" : "使用物品");
                    vo.setEvolutionValue(ev.getMinLevel() != null ? String.valueOf(ev.getMinLevel()) : "-1");
                }
            }
            
            return vo;
        }).collect(Collectors.toList());
    }
    
    private List<EvolutionChainVO> getEvolutionChainVO(Long pokemonId) {
        List<EvolutionVO> evos = getEvolutionChain(pokemonId);
        return evos.stream().map(evo -> {
            EvolutionChainVO vo = new EvolutionChainVO();
            vo.setSpeciesId(evo.getSpeciesId());
            vo.setName(evo.getPokemonName());
            vo.setSpriteUrl(evo.getSpriteUrl());
            vo.setTrigger(evo.getEvolutionMethod());
            vo.setIsCurrent(evo.getIsCurrent());
            if (evo.getEvolutionValue() != null && !"-1".equals(evo.getEvolutionValue())) {
                try {
                    vo.setMinLevel(Integer.parseInt(evo.getEvolutionValue()));
                } catch (NumberFormatException ignored) {}
            }
            return vo;
        }).collect(Collectors.toList());
    }
    
    @Override
    public List<Move> getMoves(Long pokemonId) {
        QueryWrapper<PokemonForm> formQuery = new QueryWrapper<>();
        formQuery.eq("species_id", pokemonId).eq("is_default", 1);
        PokemonForm form = formMapper.selectOne(formQuery);
        
        if (form == null) {
            return Collections.emptyList();
        }
        
        List<Map<String, Object>> moves = moveMapper.selectMovesByFormId(form.getId());
        
        return moves.stream().map(m -> {
            Move move = new Move();
            move.setId((Integer) m.get("id"));
            move.setName((String) m.get("name"));
            move.setNameEn((String) m.get("name_en"));
            move.setPower((Integer) m.get("power"));
            move.setAccuracy((Integer) m.get("accuracy"));
            move.setPp((Integer) m.get("pp"));
            move.setDescription((String) m.get("description"));
            return move;
        }).collect(Collectors.toList());
    }
    
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
}