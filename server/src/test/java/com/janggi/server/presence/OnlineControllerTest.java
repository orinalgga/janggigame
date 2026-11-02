package com.janggi.server.presence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.janggi.server.game.GameService;
import com.janggi.server.user.User;
import com.janggi.server.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.test.util.ReflectionTestUtils;

class OnlineControllerTest {

    @Test
    void listsConnectedMembersFreeFirstAndSkipsGuestsAndBanned() {
        SimpUserRegistry registry = mock(SimpUserRegistry.class);
        Set<SimpUser> connected = Set.of(simp("1"), simp("2"), simp("3"), simp("guest-abc"));
        when(registry.getUsers()).thenReturn(connected);
        User a = user(1, "가나", 1300);
        User b = user(2, "다라", 1500);
        User c = user(3, "마바", 1400);
        c.ban("욕설", OffsetDateTime.now());
        UserRepository users = mock(UserRepository.class);
        when(users.findAllById(any())).thenReturn(List.of(a, b, c));
        GameService games = mock(GameService.class);
        when(games.playingOnline()).thenReturn(Set.of(2L));

        OnlineController.OnlineDto dto = new OnlineController(registry, users, games).online();

        assertEquals(2, dto.count());
        assertEquals("가나", dto.users().get(0).nickname());
        assertFalse(dto.users().get(0).playing());
        assertEquals("다라", dto.users().get(1).nickname());
        assertTrue(dto.users().get(1).playing());
    }

    private static SimpUser simp(String name) {
        SimpUser u = mock(SimpUser.class);
        when(u.getName()).thenReturn(name);
        return u;
    }

    private static User user(long id, String nickname, int rating) {
        User u = new User(nickname, OffsetDateTime.now());
        ReflectionTestUtils.setField(u, "id", id);
        u.changeRating(rating - User.INITIAL_RATING);
        return u;
    }
}
