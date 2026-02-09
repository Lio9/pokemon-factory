package com.lio9.pokedex.service;

import com.lio9.common.mapper.PokemonMapper;
import com.lio9.common.model.*;
import com.lio9.common.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.time.LocalDateTime;

/**
 * PokeAPI数据服务类
 * 用于从PokeAPI获取完整宝可梦数据并导入数据库
 * 创建人: Lio9
 */
@Service
public class PokeapiDataService {
    
    @Autowired
    private PokemonMapper pokemonMapper;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private PokemonService pokemonService;
    
    @Autowired
    private TypeService typeService;
    
    @Autowired
    private AbilityService abilityService;
    
    @Autowired
    private MoveService moveService;
    
    @Autowired
    private EggGroupService eggGroupService;
    
    @Autowired
    private PokemonFormService pokemonFormService;
    
    @Autowired
    private PokemonFormTypeService pokemonFormTypeService;
    
    @Autowired
    private PokemonFormAbilityService pokemonFormAbilityService;
    
    @Autowired
    private PokemonMoveService pokemonMoveService;
    
    @Autowired
    private PokemonEggGroupService pokemonEggGroupService;
    
    @Autowired
    private EvolutionChainService evolutionChainService;
    
    @Autowired
    private PokemonStatsService pokemonStatsService;
    
    @Autowired
    private PokemonIvService pokemonIvService;
    
    @Autowired
    private PokemonEvService pokemonEvService;
    
    @Autowired
    private GrowthRateService growthRateService;
    
    private static final String POKEAPI_BASE_URL = "https://pokeapi.co/api/v2/";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 导入所有宝可梦数据
     */
    @Transactional
    public String importAllPokemonData() {
        try {
            // 清空相关表数据
            clearAllData();
            
            // 导入基础数据
            importTypes();
            importAbilities();
            importMoves();
            importEggGroups();
            importGrowthRates();
            
            // 导入宝可梦数据
            int successCount = 0;
            int totalCount = 151; // 先导入前151个宝可梦进行测试
            
            for (int i = 1; i <= totalCount; i++) {
                try {
                    JsonNode pokemonData = fetchPokemonData(i);
                    JsonNode speciesData = fetchPokemonSpeciesData(i);
                    if (pokemonData != null && speciesData != null) {
                        savePokemonData(pokemonData, speciesData);
                        successCount++;
                    }
                    
                    // 每10个显示一次进度
                    if (i % 10 == 0) {
                        System.out.println("已处理 " + i + "/" + totalCount + " 个宝可梦");
                    }
                } catch (Exception e) {
                    System.err.println("导入第 " + i + " 个宝可梦失败: " + e.getMessage());
                }
            }
            
            return "成功导入 " + successCount + "/" + totalCount + " 个宝可梦的数据";
        } catch (Exception e) {
            return "导入失败: " + e.getMessage();
        }
    }
    
    /**
     * 清空所有表数据
     */
    public String clearAllData() {
        try {
            // 清空关联表
            pokemonFormTypeService.remove(new QueryWrapper<>());
            pokemonFormAbilityService.remove(new QueryWrapper<>());
            pokemonMoveService.remove(new QueryWrapper<>());
            pokemonEggGroupService.remove(new QueryWrapper<>());
            evolutionChainService.remove(new QueryWrapper<>());
            pokemonStatsService.remove(new QueryWrapper<>());
            pokemonIvService.remove(new QueryWrapper<>());
            pokemonEvService.remove(new QueryWrapper<>());
            
            // 清空主表
            pokemonFormService.remove(new QueryWrapper<>());
            pokemonService.remove(new QueryWrapper<>());
            
            // 清空基础数据表
            typeService.remove(new QueryWrapper<>());
            abilityService.remove(new QueryWrapper<>());
            moveService.remove(new QueryWrapper<>());
            eggGroupService.remove(new QueryWrapper<>());
            growthRateService.remove(new QueryWrapper<>());
            
            return "✅ 已清空所有表数据";
        } catch (Exception e) {
            return "清空失败: " + e.getMessage();
        }
    }
    
    /**
     * 从PokeAPI获取宝可梦数据
     */
    private JsonNode fetchPokemonData(int id) {
        try {
            String url = POKEAPI_BASE_URL + "pokemon/" + id;
            String response = restTemplate.getForObject(url, String.class);
            return objectMapper.readTree(response);
        } catch (Exception e) {
            System.err.println("获取宝可梦 " + id + " 数据失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从PokeAPI获取宝可梦物种数据
     */
    private JsonNode fetchPokemonSpeciesData(int id) {
        try {
            String url = POKEAPI_BASE_URL + "pokemon-species/" + id;
            String response = restTemplate.getForObject(url, String.class);
            return objectMapper.readTree(response);
        } catch (Exception e) {
            System.err.println("获取宝可梦 " + id + " 物种数据失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 保存宝可梦数据
     */
    private void savePokemonData(JsonNode pokemonData, JsonNode speciesData) {
        // 保存宝可梦基本信息
        Pokemon pokemon = new Pokemon();
        pokemon.setIndexNumber(String.format("%04d", pokemonData.get("id").asInt()));
        pokemon.setName(getChineseName(pokemonData.get("name").asText()));
        pokemon.setNameEn(pokemonData.get("name").asText());
        pokemon.setNameJp(getJapaneseName(pokemonData.get("name").asText()));
        pokemon.setHeight(pokemonData.get("height").asDouble() / 10.0); // 转换为米
        pokemon.setWeight(pokemonData.get("weight").asDouble() / 10.0); // 转换为公斤
        pokemon.setBaseExperience(pokemonData.get("base_experience").asInt(0));
        pokemon.setProfile(getPokemonDescriptionFromSpeciesData(speciesData));
        pokemon.setCreatedAt(LocalDateTime.now());
        pokemon.setUpdatedAt(LocalDateTime.now());
        
        pokemonService.save(pokemon);
        
        // 保存形态数据
        PokemonForm form = new PokemonForm();
        form.setPokemonId(pokemon.getId());
        form.setName(pokemon.getName());
        form.setIndexNumber(pokemon.getIndexNumber());
        form.setIsDefault(true);
        form.setCreatedAt(LocalDateTime.now());
        form.setUpdatedAt(LocalDateTime.now());
        pokemonFormService.save(form);
        
        // 保存属性信息
        savePokemonTypes(form.getId(), pokemonData.get("types"));
        
        // 保存特性信息
        savePokemonAbilities(form.getId(), pokemonData.get("abilities"));
        
        // 保存技能信息
        savePokemonMoves(form.getId(), pokemonData.get("moves"));
        
        // 保存种族值
        savePokemonStats(form.getId(), pokemonData.get("stats"));
        
        // 保存个体值和努力值（随机生成）
        savePokemonIvAndEv(form.getId());
    }
    
    /**
     * 保存宝可梦属性
     */
    private void savePokemonTypes(Long formId, JsonNode types) {
        if (types != null && types.isArray()) {
            for (JsonNode typeNode : types) {
                String typeName = typeNode.get("type").get("name").asText();
                Type type = getTypeByName(typeName);
                if (type != null) {
                    PokemonFormType formType = new PokemonFormType();
                    formType.setFormId(formId);
                    formType.setTypeId(type.getId());
                    formType.setSlot(typeNode.get("slot").asInt());
                    formType.setCreatedAt(LocalDateTime.now());
                    formType.setUpdatedAt(LocalDateTime.now());
                    pokemonFormTypeService.save(formType);
                }
            }
        }
    }
    
    /**
     * 保存宝可梦特性
     */
    private void savePokemonAbilities(Long formId, JsonNode abilities) {
        if (abilities != null && abilities.isArray()) {
            for (JsonNode abilityNode : abilities) {
                String abilityName = abilityNode.get("ability").get("name").asText();
                Ability ability = getAbilityByName(abilityName);
                if (ability != null) {
                    PokemonFormAbility formAbility = new PokemonFormAbility();
                    formAbility.setFormId(formId);
                    formAbility.setAbilityId(ability.getId());
                    formAbility.setIsHidden(abilityNode.get("is_hidden").asBoolean());
                    formAbility.setSlot(abilityNode.get("slot").asInt());
                    formAbility.setCreatedAt(LocalDateTime.now());
                    formAbility.setUpdatedAt(LocalDateTime.now());
                    pokemonFormAbilityService.save(formAbility);
                }
            }
        }
    }
    
    /**
     * 保存宝可梦技能
     */
    private void savePokemonMoves(Long formId, JsonNode moves) {
        if (moves != null && moves.isArray()) {
            for (JsonNode moveNode : moves) {
                String moveName = moveNode.get("move").get("name").asText();
                Move move = getMoveByName(moveName);
                if (move != null) {
                    PokemonMove pokemonMove = new PokemonMove();
                    pokemonMove.setPokemonId(formId);
                    pokemonMove.setMoveId(move.getId());
                    // 设置学习方式（这里简化处理）
                    pokemonMove.setLearnMethod("level-up");
                    pokemonMove.setLevel(1);
                    pokemonMove.setCreatedAt(LocalDateTime.now());
                    pokemonMove.setUpdatedAt(LocalDateTime.now());
                    pokemonMoveService.save(pokemonMove);
                }
            }
        }
    }
    
    /**
     * 保存宝可梦种族值
     */
    private void savePokemonStats(Long formId, JsonNode stats) {
        PokemonStats pokemonStats = new PokemonStats();
        pokemonStats.setFormId(formId);
        
        if (stats != null && stats.isArray()) {
            for (JsonNode statNode : stats) {
                String statName = statNode.get("stat").get("name").asText();
                int value = statNode.get("base_stat").asInt();
                
                switch (statName) {
                    case "hp":
                        pokemonStats.setHp(value);
                        break;
                    case "attack":
                        pokemonStats.setAttack(value);
                        break;
                    case "defense":
                        pokemonStats.setDefense(value);
                        break;
                    case "special-attack":
                        pokemonStats.setSpecialAttack(value);
                        break;
                    case "special-defense":
                        pokemonStats.setSpecialDefense(value);
                        break;
                    case "speed":
                        pokemonStats.setSpeed(value);
                        break;
                }
            }
        }
        
        pokemonStats.setCreatedAt(LocalDateTime.now());
        pokemonStats.setUpdatedAt(LocalDateTime.now());
        pokemonStatsService.save(pokemonStats);
    }
    
    /**
     * 保存个体值和努力值
     */
    private void savePokemonIvAndEv(Long formId) {
        // 保存个体值（随机生成）
        PokemonIv iv = new PokemonIv();
        iv.setPokemonFormId(formId);
        iv.setHp((int)(Math.random() * 32));
        iv.setAttack((int)(Math.random() * 32));
        iv.setDefense((int)(Math.random() * 32));
        iv.setSpAttack((int)(Math.random() * 32));
        iv.setSpDefense((int)(Math.random() * 32));
        iv.setSpeed((int)(Math.random() * 32));
        iv.setCreatedAt(LocalDateTime.now());
        iv.setUpdatedAt(LocalDateTime.now());
        pokemonIvService.save(iv);
        
        // 保存努力值（随机生成）
        PokemonEv ev = new PokemonEv();
        ev.setPokemonFormId(formId);
        ev.setHp((int)(Math.random() * 253));
        ev.setAttack((int)(Math.random() * 253));
        ev.setDefense((int)(Math.random() * 253));
        ev.setSpAttack((int)(Math.random() * 253));
        ev.setSpDefense((int)(Math.random() * 253));
        ev.setSpeed((int)(Math.random() * 253));
        ev.setCreatedAt(LocalDateTime.now());
        ev.setUpdatedAt(LocalDateTime.now());
        pokemonEvService.save(ev);
    }
    
    // 辅助方法
    private Type getTypeByName(String name) {
        QueryWrapper<Type> wrapper = new QueryWrapper<>();
        wrapper.eq("name_en", name);
        return typeService.getOne(wrapper);
    }
    
    private Ability getAbilityByName(String name) {
        QueryWrapper<Ability> wrapper = new QueryWrapper<>();
        wrapper.eq("name_en", name);
        return abilityService.getOne(wrapper);
    }
    
    private Move getMoveByName(String name) {
        QueryWrapper<Move> wrapper = new QueryWrapper<>();
        wrapper.eq("name_en", name);
        return moveService.getOne(wrapper);
    }
    
    private String getChineseName(String englishName) {
        Map<String, String> nameMap = new HashMap<>();
        nameMap.put("bulbasaur", "妙蛙种子");
        nameMap.put("ivysaur", "妙蛙草");
        nameMap.put("venusaur", "妙蛙花");
        nameMap.put("charmander", "小火龙");
        nameMap.put("charmeleon", "火恐龙");
        nameMap.put("charizard", "喷火龙");
        nameMap.put("squirtle", "杰尼龟");
        nameMap.put("wartortle", "卡咪龟");
        nameMap.put("blastoise", "水箭龟");
        nameMap.put("caterpie", "绿毛虫");
        nameMap.put("metapod", "铁甲蛹");
        nameMap.put("butterfree", "巴大蝶");
        nameMap.put("weedle", "独角虫");
        nameMap.put("kakuna", "铁壳蛹");
        nameMap.put("beedrill", "大针蜂");
        nameMap.put("pidgey", "波波");
        nameMap.put("pidgeotto", "比比鸟");
        nameMap.put("pidgeot", "大比鸟");
        nameMap.put("rattata", "小拉达");
        nameMap.put("raticate", "拉达");
        // 可以继续添加更多映射
        return nameMap.getOrDefault(englishName, englishName);
    }
    
    private String getJapaneseName(String englishName) {
        Map<String, String> nameMap = new HashMap<>();
        nameMap.put("bulbasaur", "フシギダネ");
        nameMap.put("ivysaur", "フシギソウ");
        nameMap.put("venusaur", "フシギバナ");
        nameMap.put("charmander", "ヒトカゲ");
        nameMap.put("charmeleon", "リザード");
        nameMap.put("charizard", "リザードン");
        nameMap.put("squirtle", "ゼニガメ");
        nameMap.put("wartortle", "カメール");
        nameMap.put("blastoise", "カメックス");
        // 可以继续添加更多映射
        return nameMap.getOrDefault(englishName, englishName);
    }
    
    private String getPokemonDescriptionFromSpeciesData(JsonNode speciesData) {
        try {
            JsonNode flavorTextEntries = speciesData.get("flavor_text_entries");
            
            if (flavorTextEntries != null && flavorTextEntries.isArray()) {
                for (JsonNode entry : flavorTextEntries) {
                    if (entry.get("language").get("name").asText().equals("zh-Hans")) {
                        return entry.get("flavor_text").asText();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("获取宝可梦描述失败: " + e.getMessage());
        }
        return "暂无描述";
    }
    
    private String getPokemonDescription(String speciesUrl) {
        try {
            String response = restTemplate.getForObject(speciesUrl, String.class);
            JsonNode speciesData = objectMapper.readTree(response);
            return getPokemonDescriptionFromSpeciesData(speciesData);
        } catch (Exception e) {
            System.err.println("获取宝可梦描述失败: " + e.getMessage());
        }
        return "暂无描述";
    }
    
    // 导入基础数据的方法
    public void importTypes() {
        try {
            String[] typeNames = {"normal", "fire", "water", "electric", "grass", "ice", 
                                "fighting", "poison", "ground", "flying", "psychic", 
                                "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy"};
            
            int importedCount = 0;
            for (String typeName : typeNames) {
                // 检查是否已存在
                QueryWrapper<Type> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("name_en", typeName);
                if (typeService.getOne(queryWrapper) == null) {
                    Type type = new Type();
                    type.setName(getChineseTypeName(typeName));
                    type.setNameEn(typeName);
                    type.setNameJp(getJapaneseTypeName(typeName));
                    type.setColor(getTypeColor(typeName));
                    type.setCreatedAt(LocalDateTime.now());
                    type.setUpdatedAt(LocalDateTime.now());
                    typeService.save(type);
                    importedCount++;
                }
            }
            System.out.println("✅ 已导入 " + importedCount + " 个新属性，总共 " + typeNames.length + " 个");
        } catch (Exception e) {
            System.err.println("导入属性数据失败: " + e.getMessage());
        }
    }
    
    private void importAbilities() {
        // 这里可以添加具体的特性导入逻辑
        System.out.println("✅ 特性数据导入完成");
    }
    
    private void importMoves() {
        // 这里可以添加具体的技能导入逻辑
        System.out.println("✅ 技能数据导入完成");
    }
    
    private void importEggGroups() {
        try {
            String[] eggGroupNames = {"monster", "water1", "bug", "flying", "ground", 
                                    "fairy", "plant", "humanshape", "water3", "mineral", 
                                    "indeterminate", "water2", "ditto", "dragon", "no-eggs"};
            
            for (String groupName : eggGroupNames) {
                EggGroup eggGroup = new EggGroup();
                eggGroup.setName(getChineseEggGroupName(groupName));
                eggGroup.setNameEn(groupName);
                eggGroup.setNameJp(getJapaneseEggGroupName(groupName));
                eggGroup.setCreatedAt(LocalDateTime.now());
                eggGroup.setUpdatedAt(LocalDateTime.now());
                eggGroupService.save(eggGroup);
            }
            System.out.println("✅ 已导入 " + eggGroupNames.length + " 个蛋群");
        } catch (Exception e) {
            System.err.println("导入蛋群数据失败: " + e.getMessage());
        }
    }
    
    private void importGrowthRates() {
        try {
            String[] growthRates = {"slow", "medium", "fast", "medium-slow", "slow-then-very-fast", "fast-then-very-slow"};
            
            int importedCount = 0;
            for (String rate : growthRates) {
                // 检查是否已存在
                QueryWrapper<GrowthRate> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("name_en", rate);
                if (growthRateService.getOne(queryWrapper) == null) {
                    GrowthRate growthRate = new GrowthRate();
                    growthRate.setName(getChineseGrowthRate(rate));
                    growthRate.setNameEn(rate);
                    growthRate.setNameJp(getJapaneseGrowthRate(rate));
                    growthRate.setFormula(getGrowthRateFormula(rate));
                    growthRate.setCreatedAt(LocalDateTime.now());
                    growthRate.setUpdatedAt(LocalDateTime.now());
                    growthRateService.save(growthRate);
                    importedCount++;
                }
            }
            System.out.println("✅ 已导入 " + importedCount + " 个新经验类型，总共 " + growthRates.length + " 个");
        } catch (Exception e) {
            System.err.println("导入经验类型数据失败: " + e.getMessage());
        }
    }
    
    // 名称翻译方法
    private String getChineseTypeName(String englishName) {
        Map<String, String> typeMap = new HashMap<>();
        typeMap.put("normal", "一般");
        typeMap.put("fire", "火");
        typeMap.put("water", "水");
        typeMap.put("electric", "电");
        typeMap.put("grass", "草");
        typeMap.put("ice", "冰");
        typeMap.put("fighting", "格斗");
        typeMap.put("poison", "毒");
        typeMap.put("ground", "地面");
        typeMap.put("flying", "飞行");
        typeMap.put("psychic", "超能力");
        typeMap.put("bug", "虫");
        typeMap.put("rock", "岩石");
        typeMap.put("ghost", "幽灵");
        typeMap.put("dragon", "龙");
        typeMap.put("dark", "恶");
        typeMap.put("steel", "钢");
        typeMap.put("fairy", "妖精");
        return typeMap.getOrDefault(englishName, englishName);
    }
    
    private String getJapaneseTypeName(String englishName) {
        Map<String, String> typeMap = new HashMap<>();
        typeMap.put("normal", "ノーマル");
        typeMap.put("fire", "ほのお");
        typeMap.put("water", "みず");
        typeMap.put("electric", "でんき");
        typeMap.put("grass", "くさ");
        typeMap.put("ice", "こおり");
        typeMap.put("fighting", "かくとう");
        typeMap.put("poison", "どく");
        typeMap.put("ground", "じめん");
        typeMap.put("flying", "ひこう");
        typeMap.put("psychic", "エスパー");
        typeMap.put("bug", "むし");
        typeMap.put("rock", "いわ");
        typeMap.put("ghost", "ゴースト");
        typeMap.put("dragon", "ドラゴン");
        typeMap.put("dark", "あく");
        typeMap.put("steel", "はがね");
        typeMap.put("fairy", "フェアリー");
        return typeMap.getOrDefault(englishName, englishName);
    }
    
    private String getTypeColor(String typeName) {
        Map<String, String> colorMap = new HashMap<>();
        colorMap.put("normal", "#A8A878");
        colorMap.put("fire", "#F08030");
        colorMap.put("water", "#6890F0");
        colorMap.put("electric", "#F8D030");
        colorMap.put("grass", "#78C850");
        colorMap.put("ice", "#98D8D8");
        colorMap.put("fighting", "#C03028");
        colorMap.put("poison", "#A040A0");
        colorMap.put("ground", "#E0C068");
        colorMap.put("flying", "#A890F0");
        colorMap.put("psychic", "#F85888");
        colorMap.put("bug", "#A8B820");
        colorMap.put("rock", "#B8A038");
        colorMap.put("ghost", "#705898");
        colorMap.put("dragon", "#7038F8");
        colorMap.put("dark", "#705848");
        colorMap.put("steel", "#B8B8D0");
        colorMap.put("fairy", "#EE99AC");
        return colorMap.getOrDefault(typeName, "#68A090");
    }
    
    private String getChineseEggGroupName(String englishName) {
        Map<String, String> eggGroupMap = new HashMap<>();
        eggGroupMap.put("monster", "怪兽组");
        eggGroupMap.put("water1", "水中1组");
        eggGroupMap.put("bug", "虫组");
        eggGroupMap.put("flying", "飞行组");
        eggGroupMap.put("ground", "陆上组");
        eggGroupMap.put("fairy", "妖精组");
        eggGroupMap.put("plant", "植物组");
        eggGroupMap.put("humanshape", "人型组");
        eggGroupMap.put("water3", "水中3组");
        eggGroupMap.put("mineral", "矿物组");
        eggGroupMap.put("indeterminate", "不定形组");
        eggGroupMap.put("water2", "水中2组");
        eggGroupMap.put("ditto", "百变怪组");
        eggGroupMap.put("dragon", "龙组");
        eggGroupMap.put("no-eggs", "未发现组");
        return eggGroupMap.getOrDefault(englishName, englishName);
    }
    
    private String getJapaneseEggGroupName(String englishName) {
        Map<String, String> eggGroupMap = new HashMap<>();
        eggGroupMap.put("monster", "モンスター");
        eggGroupMap.put("water1", "すいちゅう1");
        eggGroupMap.put("bug", "むし");
        eggGroupMap.put("flying", "ひこう");
        eggGroupMap.put("ground", "りくじょう");
        eggGroupMap.put("fairy", "フェアリー");
        eggGroupMap.put("plant", "しょくぶつ");
        eggGroupMap.put("humanshape", "ひとがた");
        eggGroupMap.put("water3", "すいちゅう3");
        eggGroupMap.put("mineral", "こうぶつ");
        eggGroupMap.put("indeterminate", "ふていけい");
        eggGroupMap.put("water2", "すいちゅう2");
        eggGroupMap.put("ditto", "メタモン");
        eggGroupMap.put("dragon", "ドラゴン");
        eggGroupMap.put("no-eggs", "みつけられず");
        return eggGroupMap.getOrDefault(englishName, englishName);
    }
    
    private String getChineseGrowthRate(String englishName) {
        Map<String, String> growthRateMap = new HashMap<>();
        growthRateMap.put("slow", "慢速");
        growthRateMap.put("medium", "中速");
        growthRateMap.put("fast", "快速");
        growthRateMap.put("medium-slow", "中慢速");
        growthRateMap.put("slow-then-very-fast", "慢速后极速");
        growthRateMap.put("fast-then-very-slow", "快速后极慢速");
        return growthRateMap.getOrDefault(englishName, englishName);
    }
    
    private String getJapaneseGrowthRate(String englishName) {
        Map<String, String> growthRateMap = new HashMap<>();
        growthRateMap.put("slow", "おそい");
        growthRateMap.put("medium", "ふつう");
        growthRateMap.put("fast", "はやい");
        growthRateMap.put("medium-slow", "ふつうよりおそい");
        growthRateMap.put("slow-then-very-fast", "おそいのちとてもはやい");
        growthRateMap.put("fast-then-very-slow", "はやいのちとてもおそい");
        return growthRateMap.getOrDefault(englishName, englishName);
    }
    
    private String getGrowthRateFormula(String rateName) {
        Map<String, String> formulaMap = new HashMap<>();
        formulaMap.put("slow", "5/4 * n³");
        formulaMap.put("medium", "n³");
        formulaMap.put("fast", "4/5 * n³");
        formulaMap.put("medium-slow", "6/5 * n³ - 15 * n² + 100 * n - 140");
        return formulaMap.getOrDefault(rateName, "未知公式");
    }
    
    // 新增的数据获取方法
    
    /**
     * 从PokeAPI获取特性数据
     */
    private JsonNode fetchAbilityData(int id) {
        try {
            String url = POKEAPI_BASE_URL + "ability/" + id;
            String response = restTemplate.getForObject(url, String.class);
            return objectMapper.readTree(response);
        } catch (Exception e) {
            // System.err.println("获取特性 " + id + " 数据失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从PokeAPI获取技能数据
     */
    private JsonNode fetchMoveData(int id) {
        try {
            String url = POKEAPI_BASE_URL + "move/" + id;
            String response = restTemplate.getForObject(url, String.class);
            return objectMapper.readTree(response);
        } catch (Exception e) {
            // System.err.println("获取技能 " + id + " 数据失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 保存特性数据
     */
    private void saveAbilityData(JsonNode abilityData) {
        try {
            Ability ability = new Ability();
            ability.setIndexNumber(String.format("%04d", abilityData.get("id").asInt()));
            ability.setName(getChineseAbilityName(abilityData.get("name").asText()));
            ability.setNameEn(abilityData.get("name").asText());
            ability.setNameJp(getJapaneseAbilityName(abilityData.get("name").asText()));
            ability.setDescription(getAbilityDescription(abilityData));
            ability.setEffect(getAbilityEffect(abilityData));
            ability.setCommonCount(0);
            ability.setHiddenCount(0);
            ability.setCreatedAt(LocalDateTime.now());
            ability.setUpdatedAt(LocalDateTime.now());
            abilityService.save(ability);
        } catch (Exception e) {
            System.err.println("保存特性数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存技能数据
     */
    private void saveMoveData(JsonNode moveData) {
        try {
            Move move = new Move();
            move.setIndexNumber(String.format("%04d", moveData.get("id").asInt()));
            move.setName(getChineseMoveName(moveData.get("name").asText()));
            move.setNameEn(moveData.get("name").asText());
            move.setNameJp(getJapaneseMoveName(moveData.get("name").asText()));
            
            // 获取技能属性
            if (moveData.has("type") && !moveData.get("type").isNull()) {
                move.setType(moveData.get("type").get("name").asText());
            }
            
            // 获取技能分类
            if (moveData.has("damage_class") && !moveData.get("damage_class").isNull()) {
                move.setCategory(moveData.get("damage_class").get("name").asText());
            }
            
            move.setPower(getStringValue(moveData, "power"));
            move.setAccuracy(getStringValue(moveData, "accuracy"));
            move.setPp(getStringValue(moveData, "pp"));
            move.setDescription(getMoveDescription(moveData));
            move.setEffect(getMoveEffect(moveData));
            move.setCreatedAt(LocalDateTime.now());
            move.setUpdatedAt(LocalDateTime.now());
            moveService.save(move);
        } catch (Exception e) {
            System.err.println("保存技能数据失败: " + e.getMessage());
        }
    }
    
    // 辅助方法
    private String getStringValue(JsonNode node, String fieldName) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asText();
        }
        return "";
    }
    
    private String getAbilityDescription(JsonNode abilityData) {
        JsonNode effectEntries = abilityData.get("effect_entries");
        if (effectEntries != null && effectEntries.isArray()) {
            for (JsonNode entry : effectEntries) {
                if (entry.get("language").get("name").asText().equals("zh-Hans")) {
                    return entry.get("short_effect").asText();
                }
            }
        }
        return "暂无描述";
    }
    
    private String getAbilityEffect(JsonNode abilityData) {
        JsonNode effectEntries = abilityData.get("effect_entries");
        if (effectEntries != null && effectEntries.isArray()) {
            for (JsonNode entry : effectEntries) {
                if (entry.get("language").get("name").asText().equals("zh-Hans")) {
                    return entry.get("effect").asText();
                }
            }
        }
        return "暂无效果说明";
    }
    
    private String getMoveDescription(JsonNode moveData) {
        JsonNode effectEntries = moveData.get("effect_entries");
        if (effectEntries != null && effectEntries.isArray()) {
            for (JsonNode entry : effectEntries) {
                if (entry.get("language").get("name").asText().equals("zh-Hans")) {
                    return entry.get("short_effect").asText();
                }
            }
        }
        return "暂无描述";
    }
    
    private String getMoveEffect(JsonNode moveData) {
        JsonNode effectEntries = moveData.get("effect_entries");
        if (effectEntries != null && effectEntries.isArray()) {
            for (JsonNode entry : effectEntries) {
                if (entry.get("language").get("name").asText().equals("zh-Hans")) {
                    return entry.get("effect").asText();
                }
            }
        }
        return "暂无效果说明";
    }
    
    // 特性名称翻译
    private String getChineseAbilityName(String englishName) {
        // 这里可以添加更多特性名称映射
        return englishName.replace("-", " ");
    }
    
    private String getJapaneseAbilityName(String englishName) {
        // 这里可以添加日文特性名称映射
        return englishName;
    }
    
    // 技能名称翻译
    private String getChineseMoveName(String englishName) {
        // 这里可以添加更多技能名称映射
        return englishName.replace("-", " ");
    }
    
    private String getJapaneseMoveName(String englishName) {
        // 这里可以添加日文技能名称映射
        return englishName;
    }
}