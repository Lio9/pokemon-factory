package com.lio9.battle.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class BattleEngine {
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 更完整的对战引擎（单人单对单）：
     * - 支持每只宝可梦带 moves (name, power, type)
     * - 对手出招由简单 AI 决定（高威力优先；低血量时倾向 team_shield）
     * - 联防逻辑(team_shield)：全队在下一次受到的伤害降低 50%
     * - 同时出招并结算，记录每回合详细日志
     */
    public Map<String, Object> simulate(String playerTeamJson, String opponentTeamJson, int maxRounds, Map<String, String> playerMoveMap) {
        List<Map<String, Object>> pTeam = parseTeam(playerTeamJson);
        List<Map<String, Object>> oTeam = parseTeam(opponentTeamJson);

        // 初始化每只宝可梦的 HP 与 moves
        List<MonState> pStates = initStates(pTeam);
        List<MonState> oStates = initStates(oTeam);

        boolean pTeamShield = false; // 是否在本回合生效（由上一回合的 linked_defense 触发）
        boolean oTeamShield = false;
        List<Map<String, Object>> rounds = new ArrayList<>();
        Random rnd = new Random();
        int round = 0;

        while (round < maxRounds && hasAlive(pStates) && hasAlive(oStates)) {
            round++;
            // 选择出战宝可梦（第一个未倒下的）
            MonState pActive = firstAlive(pStates);
            MonState oActive = firstAlive(oStates);
            if (pActive == null || oActive == null) break;

            // 玩家出招：优先选择第一个 move（客户端可扩展为策略）
            Move pMove = selectPlayerMove(pActive, playerMoveMap);

            // 对手出招由 AI 决定
            Move oMove = selectAIMove(oStates, pStates, oActive, rnd);

            // 记录出招信息
            Map<String, Object> r = new HashMap<>();
            r.put("round", round);
            r.put("playerMon", pActive.name);
            r.put("opponentMon", oActive.name);
            r.put("playerMove", pMove.name);
            r.put("opponentMove", oMove.name);

            // 处理联防（team_shield / protect）——如果本回合对方有盾则减伤
            double pDamageMultiplier = oTeamShield ? 0.5 : 1.0;
            double oDamageMultiplier = pTeamShield ? 0.5 : 1.0;

            // 计算伤害
            int dmgToO = calculateDamage(pActive, oActive, pMove, rnd);
            int dmgToP = calculateDamage(oActive, pActive, oMove, rnd);

            // 应用盾牌减伤
            dmgToO = (int) Math.max(0, Math.round(dmgToO * oDamageMultiplier));
            dmgToP = (int) Math.max(0, Math.round(dmgToP * pDamageMultiplier));

            // 如果 move 是 team_shield / protect，则设置下一回合的 shield（联防）
            boolean pSetShieldNext = "team_shield".equalsIgnoreCase(pMove.name) || "protect".equalsIgnoreCase(pMove.name);
            boolean oSetShieldNext = "team_shield".equalsIgnoreCase(oMove.name) || "protect".equalsIgnoreCase(oMove.name);

            // 扣血（同时发生）
            oActive.currentHp -= dmgToO;
            pActive.currentHp -= dmgToP;

            r.put("dmgToOpponent", dmgToO);
            r.put("dmgToPlayer", dmgToP);
            r.put("playerHpAfter", Math.max(0, pActive.currentHp));
            r.put("opponentHpAfter", Math.max(0, oActive.currentHp));

            // 记录是否被击倒
            if (oActive.currentHp <= 0) r.put("opponentFainted", true);
            if (pActive.currentHp <= 0) r.put("playerFainted", true);

            rounds.add(r);

            // 设置招式冷却（简单策略：team_shield/protect 有 2 回合冷却）
            if (pMove != null) {
                if ("team_shield".equalsIgnoreCase(pMove.name) || "protect".equalsIgnoreCase(pMove.name)) {
                    pActive.cooldowns.put(pMove.name, 2);
                }
            }
            if (oMove != null) {
                if ("team_shield".equalsIgnoreCase(oMove.name) || "protect".equalsIgnoreCase(oMove.name)) {
                    oActive.cooldowns.put(oMove.name, 2);
                }
            }

            // 回合结束，所有宝可梦冷却 -1
            pStates.forEach(s -> s.decrementCooldowns());
            oStates.forEach(s -> s.decrementCooldowns());

            // 更新 teamShield 状态：本回合生效的盾在回合结算后失效；下一回合由队内出招触发
            pTeamShield = pSetShieldNext;
            oTeamShield = oSetShieldNext;
        }

        // 计算最终胜者
        boolean playerWon = hasAlive(pStates) && !hasAlive(oStates);
        if (!playerWon && hasAlive(pStates) && hasAlive(oStates)) { // 平局随机决出
            playerWon = new Random().nextBoolean();
        }

        Map<String, Object> out = new HashMap<>();
        out.put("rounds", rounds);
        out.put("winner", playerWon ? "player" : "opponent");
        out.put("playerRemaining", aliveCount(pStates));
        out.put("opponentRemaining", aliveCount(oStates));
        out.put("roundsCount", rounds.size());
        return out;
    }

    // 初始化队伍状态，从 json 中读取 hp 和 moves（若无则根据 base_experience 生成）
    @SuppressWarnings("unchecked")
    private List<MonState> initStates(List<Map<String, Object>> team) {
        List<MonState> out = new ArrayList<>();
        for (Map<String, Object> m : team) {
            String name = m.getOrDefault("name", "unk").toString();
            int be = 10;
            if (m.get("base_experience") instanceof Number) be = ((Number) m.get("base_experience")).intValue();
            int hp = Math.max(20, be * 2);
            List<Move> moves = new ArrayList<>();
            Object mv = m.get("moves");
            if (mv instanceof List) {
                for (Object o : (List<?>) mv) {
                    if (o instanceof Map) {
                        Map<String, Object> mm = (Map<String, Object>) o;
                        String mn = mm.getOrDefault("name", "Tackle").toString();
                        int power = mm.getOrDefault("power", 10) instanceof Number ? ((Number) mm.get("power")).intValue() : 10;
                        moves.add(new Move(mn, power));
                    }
                }
            }
            if (moves.isEmpty()) moves.add(new Move("Tackle", Math.max(8, be / 5)));
            out.add(new MonState(name, hp, moves, be));
        }
        return out;
    }

    private boolean hasAlive(List<MonState> states) {
        return states.stream().anyMatch(s -> s.currentHp > 0);
    }

    private int aliveCount(List<MonState> states) {
        return (int) states.stream().filter(s -> s.currentHp > 0).count();
    }

    private MonState firstAlive(List<MonState> states) {
        return states.stream().filter(s -> s.currentHp > 0).findFirst().orElse(null);
    }

    private Move selectPlayerMove(MonState s, Map<String, String> playerMoveMap) {
        // 优先根据 playerMoveMap 指定的 move 名称或索引选择，但跳过处于冷却中的招式
        if (playerMoveMap != null && playerMoveMap.containsKey(s.name)) {
            String val = playerMoveMap.get(s.name);
            // try name
            for (Move mv : s.moves) {
                if (mv.name.equalsIgnoreCase(val) && s.cooldowns.getOrDefault(mv.name,0) <= 0) {
                    return mv;
                }
            }
            // try index
            try {
                int idx = Integer.parseInt(val);
                if (idx >= 0 && idx < s.moves.size()) {
                    Move m = s.moves.get(idx);
                    if (s.cooldowns.getOrDefault(m.name,0) <= 0) return m;
                }
            } catch (NumberFormatException ignored) {}
        }
        // 回退：选择第一个非冷却的招式
        for (Move mv : s.moves) {
            if (s.cooldowns.getOrDefault(mv.name,0) <= 0) return mv;
        }
        // 若全部在冷却，仍选择第一招
        return s.moves.get(0);
    }

    private Move selectAIMove(List<MonState> myTeam, List<MonState> enemyTeam, MonState active, Random rnd) {
        // 如果团队总体血量偏低，则倾向于 team_shield（若存在该招式）
        int totalHp = myTeam.stream().mapToInt(m -> m.currentHp).sum();
        int totalMax = myTeam.stream().mapToInt(m -> m.maxHp).sum();
        double hpRatio = totalMax == 0 ? 0 : (double) totalHp / totalMax;

        // 找到最高威力的招式
        Move best = active.moves.get(0);
        for (Move mv : active.moves) {
            if (mv.power > best.power) best = mv;
        }

        // 如果血量低且存在 team_shield，则使用
        for (Move mv : active.moves) {
            if ((mv.name.equalsIgnoreCase("team_shield") || mv.name.equalsIgnoreCase("protect")) && hpRatio < 0.45) {
                return mv;
            }
        }

        // 小概率使用次优招式以制造多样性
        if (rnd.nextDouble() < 0.15 && active.moves.size() > 1) {
            return active.moves.get(rnd.nextInt(active.moves.size()));
        }
        return best;
    }

    private int calculateDamage(MonState attacker, MonState target, Move move, Random rnd) {
        // 基础伤害：move.power + attacker.be/10 + 随机项
        int base = move.power + Math.max(0, attacker.baseExp / 10) + (rnd.nextInt(7) - 3);
        return Math.max(0, base);
    }

    private List<Map<String, Object>> parseTeam(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        try {
            Object o = mapper.readValue(json, Object.class);
            if (o instanceof List) return (List<Map<String, Object>>) o;
            return Collections.singletonList((Map<String, Object>) o);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // 内部状态类
    private static class MonState {
        final String name;
        final int maxHp;
        int currentHp;
        final List<Move> moves;
        final int baseExp;
        final java.util.Map<String, Integer> cooldowns = new java.util.HashMap<>();

        MonState(String name, int maxHp, List<Move> moves, int baseExp) {
            this.name = name;
            this.maxHp = maxHp;
            this.currentHp = maxHp;
            this.moves = moves;
            this.baseExp = baseExp;
        }

        void decrementCooldowns() {
            var it = cooldowns.entrySet().iterator();
            java.util.List<String> toUpdate = new java.util.ArrayList<>();
            while (it.hasNext()) {
                var e = it.next();
                int v = e.getValue() - 1;
                if (v <= 0) toUpdate.add(e.getKey()); else toUpdate.add(e.getKey());
                cooldowns.put(e.getKey(), Math.max(0, v));
            }
        }
    }

    private static class Move {
        final String name;
        final int power;

        Move(String name, int power) {
            this.name = name;
            this.power = power;
        }
    }
}
