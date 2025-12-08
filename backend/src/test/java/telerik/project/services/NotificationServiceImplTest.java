package telerik.project.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import telerik.project.exceptions.EntityNotFoundException;
import telerik.project.models.Notification;
import telerik.project.models.User;
import telerik.project.models.filters.NotificationFilterOptions;
import telerik.project.repositories.NotificationRepository;
import telerik.project.repositories.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User acting;

    @BeforeEach
    void setUp() {
        acting = new User(); acting.setId(1L);
    }

    @Test
    void getAll_delegatesAndValidates() {
        NotificationFilterOptions opts = new NotificationFilterOptions();
        when(userRepository.findById(1L)).thenReturn(Optional.of(acting));
        when(notificationRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Notification>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new Notification())));

        List<Notification> res = notificationService.getAll(1L, opts);
        assertEquals(1, res.size());
    }

    @Test
    void getById_whenNotOwned_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(acting));
        Notification n = new Notification(); n.setId(5L);
        User other = new User(); other.setId(9L);
        n.setRecipient(other);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(n));

        assertThrows(telerik.project.exceptions.AuthorizationException.class, () -> notificationService.getById(1L, 5L));
    }

    @Test
    void markAsRead_setsAndSaves() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(acting));
        Notification n = new Notification(); n.setId(6L); n.setIsRead(false); n.setRecipient(acting);
        when(notificationRepository.findById(6L)).thenReturn(Optional.of(n));

        notificationService.markAsRead(1L, 6L);

        assertTrue(n.getIsRead());
        verify(notificationRepository).save(n);
    }

    @Test
    void markAllAsRead_savesAll() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(acting));
        Notification n1 = new Notification(); n1.setId(7L); n1.setIsRead(false); n1.setRecipient(acting);
        when(notificationRepository.findByRecipient_IdAndIsReadFalse(1L)).thenReturn(List.of(n1));

        notificationService.markAllAsRead(1L);

        assertTrue(n1.getIsRead());
        verify(notificationRepository).saveAll(any());
    }

    @Test
    void send_createsAndSaves() {
        User a = new User(); a.setId(2L);
        User r = new User(); r.setId(3L);

        notificationService.send(a, r, 10L, "POST", "CREATE");
        verify(notificationRepository).save(any());
    }
}
