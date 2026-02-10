package com.lio9.pokedex.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.common.model.Pokemon;
import com.lio9.common.model.Move;
import com.lio9.common.model.Item;
import com.lio9.common.service.PokemonService;
import com.lio9.common.service.MoveService;
import com.lio9.common.service.ItemService;
import com.lio9.common.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 宝可梦控制器
 * 创建人: Lio9
 */
@RestController
@RequestMapping("/api/pokemon")
@CrossOrigin(origins = "*")
public class PokemonController {
    
    @Autowired
    private PokemonService pokemonService;
    
    @Autowired
    private MoveService moveService;
    
    @Autowired
    private ItemService itemService;
    
    /**
     * 分页获取宝可梦列表
     */
    @GetMapping("/list")
    public Map<String, Object> getPokemonList(PokemonQueryVO queryVO) {
        Map<String, Object> result = new HashMap<>();
        Page<Pokemon> page = new Page<>(queryVO.getCurrent(), queryVO.getSize());
        
        // 构建查询条件
        QueryWrapper<Pokemon> queryWrapper = new QueryWrapper<>();
        if (queryVO.getName() != null && !queryVO.getName().isEmpty()) {
            queryWrapper.like("name", queryVO.getName())
                       .or()
                       .like("name_en", queryVO.getName())
                       .or()
                       .like("name_jp", queryVO.getName());
        }
        queryWrapper.orderByAsc("id");
        
        Page<Pokemon> pokemonPage = pokemonService.page(page, queryWrapper);
        
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", pokemonPage);
        return result;
    }
    
    /**
     * 获取宝可梦详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getPokemonDetail(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        Pokemon pokemon = pokemonService.getById(id);
        
        if (pokemon != null) {
            PokemonDetailVO detail = buildSimplePokemonDetail(pokemon);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", detail);
        } else {
            result.put("code", 404);
            result.put("message", "宝可梦不存在");
        }
        return result;
    }
    
    /**
     * 搜索宝可梦
     */
    @GetMapping("/search")
    public Map<String, Object> searchPokemon(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        
        Map<String, Object> result = new HashMap<>();
        Page<Pokemon> page = new Page<>(current, size);
        
        QueryWrapper<Pokemon> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("name", keyword)
                   .or()
                   .like("name_en", keyword)
                   .or()
                   .like("name_jp", keyword)
                   .orderByAsc("id");
        
        Page<Pokemon> pokemonPage = pokemonService.page(page, queryWrapper);
        
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", pokemonPage);
        return result;
    }
    
    /**
     * 根据编号获取宝可梦
     */
    @GetMapping("/number/{indexNumber}")
    public Map<String, Object> getPokemonByIndexNumber(@PathVariable String indexNumber) {
        Map<String, Object> result = new HashMap<>();
        
        QueryWrapper<Pokemon> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("index_number", indexNumber);
        Pokemon pokemon = pokemonService.getOne(queryWrapper);
        
        if (pokemon != null) {
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", pokemon);
        } else {
            result.put("code", 404);
            result.put("message", "宝可梦不存在");
        }
        return result;
    }
    
    /**
     * 获取宝可梦技能列表
     */
    @GetMapping("/{id}/moves")
    public Map<String, Object> getPokemonMoves(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        // 模拟技能数据
        List<MoveVO> moves = new ArrayList<>();
        
        MoveVO move1 = new MoveVO();
        move1.setId(1L);
        move1.setName("撞击");
        move1.setNameEn("tackle");
        move1.setType("一般");
        move1.setCategory("物理");
        move1.setPower("40");
        move1.setAccuracy("100");
        move1.setPp("35");
        move1.setDescription("用身体撞向对手进行攻击。");
        moves.add(move1);
        
        MoveVO move2 = new MoveVO();
        move2.setId(2L);
        move2.setName("藤鞭");
        move2.setNameEn("vine whip");
        move2.setType("草");
        move2.setCategory("物理");
        move2.setPower("45");
        move2.setAccuracy("100");
        move2.setPp("25");
        move2.setDescription("用细长的藤蔓抽打对手。");
        moves.add(move2);
        
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", moves);
        return result;
    }
    
    /**
     * 获取宝可梦特性列表
     */
    @GetMapping("/{id}/abilities")
    public Map<String, Object> getPokemonAbilities(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        // 模拟特性数据
        List<AbilityVO> abilities = new ArrayList<>();
        
        AbilityVO ability1 = new AbilityVO();
        ability1.setId(1L);
        ability1.setName("茂盛");
        ability1.setNameEn("overgrow");
        ability1.setDescription("ＨＰ减少的时候，草属性的招式威力会提高。");
        ability1.setIsHidden(false);
        ability1.setSlot(1);
        abilities.add(ability1);
        
        AbilityVO ability2 = new AbilityVO();
        ability2.setId(2L);
        ability2.setName("叶绿素");
        ability2.setNameEn("chlorophyll");
        ability2.setDescription("天气为晴朗时，速度会提高。");
        ability2.setIsHidden(true);
        ability2.setSlot(3);
        abilities.add(ability2);
        
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", abilities);
        return result;
    }
    
    /**
     * 获取宝可梦进化链
     */
    @GetMapping("/{id}/evolution")
    public Map<String, Object> getEvolutionChain(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        // 模拟进化链数据
        List<EvolutionVO> evolutionChain = new ArrayList<>();
        
        EvolutionVO evol1 = new EvolutionVO();
        evol1.setId(1L);
        evol1.setPokemonId(1L);
        evol1.setPokemonName("妙蛙种子");
        evol1.setPokemonIndexNumber("0001");
        evol1.setEvolvesFromId(null);
        evol1.setEvolvesFromName(null);
        evol1.setEvolutionMethod("升级");
        evol1.setEvolutionParameter("等级");
        evol1.setEvolutionValue("16");
        evolutionChain.add(evol1);
        
        EvolutionVO evol2 = new EvolutionVO();
        evol2.setId(2L);
        evol2.setPokemonId(2L);
        evol2.setPokemonName("妙蛙草");
        evol2.setPokemonIndexNumber("0002");
        evol2.setEvolvesFromId(1L);
        evol2.setEvolvesFromName("妙蛙种子");
        evol2.setEvolutionMethod("升级");
        evol2.setEvolutionParameter("等级");
        evol2.setEvolutionValue("32");
        evolutionChain.add(evol2);
        
        EvolutionVO evol3 = new EvolutionVO();
        evol3.setId(3L);
        evol3.setPokemonId(3L);
        evol3.setPokemonName("妙蛙花");
        evol3.setPokemonIndexNumber("0003");
        evol3.setEvolvesFromId(2L);
        evol3.setEvolvesFromName("妙蛙草");
        evol3.setEvolutionMethod("升级");
        evol3.setEvolutionParameter("等级");
        evol3.setEvolutionValue("-1");
        evolutionChain.add(evol3);
        
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", evolutionChain);
        return result;
    }
    
    /**
     * 构建完整的宝可梦详情数据
     */
    private PokemonDetailVO buildSimplePokemonDetail(Pokemon pokemon) {
        PokemonDetailVO detail = new PokemonDetailVO();
        detail.setId(pokemon.getId());
        detail.setIndexNumber(pokemon.getIndexNumber());
        detail.setName(pokemon.getName());
        detail.setNameEn(pokemon.getNameEn());
        detail.setNameJp(pokemon.getNameJp());
        detail.setProfile(pokemon.getProfile());
        detail.setGenus("宝可梦");
        
        // 构建完整的形态信息
        List<PokemonFormVO> formVOs = new ArrayList<>();
        PokemonFormVO formVO = new PokemonFormVO();
        formVO.setId(pokemon.getId());
        formVO.setName(pokemon.getName());
        formVO.setIndexNumber(pokemon.getIndexNumber());
        formVO.setHp(45);
        formVO.setAttack(49);
        formVO.setDefense(49);
        formVO.setSpAttack(65);
        formVO.setSpDefense(65);
        formVO.setSpeed(45);
        
        // 添加示例属性数据
        List<TypeVO> types = new ArrayList<>();
        TypeVO type1 = new TypeVO();
        type1.setId(1L);
        type1.setName("草");
        type1.setNameEn("grass");
        type1.setNameJp("くさ");
        type1.setColor("#78C850");
        types.add(type1);
        
        TypeVO type2 = new TypeVO();
        type2.setId(2L);
        type2.setName("毒");
        type2.setNameEn("poison");
        type2.setNameJp("どく");
        type2.setColor("#A040A0");
        types.add(type2);
        
        formVO.setTypes(types);
        formVOs.add(formVO);
        
        detail.setForms(formVOs);
        return detail;
    }
    
    /**
     * 手动初始化数据库
     */
    @GetMapping("/init-data")
    public Map<String, Object> initDatabase() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 初始化技能数据
            initMoveData();
            // 初始化物品数据
            initItemData();
            result.put("code", 200);
            result.put("message", "数据库初始化成功！");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "数据库初始化失败：" + e.getMessage());
        }
        return result;
    }
    
    /**
     * 初始化技能数据
     */
    private void initMoveData() {
        // 检查是否已有数据
        if (moveService.count() > 0) {
            return;
        }
        
        // 创建示例技能数据
        List<Move> moves = Arrays.asList(
            new Move().setId(1L).setName("撞击").setNameEn("tackle").setCategory("物理").setPower(40).setAccuracy(100).setPp(35).setDescription("用身体撞向对手进行攻击。"),
            new Move().setId(2L).setName("藤鞭").setNameEn("vine whip").setCategory("物理").setPower(45).setAccuracy(100).setPp(25).setDescription("用细长的藤蔓抽打对手。"),
            new Move().setId(3L).setName("毒粉").setNameEn("poison powder").setCategory("变化").setPower(null).setAccuracy(75).setPp(35).setDescription("撒出毒粉，让对手陷入中毒状态。"),
            new Move().setId(4L).setName("寄生种子").setNameEn("leech seed").setCategory("变化").setPower(null).setAccuracy(90).setPp(10).setDescription("植入寄生种子，每回合吸取对手的ＨＰ。"),
            new Move().setId(5L).setName("水枪").setNameEn("water gun").setCategory("特殊").setPower(40).setAccuracy(100).setPp(25).setDescription("喷射水流攻击对手。"),
            new Move().setId(6L).setName("火焰拳").setNameEn("fire punch").setCategory("物理").setPower(75).setAccuracy(100).setPp(15).setDescription("让拳头燃烧火焰向对手突击，有时会让对手陷入灼伤状态。")
        );
        
        // 批量插入技能数据
        for (Move move : moves) {
            moveService.save(move);
        }
    }
    
    /**
     * 初始化物品数据
     */
    private void initItemData() {
        // 检查是否已有数据
        if (itemService.count() > 0) {
            return;
        }
        
        // 创建示例物品数据
        List<Item> items = Arrays.asList(
            new Item().setId(1L).setName("精灵球").setNameEn("Poké Ball").setCategory("其他").setPrice(200).setDescription("用于捕捉宝可梦的球。"),
            new Item().setId(2L).setName("高级球").setNameEn("Great Ball").setCategory("其他").setPrice(600).setDescription("比精灵球更好用的捕捉球。"),
            new Item().setId(3L).setName("超级球").setNameEn("Ultra Ball").setCategory("其他").setPrice(1200).setDescription("最强大的捕捉球。"),
            new Item().setId(4L).setName("生命球").setNameEn("Potion").setCategory("药水").setPrice(300).setDescription("回复宝可梦的一般ＨＰ。"),
            new Item().setId(5L).setName("全复球").setNameEn("Full Restore").setCategory("药水").setPrice(3000).setDescription("回复宝可梦的所有ＨＰ和状态。")
        );
        
        // 批量插入物品数据
        for (Item item : items) {
            itemService.save(item);
        }
    }
}
