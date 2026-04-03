package com.lio9.battle.service;

import com.lio9.battle.mapper.OpponentPoolMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class OpponentPoolService {
    private final OpponentPoolMapper opponentPoolMapper;

    public OpponentPoolService(OpponentPoolMapper opponentPoolMapper) {
        this.opponentPoolMapper = opponentPoolMapper;
    }

    public void addTeamToPool(Integer teamId, Integer rank) {
        if (teamId == null) return;
        opponentPoolMapper.addTeam(teamId, rank == null ? 0 : rank);
    }

    public List<Map<String, Object>> sample(int rank, int window, int limit) {
        int low = Math.max(0, rank - window);
        int high = rank + window;
        return opponentPoolMapper.sample(low, high, limit);
    }

    public void cleanupOlderThanDays(int days) {
        opponentPoolMapper.cleanupOlderThan("-" + days + " days");
    }
}
