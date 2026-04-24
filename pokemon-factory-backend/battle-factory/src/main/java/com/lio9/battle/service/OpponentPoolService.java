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
        int low = Math.max(0, rank - window);
        int high = rank + window;
        return opponentPoolMapper.sample(low, high, limit, rank);
    }

    /**
     * 清理过旧的对手池记录。
     */
    public void cleanupOlderThanDays(int days) {
        opponentPoolMapper.cleanupOlderThan("-" + days + " days");
    }
}
