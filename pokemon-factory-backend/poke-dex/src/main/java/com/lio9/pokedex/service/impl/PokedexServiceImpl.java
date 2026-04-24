package com.lio9.pokedex.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.pokedex.config.PokeDexAssetProperties;
import com.lio9.pokedex.model.*;
import com.lio9.pokedex.mapper.*;
import com.lio9.pokedex.service.PokedexService;
import com.lio9.pokedex.vo.AbilityVO;
import com.lio9.pokedex.vo.ItemVO;
import com.lio9.pokedex.vo.MoveVO;
import com.lio9.pokedex.vo.PokemonDetailVO;
import com.lio9.pokedex.vo.PokemonFormDetailVO;
import com.lio9.pokedex.vo.PokemonListVO;
import com.lio9.pokedex.vo.StatVO;
import com.lio9.pokedex.vo.TypeVO;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 图鉴服务实现
 */
@Service
public class PokedexServiceImpl extends ServiceImpl<PokemonMapper, Pokemon> implements PokedexService {
    private final PokemonFormMapper pokemonFormMapper;
    private final PokemonFormTypeMapper pokemonFormTypeMapper;
    private final PokemonFormAbilityMapper pokemonFormAbilityMapper;
    private final PokemonFormStatMapper pokemonFormStatMapper;
    private final TypeMapper typeMapper;
    private final AbilityMapper abilityMapper;
    private final PokemonEggGroupMapper pokemonEggGroupMapper;
    private final EggGroupMapper eggGroupMapper;
    private final GrowthRateMapper growthRateMapper;
    private final PokeDexAssetProperties assetProperties;

    public PokedexServiceImpl(
        PokemonFormMapper pokemonFormMapper,
        PokemonFormTypeMapper pokemonFormTypeMapper,
        PokemonFormAbilityMapper pokemonFormAbilityMapper,
        PokemonFormStatMapper pokemonFormStatMapper,
        TypeMapper typeMapper,
        AbilityMapper abilityMapper,
        PokemonEggGroupMapper pokemonEggGroupMapper,
        EggGroupMapper eggGroupMapper,
        GrowthRateMapper growthRateMapper,
        PokeDexAssetProperties assetProperties
    ) {
        this.pokemonFormMapper = pokemonFormMapper;
        this.pokemonFormTypeMapper = pokemonFormTypeMapper;
        this.pokemonFormAbilityMapper = pokemonFormAbilityMapper;
        this.pokemonFormStatMapper = pokemonFormStatMapper;
        this.typeMapper = typeMapper;
        this.abilityMapper = abilityMapper;
        this.pokemonEggGroupMapper = pokemonEggGroupMapper;
        this.eggGroupMapper = eggGroupMapper;
        this.growthRateMapper = growthRateMapper;
        this.assetProperties = assetProperties;
    }

    private String imageBaseUrl() {
        return assetProperties.imageBaseUrl();
    }

    @Override
    public Page<PokemonListVO> getPokemonList(int current, int size, Integer typeId, Integer generationId, String keyword) {
        // 1. 构建查询条件
        QueryWrapper<Pokemon> wrapper = new QueryWrapper<>();

        // 关键字搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like("name", keyword.trim())
                    .or()
                    .like("name_en", keyword.trim()));
        }

        // 世代筛选
        if (generationId != null) {
            wrapper.eq("generation_id", generationId);
        }

        // 排序
        wrapper.orderByAsc("id");

        // 2. 分页查询
        Page<Pokemon> page = new Page<>(current, size);
        Page<Pokemon> pokemonPage = page(page, wrapper);

        // 3. 转换为VO
        Page<PokemonListVO> resultPage = new Page<>(current, size, pokemonPage.getTotal());
        List<PokemonListVO> records = new ArrayList<>();

        if (!pokemonPage.getRecords().isEmpty()) {
            List<Integer> speciesIds = pokemonPage.getRecords().stream()
                    .map(Pokemon::getId)
                    .collect(Collectors.toList());

            // 批量获取默认形态
            Map<Integer, Map<String, Object>> defaultFormsMap = new HashMap<>();
            List<Map<String, Object>> defaultForms = pokemonFormMapper.selectDefaultFormsBySpeciesIds(speciesIds);
            for (Map<String, Object> form : defaultForms) {
                Integer speciesId = (Integer) form.get("species_id");
                defaultFormsMap.put(speciesId, form);
            }

            // 批量获取形态的属性信息
            List<Integer> formIds = defaultForms.stream()
                    .map(form -> (Integer) form.get("id"))
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

                    formTypesMap.computeIfAbsent(formId, k -> new ArrayList<>()).add(typeVO);
                }
            }

            // 属性筛选（如果有）
            Set<Integer> filteredSpeciesIds = new HashSet<>(speciesIds);
            if (typeId != null) {
                filteredSpeciesIds.clear();
                for (Map.Entry<Integer, List<TypeVO>> entry : formTypesMap.entrySet()) {
                    boolean hasType = entry.getValue().stream()
                            .anyMatch(type -> type.getId().equals(typeId));
                    if (hasType) {
                        Integer formId = entry.getKey();
                        for (Map<String, Object> form : defaultForms) {
                            if (form.get("id").equals(formId)) {
                                filteredSpeciesIds.add((Integer) form.get("species_id"));
                                break;
                            }
                        }
                    }
                }
            }

            // 构建VO列表
            for (Pokemon pokemon : pokemonPage.getRecords()) {
                // 属性筛选检查
                if (typeId != null && !filteredSpeciesIds.contains(pokemon.getId())) {
                    continue;
                }

                PokemonListVO vo = new PokemonListVO();
                vo.setId(pokemon.getId());
                vo.setName(pokemon.getName());
                vo.setNameEn(pokemon.getNameEn());
                vo.setGenus(pokemon.getGenus());
                vo.setIsLegendary(pokemon.getIsLegendary());
                vo.setIsMythical(pokemon.getIsMythical());
                vo.setGenerationId(pokemon.getGenerationId());

                // 设置默认形态信息
                Map<String, Object> defaultForm = defaultFormsMap.get(pokemon.getId());
                if (defaultForm != null) {
                    vo.setDefaultFormId((Integer) defaultForm.get("id"));

                    // 动态拼接图片URL
                    Integer pokemonId = pokemon.getId();
                    vo.setSpriteUrl(imageBaseUrl() + "/pokemon/" + pokemonId + ".png");
                    vo.setOfficialArtworkUrl(imageBaseUrl() + "/pokemon/other/official-artwork/" + pokemonId + ".png");

                    // 设置属性
                    Integer formId = (Integer) defaultForm.get("id");
                    vo.setTypes(formTypesMap.getOrDefault(formId, new ArrayList<>()));
                }

                records.add(vo);
            }
        }

        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public PokemonDetailVO getPokemonDetail(Integer speciesId) {
        // 1. 获取物种基本信息
        Pokemon pokemon = getById(speciesId);
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
        QueryWrapper<com.lio9.pokedex.model.PokemonEggGroup> eggGroupWrapper = new QueryWrapper<>();
        eggGroupWrapper.eq("pokemon_id", speciesId);
        List<com.lio9.pokedex.model.PokemonEggGroup> pokemonEggGroups = pokemonEggGroupMapper.selectList(eggGroupWrapper);
        if (!pokemonEggGroups.isEmpty()) {
            List<Long> eggGroupIds = pokemonEggGroups.stream()
                    .map(com.lio9.pokedex.model.PokemonEggGroup::getEggGroupId)
                    .collect(Collectors.toList());
            QueryWrapper<com.lio9.pokedex.model.EggGroup> egWrapper = new QueryWrapper<>();
            egWrapper.in("id", eggGroupIds);
            List<com.lio9.pokedex.model.EggGroup> eggGroups = eggGroupMapper.selectList(egWrapper);
            detailVO.setEggGroups(eggGroups.stream()
                    .map(com.lio9.pokedex.model.EggGroup::getName)
                    .collect(Collectors.toList()));
        }

        // 4. 获取形态列表
        List<PokemonForm> forms = pokemonFormMapper.selectBySpeciesId(speciesId);
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
                QueryWrapper<com.lio9.pokedex.model.PokemonFormAbility> abilityWrapper = new QueryWrapper<>();
                abilityWrapper.in("form_id", formIds);
                List<com.lio9.pokedex.model.PokemonFormAbility> formAbilities = pokemonFormAbilityMapper.selectList(abilityWrapper);

                // 批量查询特性，避免N+1查询问题
                Set<Integer> abilityIds = formAbilities.stream()
                        .map(com.lio9.pokedex.model.PokemonFormAbility::getAbilityId)
                        .collect(Collectors.toSet());

                Map<Integer, com.lio9.pokedex.model.Ability> abilityMap = new HashMap<>();
                if (!abilityIds.isEmpty()) {
                        List<com.lio9.pokedex.model.Ability> abilities = abilityMapper.selectByIds(abilityIds);
                    abilityMap = abilities.stream()
                            .collect(Collectors.toMap(com.lio9.pokedex.model.Ability::getId, a -> a));
                }

                for (com.lio9.pokedex.model.PokemonFormAbility formAbility : formAbilities) {
                    Integer formId = formAbility.getFormId();
                    com.lio9.pokedex.model.Ability ability = abilityMap.get(formAbility.getAbilityId());
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
                QueryWrapper<com.lio9.pokedex.model.PokemonFormStat> statWrapper = new QueryWrapper<>();
                statWrapper.in("form_id", formIds);
                List<com.lio9.pokedex.model.PokemonFormStat> formStats = pokemonFormStatMapper.selectList(statWrapper);

                Map<Integer, List<com.lio9.pokedex.model.PokemonFormStat>> statsByForm = formStats.stream()
                        .collect(Collectors.groupingBy(com.lio9.pokedex.model.PokemonFormStat::getFormId));

                for (Map.Entry<Integer, List<com.lio9.pokedex.model.PokemonFormStat>> entry : statsByForm.entrySet()) {
                    Integer formId = entry.getKey();
                    List<com.lio9.pokedex.model.PokemonFormStat> stats = entry.getValue();

                    StatVO statVO = new StatVO();
                    for (com.lio9.pokedex.model.PokemonFormStat stat : stats) {
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

                // 动态拼接图片URL
                Integer pokemonId = pokemon.getId();
                formVO.setSpriteUrl(imageBaseUrl() + "/pokemon/" + pokemonId + ".png");
                formVO.setSpriteBackUrl(imageBaseUrl() + "/pokemon/back/" + pokemonId + ".png");
                formVO.setSpriteShinyUrl(imageBaseUrl() + "/pokemon/shiny/" + pokemonId + ".png");
                formVO.setOfficialArtworkUrl(imageBaseUrl() + "/pokemon/other/official-artwork/" + pokemonId + ".png");

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

/**
     * 获取指定形态在指定版本组下可学的技能列表。
     * <p>
     * 参数：
     *  - formId: 形态 ID（非空）
     *  - versionGroupId: 版本组 ID（可为空，表示所有版本）
     * </p>
     * 返回 MoveVO 列表；当前为占位实现（返回空列表），后续可按数据库表完善查询并缓存。
     */
    @Override
    public List<MoveVO> getFormMoves(Integer formId, Integer versionGroupId) {
        // Placeholder implementation: return an empty list until feature is implemented.
        return List.of();
    }

    @Override
    public List<TypeVO> getAllTypes() {
        QueryWrapper<Type> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("id");
        List<Type> types = typeMapper.selectList(wrapper);

        return types.stream().map(type -> {
            TypeVO vo = new TypeVO();
            vo.setId(type.getId());
            vo.setName(type.getName());
            vo.setNameEn(type.getNameEn());
            vo.setColor(type.getColor());
            vo.setIconUrl(type.getIconUrl());
            return vo;
        }).collect(Collectors.toList());
    }

/**
     * 获取特性分页列表。
     * <p>
     * 参数：current/size 为分页参数，keyword 用于模糊搜索。
     * 返回 AbilityVO 的分页对象；当前为占位实现（空分页），后续可按需求补充查询与排序。
     */
    @Override
    public Page<AbilityVO> getAbilityList(int current, int size, String keyword) {
        // Placeholder implementation: return empty page until implemented.
        return new Page<>(current, size);
    }

/**
     * 获取技能分页列表。
     * <p>
     * 支持按属性（typeId）和关键字搜索（keyword）。当前为占位实现，返回空分页。
     */
    @Override
    public Page<MoveVO> getMoveList(int current, int size, Integer typeId, String keyword) {
        // Placeholder implementation: return empty page until implemented.
        return new Page<>(current, size);
    }

/**
     * 获取物品分页列表。
     * <p>
     * 支持按分类（categoryId）和关键字搜索（keyword）。当前为占位实现，返回空分页。
     */
    @Override
    public Page<ItemVO> getItemList(int current, int size, Integer categoryId, String keyword) {
        // Placeholder implementation: return empty page until implemented.
        return new Page<>(current, size);
    }
}
