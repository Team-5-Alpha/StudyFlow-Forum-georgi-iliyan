package telerik.project.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import telerik.project.exceptions.EntityDuplicateException;
import telerik.project.exceptions.EntityNotFoundException;
import telerik.project.models.Tag;
import telerik.project.models.User;
import telerik.project.repositories.TagRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagServiceImpl tagService;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = new User(); admin.setId(1L); admin.setRole(telerik.project.models.Role.ADMIN);
    }

    @Test
    void getAll_delegates() {
        Tag t1 = new Tag(); t1.setId(1L);
        when(tagRepository.findAll()).thenReturn(List.of(t1));

        assertEquals(1, tagService.getAll().size());
        verify(tagRepository).findAll();
    }

    @Test
    void getById_missing_throws() {
        when(tagRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> tagService.getById(5L));
    }

    @Test
    void createIfNotExists_returnsExistingIfFound() {
        Tag existing = new Tag(); existing.setId(2L); existing.setName("java");
        when(tagRepository.findByNameIgnoreCase("java")).thenReturn(Optional.of(existing));

        Tag res = tagService.createIfNotExists("java");
        assertEquals(existing, res);
        verify(tagRepository, never()).save(any());
    }

    @Test
    void createIfNotExists_createsWhenMissing() {
        when(tagRepository.findByNameIgnoreCase("new")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag t = invocation.getArgument(0);
            t.setId(99L);
            return t;
        });

        Tag res = tagService.createIfNotExists("new");
        assertEquals(99L, res.getId());
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    void update_nameConflict_throws() {
        Tag existing = new Tag(); existing.setId(10L); existing.setName("old");
        Tag updated = new Tag(); updated.setName("conflict");

        when(tagRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(tagRepository.existsByNameIgnoreCase(updated.getName())).thenReturn(true);

        assertThrows(EntityDuplicateException.class, () -> tagService.update(10L, updated, admin));
    }

    @Test
    void delete_callsRepositoryDelete() {
        Tag t = new Tag(); t.setId(20L);
        when(tagRepository.findById(20L)).thenReturn(Optional.of(t));

        tagService.delete(20L, admin);

        verify(tagRepository).delete(t);
    }
}

