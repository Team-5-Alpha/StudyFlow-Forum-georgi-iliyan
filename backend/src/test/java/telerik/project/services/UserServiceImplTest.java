package telerik.project.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import telerik.project.exceptions.EntityDuplicateException;
import telerik.project.exceptions.EntityNotFoundException;
import telerik.project.models.Post;
import telerik.project.models.Role;
import telerik.project.models.User;
import telerik.project.repositories.UserRepository;
import telerik.project.services.contracts.NotificationService;
import telerik.project.services.contracts.PostService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostService postService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserServiceImpl userService;

    private User acting;

    @BeforeEach
    void setUp() {
        acting = new User();
        acting.setId(1L);
        acting.setFollowing(new java.util.HashSet<>());
        acting.setFollowers(new java.util.HashSet<>());
    }

    @Test
    void create_normalizes_and_saves() {
        User u = new User();
        u.setEmail("TEST@EXAMPLE.COM");
        u.setUsername("UserName");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("username")).thenReturn(false);
        when(userRepository.save(u)).thenAnswer(invocation -> invocation.getArgument(0));

        userService.create(u);

        assertEquals("test@example.com", u.getEmail());
        assertEquals("username", u.getUsername());
        verify(userRepository).save(u);
    }

    @Test
    void followUser_whenAlreadyFollowing_throws() {
        User target = new User(); target.setId(2L);
        acting.getFollowing().add(target);

        when(userRepository.findById(acting.getId())).thenReturn(Optional.of(acting));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThrows(EntityDuplicateException.class, () -> userService.followUser(2L, acting));
    }

    @Test
    void followUser_success_savesAndSendsNotification() {
        User target = new User(); target.setId(3L);
        when(userRepository.findById(acting.getId())).thenReturn(Optional.of(acting));
        when(userRepository.findById(3L)).thenReturn(Optional.of(target));
        when(userRepository.save(acting)).thenAnswer(invocation -> invocation.getArgument(0));

        userService.followUser(3L, acting);

        assertTrue(acting.getFollowing().contains(target));
        verify(userRepository).save(acting);
        verify(notificationService).send(eq(acting), eq(target), eq(acting.getId()), eq("USER"), eq("FOLLOW"));
    }

    @Test
    void unfollowUser_notFollowing_throws() {
        User target = new User(); target.setId(4L);
        when(userRepository.findById(acting.getId())).thenReturn(Optional.of(acting));
        when(userRepository.findById(4L)).thenReturn(Optional.of(target));

        assertThrows(EntityNotFoundException.class, () -> userService.unfollowUser(4L, acting));
    }

    @Test
    void blockUser_setsBlocked_and_sends() {
        User admin = new User(); admin.setId(99L); admin.setRole(Role.ADMIN);
        User target = new User(); target.setId(5L);

        when(userRepository.findById(5L)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenAnswer(invocation -> invocation.getArgument(0));

        userService.blockUser(5L, admin);

        assertTrue(target.isBlocked());
        verify(userRepository).save(target);
        verify(notificationService).send(eq(admin), eq(target), eq(5L), eq("USER"), eq("BLOCK"));
    }

    @Test
    void getFollowers_and_counts() {
        User target = new User(); target.setId(6L);
        User follower = new User(); follower.setId(7L);
        target.getFollowers().add(follower);

        when(userRepository.findById(6L)).thenReturn(Optional.of(target));

        assertEquals(1, userService.getFollowers(6L).size());
        assertEquals(1, userService.countFollowers(6L));
        assertEquals(0, userService.countFollowing(6L));
    }

    @Test
    void getById_missing_throws() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(telerik.project.exceptions.EntityNotFoundException.class, () -> userService.getById(999L));
    }

    @Test
    void getByUsername_missing_throws() {
        when(userRepository.findByUsername("nope")).thenReturn(Optional.empty());
        assertThrows(telerik.project.exceptions.EntityNotFoundException.class, () -> userService.getByUsername("nope"));
    }

    @Test
    void getByEmail_missing_throws() {
        when(userRepository.findByEmail("noone@example.com")).thenReturn(Optional.empty());
        assertThrows(telerik.project.exceptions.EntityNotFoundException.class, () -> userService.getByEmail("noone@example.com"));
    }

    @Test
    void create_usernameOrEmailTaken_throws() {
        User u = new User();
        u.setEmail("a@b.com"); u.setUsername("Me");

        when(userRepository.existsByUsername("me")).thenReturn(true);

        assertThrows(telerik.project.exceptions.EntityDuplicateException.class, () -> userService.create(u));

        // email taken scenario
        when(userRepository.existsByUsername("me")).thenReturn(false);
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        assertThrows(telerik.project.exceptions.EntityDuplicateException.class, () -> userService.create(u));
    }

    @Test
    void update_adminCopiesPhoneNumber_and_saves() {
        User existing = new User(); existing.setId(10L); existing.setRole(telerik.project.models.Role.ADMIN);
        existing.setEmail("old@x.com");

        User acting = existing; // owner
        User updated = new User(); updated.setFirstName("F"); updated.setLastName("L"); updated.setEmail("old@x.com"); updated.setPhoneNumber("12345");

        when(userRepository.findById(10L)).thenReturn(Optional.of(existing));

        userService.update(10L, updated, acting);

        verify(userRepository).save(existing);
        assertEquals("12345", existing.getPhoneNumber());
    }

    @Test
    void delete_owner_callsRepositoryDelete() {
        User target = new User(); target.setId(20L);
        User acting = target;
        when(userRepository.findById(20L)).thenReturn(Optional.of(target));

        userService.delete(20L, acting);

        verify(userRepository).delete(target);
    }

    @Test
    void block_unblock_and_promote_flow() {
        User admin = new User(); admin.setId(99L); admin.setRole(telerik.project.models.Role.ADMIN);
        User target = new User(); target.setId(21L); target.setIsBlocked(false); target.setRole(telerik.project.models.Role.USER);

        when(userRepository.findById(21L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.blockUser(21L, admin);
        assertTrue(target.isBlocked());
        verify(notificationService).send(eq(admin), eq(target), eq(21L), eq("USER"), eq("BLOCK"));

        // prepare for unblock
        target.setIsBlocked(true);
        when(userRepository.findById(21L)).thenReturn(Optional.of(target));

        userService.unblockUser(21L, admin);
        assertFalse(target.isBlocked());
        verify(notificationService).send(eq(admin), eq(target), eq(21L), eq("USER"), eq("UNBLOCK"));

        // promote
        when(userRepository.findById(21L)).thenReturn(Optional.of(target));
        userService.promoteToAdmin(21L, admin);
        assertEquals(telerik.project.models.Role.ADMIN, target.getRole());
        verify(notificationService).send(eq(admin), eq(target), eq(21L), eq("ADMIN"), eq("PROMOTE"));
    }

    @Test
    void unfollowUser_success_removesAndSaves() {
        User target = new User(); target.setId(30L);
        User actingUser = new User(); actingUser.setId(31L);
        actingUser.setFollowing(new java.util.HashSet<>());
        actingUser.getFollowing().add(target);

        when(userRepository.findById(actingUser.getId())).thenReturn(Optional.of(actingUser));
        when(userRepository.findById(30L)).thenReturn(Optional.of(target));
        when(userRepository.save(actingUser)).thenAnswer(invocation -> invocation.getArgument(0));

        userService.unfollowUser(30L, actingUser);

        assertFalse(actingUser.getFollowing().contains(target));
        verify(userRepository).save(actingUser);
    }
}
