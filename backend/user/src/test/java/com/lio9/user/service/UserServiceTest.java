package com.lio9.user.service;



import com.lio9.user.dto.AuthRequest;
import com.lio9.user.dto.AuthResponse;
import com.lio9.user.mapper.UserMapper;
import com.lio9.user.model.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper, 24);
        userService.init();
    }

    @Test
    void register_createsUserAndReturnsSession() {
        UserAccount account = userAccount(7L, "Ash", new BCryptPasswordEncoder().encode("pikachu123"));
        when(userMapper.findByUsername("Ash")).thenReturn(null, account, account);

        AuthResponse response = userService.register(new AuthRequest(" Ash ", "pikachu123"));

        assertNotNull(response.token());
        assertEquals("Ash", response.user().username());
        verify(userMapper).insertUser(
                org.mockito.ArgumentMatchers.eq("Ash"),
                org.mockito.ArgumentMatchers.eq("Ash"),
                argThat(hash -> hash != null && !hash.equals("pikachu123"))
        );
        verify(userMapper).touchLogin(7L);
    }

    @Test
    void login_returnsSessionWhenPasswordMatches() {
        String passwordHash = new BCryptPasswordEncoder().encode("pikachu123");
        UserAccount account = userAccount(9L, "Red", passwordHash);
        when(userMapper.findByUsername("Red")).thenReturn(account, account);

        AuthResponse response = userService.login(new AuthRequest("Red", "pikachu123"));

        assertEquals("Red", response.user().username());
        assertNotNull(response.token());
        verify(userMapper).touchLogin(9L);
    }

    @Test
    void getCurrentUser_throwsWhenUserDoesNotExist() {
        when(userMapper.findByUsername("Blue")).thenReturn(null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> userService.getCurrentUser("Blue"));

        assertEquals(401, error.getStatusCode().value());
    }

    @Test
    void validateToken_returnsNullForInvalidToken() {
        assertNull(userService.validateTokenAndGetUsername("not-a-jwt"));
    }

    @Test
    void login_rejectsWrongPassword() {
        UserAccount account = userAccount(10L, "Leaf", new BCryptPasswordEncoder().encode("correct-password"));
        when(userMapper.findByUsername("Leaf")).thenReturn(account);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> userService.login(new AuthRequest("Leaf", "wrong-password")));

        assertEquals(401, error.getStatusCode().value());
    }

    private UserAccount userAccount(Long id, String username, String passwordHash) {
        UserAccount account = new UserAccount();
        account.setId(id);
        account.setUsername(username);
        account.setDisplayName(username);
        account.setPasswordHash(passwordHash);
        return account;
    }
}
