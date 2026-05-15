package com.lio9.battle.service;



import com.lio9.battle.mapper.OpponentPoolMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 对手池服务。
 * <p>
 * 胜利后的玩家队伍会被写入对手池，供后续匹配时抽样复用，
 * 从而让对战工厂拥有一部分“玩家养成反哺对局”的感觉。
 * </p>
 */
@Service
public class OpponentPoolService {
    private final OpponentPoolMapper opponentPoolMapper;

    public OpponentPoolService(OpponentPoolMapper opponentPoolMapper) {
        this.opponentPoolMapper = opponentPoolMapper;
    }

    /**
     * 把一支队伍加入对手池。
     */
    public void addTeamToPool(Integer teamId, Integer rank) {
        if (teamId == null) return;
        opponentPoolMapper.addTeam(teamId, rank == null ? 0 : rank);
    }

    /**
     * 按段位窗口抽样对手池。
     */
    public List<Map<String, Object>> sample(int rank, int window, int limit) {
        // 严格限制段位窗口，防止低段位玩家遭遇过强对手
        int low = Math.max(0, rank - window);
        int high = rank + window;
        return opponentPoolMapper.sample(low, high, limit, rank);
    }

    /**
     * 根据配置化的段位限制进行抽样。
     */
    public List<Map<String, Object>> sampleWithConfig(int rank, int window, int limit,
                                                       com.lio9.battle.config.BattleConfig.TierRestrictionConfig config) {
        if (config.isStrictMode()) {
            // 严格模式下，绝不超出配置的最低/最高段位
            int minRank = config.getMinTier() * 30; // 假设每段位约 30 分
            int maxRank = config.getMaxTier() * 30;
            int low = Math.max(minRank, rank - window);
            int high = Math.min(maxRank, rank + window);
            return opponentPoolMapper.sample(low, high, limit, rank);
        }
        return sample(rank, window, limit);
    }

    /**
     * 清理过旧的对手池记录。
     */
    public void cleanupOlderThanDays(int days) {
        opponentPoolMapper.cleanupOlderThan("-" + days + " days");
    }
}
