package com.lio9.battle.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class BattleEngine {
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 简单回合模拟器：基于团队 "strength" 值进行伤害交换，记录回合日志
     * 可替换为更复杂的 VGC 双打引擎。保留扩展点。
     */
    public Map<String, Object> simulate(String playerTeamJson, String opponentTeamJson, int maxRounds) {
        List<Map<String, Object>> pTeam = parseTeam(playerTeamJson);
        List<Map<String, Object>> oTeam = parseTeam(opponentTeamJson);

        int pScore = computeStrength(pTeam);
        int oScore = computeStrength(oTeam);

        List<Map<String, Object>> rounds = new ArrayList<>();
        Random rnd = new Random();
        int pHP = Math.max(1, pScore);
        int oHP = Math.max(1, oScore);
        int roundsCount = 0;

        while (roundsCount < maxRounds && pHP > 0 && oHP > 0) {
            roundsCount++;
            // 基于强度和随机因子计算伤害
            int dmgToO = Math.max(1, rnd.nextInt(10) + Math.max(0, pScore / 50));
            int dmgToP = Math.max(1, rnd.nextInt(10) + Math.max(0, oScore / 50));

            oHP -= dmgToO;
            pHP -= dmgToP;

            Map<String, Object> r = new HashMap<>();
            r.put("round", roundsCount);
            r.put("dmgToOpponent", dmgToO);
            r.put("dmgToPlayer", dmgToP);
            r.put("playerHP", Math.max(pHP, 0));
            r.put("opponentHP", Math.max(oHP, 0));
            rounds.add(r);
        }

        boolean playerWon;
        if (pHP > 0 && oHP <= 0) playerWon = true;
        else if (oHP > 0 && pHP <= 0) playerWon = false;
        else playerWon = rnd.nextBoolean();

        Map<String, Object> out = new HashMap<>();
        out.put("rounds", rounds);
        out.put("winner", playerWon ? "player" : "opponent");
        out.put("playerStrength", pScore);
        out.put("opponentStrength", oScore);
        out.put("roundsCount", roundsCount);
        return out;
    }

    @SuppressWarnings("unchecked")
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

    private int computeStrength(List<Map<String, Object>> team) {
        int s = 0;
        for (Map<String, Object> p : team) {
            Object be = p.get("base_experience");
            if (be instanceof Number) s += ((Number) be).intValue();
            else {
                Object id = p.get("id");
                if (id instanceof Number) s += ((Number) id).intValue();
                else if (p.get("name") != null) s += Math.min(200, p.get("name").toString().length() * 10);
            }
        }
        return Math.max(10, s);
    }
}
