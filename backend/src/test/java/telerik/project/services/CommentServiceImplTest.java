package telerik.project.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import telerik.project.exceptions.EntityNotFoundException;
import telerik.project.models.Comment;
import telerik.project.models.Post;
import telerik.project.models.User;
import telerik.project.repositories.CommentRepository;
import telerik.project.repositories.UserRepository;
import telerik.project.services.contracts.NotificationService;
import telerik.project.services.contracts.PostService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostService postService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User author;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setId(1L);
        author.setFollowers(Set.of());
    }

    @Test
    void getById_existing_returns() {
        Comment c = new Comment(); c.setId(11L); c.setIsDeleted(false);
        when(commentRepository.findById(11L)).thenReturn(Optional.of(c));

        Comment r = commentService.getById(11L);

        assertEquals(11L, r.getId());
    }

    @Test
    void getById_missing_throws() {
        when(commentRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> commentService.getById(2L));
    }

    @Test
    void create_reply_sendsTwoNotifications_and_saves() {
        Post post = new Post(); post.setId(20L);
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(postService.getById(20L)).thenReturn(post);

        Comment parent = new Comment(); parent.setId(30L);
        parent.setAuthor(new User()); parent.getAuthor().setId(5L);

        parent.setPost(post);
        when(commentRepository.findById(30L)).thenReturn(Optional.of(parent));

        Comment toCreate = new Comment();
        Comment parentRef = new Comment(); parentRef.setId(30L);
        toCreate.setParentComment(parentRef);

        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        commentService.create(toCreate, 20L, author);

        verify(notificationService).send(eq(author), eq(parent.getAuthor()), eq(parent.getId()), eq("COMMENT"), eq("REPLY"));
        verify(notificationService).send(eq(author), eq(post.getAuthor()), eq(post.getId()), eq("COMMENT"), eq("CREATE"));
    }

    @Test
    void likeComment_addsLike_and_saves() {
        User acting = new User(); acting.setId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(acting));

        Comment comment = new Comment(); comment.setId(40L);
        when(commentRepository.findById(40L)).thenReturn(Optional.of(comment));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        commentService.likeComment(40L, acting);

        assertTrue(comment.getLikedByUsers().contains(acting));
        verify(userRepository).save(acting);
        verify(commentRepository).save(comment);
        verify(notificationService).send(any(), any(), any(), any(), any());
    }

    @Test
    void unlikeComment_notLiked_doesNotSave() {
        User acting = new User(); acting.setId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(acting));

        Comment comment = new Comment(); comment.setId(41L);
        when(commentRepository.findById(41L)).thenReturn(Optional.of(comment));

        assertThrows(telerik.project.exceptions.InvalidOperationException.class,
                () -> commentService.unlikeComment(41L, acting));
    }

    @Test
    void getReplies_filtersDeleted() {
        Comment r1 = new Comment(); r1.setId(1L); r1.setIsDeleted(false);
        Comment r2 = new Comment(); r2.setId(2L); r2.setIsDeleted(true);
        when(commentRepository.findByParentCommentId(100L)).thenReturn(List.of(r1, r2));

        List<Comment> res = commentService.getReplies(100L);
        assertEquals(1, res.size());
        assertEquals(1L, res.get(0).getId());
    }

    @Test
    void update_existing_changesContent_and_saves() {
        Comment existing = new Comment(); existing.setId(100L); existing.setContent("old"); existing.setIsDeleted(false);
        User owner = new User(); owner.setId(1L);
        existing.setAuthor(owner);

        Post post = new Post(); post.setId(500L); post.setIsDeleted(false);
        existing.setPost(post);

        when(commentRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment updated = new Comment(); updated.setContent("new");

        commentService.update(100L, updated, owner);

        assertEquals("new", existing.getContent());
        verify(commentRepository).save(existing);
    }

    @Test
    void delete_admin_setsDeleted_and_sendsNotification() {
        User admin = new User(); admin.setId(50L); admin.setRole(telerik.project.models.Role.ADMIN);
        Comment comment = new Comment(); comment.setId(101L); comment.setIsDeleted(false);
        User authorUser = new User(); authorUser.setId(51L);
        comment.setAuthor(authorUser);

        when(commentRepository.findById(101L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        commentService.delete(101L, admin);

        assertTrue(comment.getIsDeleted());
        verify(commentRepository).save(comment);
        verify(notificationService).send(eq(admin), eq(authorUser), eq(101L), eq("COMMENT"), eq("DELETED"));
    }

    @Test
    void unlikeComment_whenLiked_removesLike_and_saves() {
        User acting = new User(); acting.setId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(acting));

        Comment comment = new Comment(); comment.setId(201L);
        when(commentRepository.findById(201L)).thenReturn(Optional.of(comment));

        acting.getLikedComments().add(comment);
        comment.getLikedByUsers().add(acting);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        commentService.unlikeComment(201L, acting);

        assertFalse(comment.getLikedByUsers().contains(acting));
        verify(userRepository).save(acting);
        verify(commentRepository).save(comment);
    }

    @Test
    void likeComment_whenAlreadyLiked_throwsInvalidOperation() {
        User acting = new User(); acting.setId(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(acting));

        Comment comment = new Comment(); comment.setId(301L);
        when(commentRepository.findById(301L)).thenReturn(Optional.of(comment));

        acting.getLikedComments().add(comment);
        comment.getLikedByUsers().add(acting);

        assertThrows(telerik.project.exceptions.InvalidOperationException.class,
                () -> commentService.likeComment(301L, acting));
    }

}
