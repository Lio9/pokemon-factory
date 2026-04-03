package com.lio9.battle.controller;

import com.lio9.battle.service.BattleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(BattleController.class)
public class BattleControllerExchangeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BattleService battleService;

    @Test
    void exchangeEndpointReturnsOk() throws Exception {
        // Mock service to return true for any exchange
        when(battleService.exchange(any())).thenReturn(true);

        String payload = "{\"battleId\":1,\"playerTeamId\":2,\"opponentId\":3,\"replacedIndex\":0,\"selectedOpponentPokemonId\":101}";

        mockMvc.perform(post("/api/battle/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
