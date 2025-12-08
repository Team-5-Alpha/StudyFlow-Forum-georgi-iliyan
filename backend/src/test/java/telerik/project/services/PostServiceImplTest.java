package telerik.project.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import telerik.project.exceptions.EntityNotFoundException;
import telerik.project.models.Post;
import telerik.project.models.Tag;
import telerik.project.models.User;
import telerik.project.models.dtos.create.PostCreateDTO;
import telerik.project.repositories.PostRepository;
import telerik.project.repositories.UserRepository;
import telerik.project.services.contracts.NotificationService;
import telerik.project.services.contracts.TagService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private TagService tagService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostServiceImpl postService;

    private User author;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setId(1L);
        author.setFollowers(Set.of());
    }

    @Test
    void getById_existing_returnsPost() {
        Post p = new Post();
        p.setId(10L);
        p.setIsDeleted(false);

        when(postRepository.findByIdWithLikes(10L)).thenReturn(Optional.of(p));

        Post result = postService.getById(10L);

        assertEquals(10L, result.getId());
        verify(postRepository).findByIdWithLikes(10L);
    }

    @Test
    void getById_missing_throws() {
        when(postRepository.findByIdWithLikes(5L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> postService.getById(5L));
    }

    @Test
    void create_withTags_resolvesTags_and_sendsNotifications() {
        PostCreateDTO dto = new PostCreateDTO();
        dto.setTitle("T");
        dto.setContent("C");
        dto.setTags(Set.of("Java", "Spring"));

        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));

        Tag tag1 = new Tag(); tag1.setName("java"); tag1.setId(1L);
        Tag tag2 = new Tag(); tag2.setName("spring"); tag2.setId(2L);

        when(tagService.createIfNotExists("java")).thenReturn(tag1);
        when(tagService.createIfNotExists("spring")).thenReturn(tag2);

        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post arg = invocation.getArgument(0);
            arg.setId(99L);
            return arg;
        });

        Post saved = postService.create(dto, author);

        assertNotNull(saved);
        assertEquals(99L, saved.getId());
        assertEquals(author, saved.getAuthor());
        assertEquals(2, saved.getTags().size());

        verify(tagService).createIfNotExists("java");
        verify(tagService).createIfNotExists("spring");
        verify(notificationService, times(0)).send(any(), any(), any(), any(), any());
    }

    @Test
    void create_withNoTags_doesNotCallTagService() {
        PostCreateDTO dto = new PostCreateDTO();
        dto.setTitle("T");
        dto.setContent("C");
        dto.setTags(null);

        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post saved = postService.create(dto, author);

        assertNotNull(saved);
        verify(tagService, never()).createIfNotExists(any());
    }

    @Test
    void toggleLike_addsLike_and_swallowsNotificationException() {
        User acting = new User(); acting.setId(2L);
        User postAuthor = new User(); postAuthor.setId(3L);
        Post post = new Post(); post.setId(50L); post.setAuthor(postAuthor);

        when(postRepository.findByIdWithLikes(50L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("boom")).when(notificationService).send(any(), any(), any(), any(), any());

        Post result = postService.toggleLike(50L, acting);

        assertTrue(result.getLikedByUsers().contains(acting));
        verify(postRepository).save(any(Post.class));
        // notification exception should be swallowed
        verify(notificationService).send(any(), any(), any(), any(), any());
    }

    @Test
    void likePost_whenAlreadyLiked_doesNotSave() {
        User acting = new User(); acting.setId(2L);
        Post post = new Post(); post.setId(60L);
        post.getLikedByUsers().add(acting);

        when(postRepository.findByIdWithLikes(60L)).thenReturn(Optional.of(post));

        postService.likePost(60L, acting);

        verify(postRepository, never()).save(post);
    }

    @Test
    void unlikePost_whenLiked_removesAndSaves() {
        User acting = new User(); acting.setId(2L);
        Post post = new Post(); post.setId(70L);
        post.getLikedByUsers().add(acting);

        when(postRepository.findByIdWithLikes(70L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        postService.unlikePost(70L, acting);

        assertFalse(post.getLikedByUsers().contains(acting));
        verify(postRepository).save(post);
    }

    @Test
    void countByAuthor_delegates() {
        when(postRepository.countByAuthor_Id(1L)).thenReturn(5L);

        long c = postService.countByAuthor(1L);
        assertEquals(5L, c);
        verify(postRepository).countByAuthor_Id(1L);
    }

    @Test
    void update_updatesFields_and_callsTagService() {
        User authorUser = new User(); authorUser.setId(1L);
        Post existing = new Post(); existing.setId(100L); existing.setAuthor(authorUser);
        existing.setTitle("Old"); existing.setContent("OldC");

        when(postRepository.findByIdWithLikes(100L)).thenReturn(Optional.of(existing));
        when(tagService.createIfNotExists("newtag")).thenReturn(new Tag());
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        telerik.project.models.dtos.update.PostUpdateDTO dto = new telerik.project.models.dtos.update.PostUpdateDTO();
        dto.setTitle("New"); dto.setContent("NewC"); dto.setTags(Set.of("NewTag"));

        postService.update(100L, dto, authorUser);

        assertEquals("New", existing.getTitle());
        assertEquals("NewC", existing.getContent());
        verify(tagService).createIfNotExists("newtag");
        verify(postRepository).save(existing);
    }

    @Test
    void delete_setsIsDeleted_and_sendsNotification_ifAdmin() {
        User admin = new User(); admin.setId(2L); admin.setRole(telerik.project.models.Role.ADMIN);
        User postAuthor = new User(); postAuthor.setId(3L);
        Post post = new Post(); post.setId(200L); post.setAuthor(postAuthor); post.setIsDeleted(false);

        when(postRepository.findByIdWithLikes(200L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        postService.delete(200L, admin);

        assertTrue(post.isDeleted());
        verify(postRepository).save(post);
        verify(notificationService).send(eq(admin), eq(postAuthor), eq(200L), eq("POST"), eq("DELETED"));
    }

    @Test
    void getByAuthorId_filtersDeleted() {
        Post p1 = new Post(); p1.setId(1L); p1.setIsDeleted(false);
        Post p2 = new Post(); p2.setId(2L); p2.setIsDeleted(true);
        when(postRepository.findByAuthor_Id(9L)).thenReturn(List.of(p1, p2));

        List<Post> res = postService.getByAuthorId(9L);
        assertEquals(1, res.size());
        assertEquals(1L, res.get(0).getId());
    }

    @Test
    void getMostRecent_limitsAndFilters() {
        Post p1 = new Post(); p1.setId(10L); p1.setIsDeleted(false);
        Post p2 = new Post(); p2.setId(11L); p2.setIsDeleted(true);
        when(postRepository.findAll(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Sort.class))).thenReturn(List.of(p1, p2));

        List<Post> res = postService.getMostRecent();
        assertEquals(1, res.size());
        assertEquals(10L, res.get(0).getId());
    }

    @Test
    void getMostCommented_limitsAndFilters() {
        Post p1 = new Post(); p1.setId(20L); p1.setIsDeleted(false);
        Post p2 = new Post(); p2.setId(21L); p2.setIsDeleted(true);
        when(postRepository.findMostCommented()).thenReturn(List.of(p1, p2));

        List<Post> res = postService.getMostCommented();
        assertEquals(1, res.size());
        assertEquals(20L, res.get(0).getId());
    }

    @Test
    void getByTags_mergesDistinctAndFilters() {
        Post p1 = new Post(); p1.setId(30L); p1.setIsDeleted(false);
        Post p2 = new Post(); p2.setId(31L); p2.setIsDeleted(false);
        when(postRepository.findByTags_Name("a")).thenReturn(List.of(p1, p2));
        when(postRepository.findByTags_Name("b")).thenReturn(List.of(p1));

        List<Post> res = postService.getByTags(List.of("a", "b"));
        assertEquals(2, res.size());
    }

    @Test
    void getLikedPosts_filtersDeleted() {
        Post p1 = new Post(); p1.setId(40L); p1.setIsDeleted(false);
        Post p2 = new Post(); p2.setId(41L); p2.setIsDeleted(true);
        when(postRepository.findByLikedByUsers_Id(5L)).thenReturn(List.of(p1, p2));

        List<Post> res = postService.getLikedPosts(5L);
        assertEquals(1, res.size());
        assertEquals(40L, res.get(0).getId());
    }

    @Test
    void likePost_addsLike_and_saves() {
        User acting = new User(); acting.setId(2L);
        Post post = new Post(); post.setId(80L);

        when(postRepository.findByIdWithLikes(80L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        postService.likePost(80L, acting);

        assertTrue(post.getLikedByUsers().contains(acting));
        verify(postRepository).save(post);
    }

    @Test
    void toggleLike_whenAlreadyLiked_removesLike() {
        User acting = new User(); acting.setId(3L);
        Post post = new Post(); post.setId(81L);
        post.getLikedByUsers().add(acting);

        when(postRepository.findByIdWithLikes(81L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post res = postService.toggleLike(81L, acting);

        assertFalse(res.getLikedByUsers().contains(acting));
        verify(postRepository).save(post);
    }

    @Test
    void toggleLike_noNotification_whenAuthorIsActingUser() {
        User acting = new User(); acting.setId(4L);
        Post post = new Post(); post.setId(82L); post.setAuthor(acting);

        when(postRepository.findByIdWithLikes(82L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        postService.toggleLike(82L, acting);

        verify(notificationService, never()).send(any(), any(), any(), any(), any());
    }

    @Test
    void update_blankTitle_and_nullTags_doNotChangeFields() {
        User authorUser = new User(); authorUser.setId(1L);
        Post existing = new Post(); existing.setId(1000L); existing.setAuthor(authorUser);
        existing.setTitle("KeepMe"); existing.setContent("C");

        when(postRepository.findByIdWithLikes(1000L)).thenReturn(Optional.of(existing));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        telerik.project.models.dtos.update.PostUpdateDTO dto = new telerik.project.models.dtos.update.PostUpdateDTO();
        dto.setTitle("   "); // blank
        dto.setContent(null);
        dto.setTags(null);

        postService.update(1000L, dto, authorUser);

        assertEquals("KeepMe", existing.getTitle());
        verify(postRepository).save(existing);
    }

    @Test
    void create_withFollowers_sendsNotificationsPerFollower() {
        PostCreateDTO dto = new PostCreateDTO();
        dto.setTitle("T"); dto.setContent("C"); dto.setTags(null);

        User follower = new User(); follower.setId(10L);
        author.setFollowers(Set.of(follower));

        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post created = postService.create(dto, author);

        // follower should receive a notification
        verify(notificationService).send(eq(author), eq(follower), any(), eq("POST"), eq("CREATE"));
    }

    @Test
    void toggleLike_whenAdding_sendsNotificationNormally() {
        User acting = new User(); acting.setId(20L);
        User postAuthor = new User(); postAuthor.setId(21L);
        Post post = new Post(); post.setId(300L); post.setAuthor(postAuthor);

        when(postRepository.findByIdWithLikes(300L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        postService.toggleLike(300L, acting);

        assertTrue(post.getLikedByUsers().contains(acting));
        verify(notificationService).send(eq(acting), eq(postAuthor), eq(300L), eq("POST"), eq("LIKED"));
    }

    @Test
    void delete_ownerDoesNotSendAdminNotification() {
        User owner = new User(); owner.setId(40L);
        Post post = new Post(); post.setId(400L); post.setAuthor(owner); post.setIsDeleted(false);

        when(postRepository.findByIdWithLikes(400L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        postService.delete(400L, owner);

        assertTrue(post.isDeleted());
        verify(notificationService, never()).send(any(), any(), any(), any(), any());
    }

    @Test
    void update_contentOnly_changesContent() {
        User authorUser = new User(); authorUser.setId(1L);
        Post existing = new Post(); existing.setId(5000L); existing.setAuthor(authorUser);
        existing.setTitle("Title"); existing.setContent("OldContent");

        when(postRepository.findByIdWithLikes(5000L)).thenReturn(Optional.of(existing));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        telerik.project.models.dtos.update.PostUpdateDTO dto = new telerik.project.models.dtos.update.PostUpdateDTO();
        dto.setTitle(null);
        dto.setContent("UpdatedContent");
        dto.setTags(null);

        postService.update(5000L, dto, authorUser);

        assertEquals("UpdatedContent", existing.getContent());
        verify(postRepository).save(existing);
    }

    @Test
    void likePost_missing_throws() {
        User acting = new User(); acting.setId(2L);
        when(postRepository.findByIdWithLikes(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> postService.likePost(999L, acting));
    }

    @Test
    void unlikePost_missing_throws() {
        User acting = new User(); acting.setId(2L);
        when(postRepository.findByIdWithLikes(998L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> postService.unlikePost(998L, acting));
    }

    @Test
    void toggleLike_missing_throws() {
        User acting = new User(); acting.setId(2L);
        when(postRepository.findByIdWithLikes(997L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> postService.toggleLike(997L, acting));
    }

    @Test
    void create_userNotFound_throws() {
        PostCreateDTO dto = new PostCreateDTO(); dto.setTitle("T"); dto.setContent("C"); dto.setTags(null);
        // simulate missing user in repository
        when(userRepository.findById(author.getId())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> postService.create(dto, author));
    }

    @Test
    void unlikePost_whenNotLiked_doesNotSave() {
        User acting = new User(); acting.setId(2L);
        Post post = new Post(); post.setId(701L);
        // post has no likes by acting
        when(postRepository.findByIdWithLikes(701L)).thenReturn(Optional.of(post));

        postService.unlikePost(701L, acting);

        verify(postRepository, never()).save(post);
    }

}
