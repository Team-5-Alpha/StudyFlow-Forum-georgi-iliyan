package telerik.project.controllers.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import telerik.project.helpers.AuthenticationHelper;
import telerik.project.helpers.mappers.CommentMapper;
import telerik.project.helpers.mappers.PostMapper;
import telerik.project.models.Comment;
import telerik.project.models.Post;
import telerik.project.models.User;
import telerik.project.models.dtos.create.CommentCreateDTO;
import telerik.project.models.dtos.create.PostCreateDTO;
import telerik.project.models.dtos.response.CommentResponseDTO;
import telerik.project.models.dtos.response.PostResponseDTO;
import telerik.project.models.dtos.update.PostUpdateDTO;
import telerik.project.models.filters.CommentFilterOptions;
import telerik.project.models.filters.PostFilterOptions;
import telerik.project.services.contracts.CommentService;
import telerik.project.services.contracts.PostService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostRestController {

    private final PostService postService;
    private final CommentService commentService;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    @GetMapping
    public List<PostResponseDTO> getAll(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String tagName,
            @RequestParam(required = false) Boolean isDeleted,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        // Взимаме текущия потребител (или null)
        User currentUser = AuthenticationHelper.tryGetLoggedUser();

        PostFilterOptions filterOptions = new PostFilterOptions(
                title, keyword, authorId, tagName, isDeleted,
                sortBy, sortOrder, page, size);

        return postService.getAll(filterOptions).stream()
                // Подаваме юзъра на мапъра
                .map(post -> postMapper.toResponse(post, currentUser))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public PostResponseDTO getById(@PathVariable Long id) {
        User currentUser = AuthenticationHelper.tryGetLoggedUser();
        Post post = postService.getById(id);
        return postMapper.toResponse(post, currentUser);
    }

    @GetMapping("/latest")
    public List<PostResponseDTO> getLatest(@RequestParam(defaultValue = "10") int limit) {
        User currentUser = AuthenticationHelper.tryGetLoggedUser();
        return postService.getMostRecent().stream()
                .limit(limit)
                .map(post -> postMapper.toResponse(post, currentUser))
                .collect(Collectors.toList());
    }

    @GetMapping("/top-commented")
    public List<PostResponseDTO> getTopCommented(@RequestParam(defaultValue = "10") int limit) {
        User currentUser = AuthenticationHelper.tryGetLoggedUser();
        return postService.getMostCommented().stream()
                .limit(limit)
                .map(post -> postMapper.toResponse(post, currentUser))
                .collect(Collectors.toList());
    }

    // ... Останалите методи са същите ...

    @GetMapping("/{postId}/comments")
    public List<CommentResponseDTO> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        CommentFilterOptions filterOptions = new CommentFilterOptions(postId, null, null, null, null, null, page, size);
        return commentService.getAll(filterOptions).stream()
                .map(commentMapper::toResponse) // Тук можеш да направиш същото за коментарите, ако искаш isLiked и там
                .collect(Collectors.toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponseDTO create(@Valid @RequestBody PostCreateDTO dto) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        Post post = postService.create(dto, actingUser);
        // При създаване е ясно, че авторът не го е лайкнал още
        return postMapper.toResponse(post, actingUser);
    }

    @PutMapping("/{id}")
    public PostResponseDTO update(
            @PathVariable Long id,
            @Valid @RequestBody PostUpdateDTO dto
    ) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        postService.update(id, dto, actingUser);
        return postMapper.toResponse(postService.getById(id), actingUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        postService.delete(id, actingUser);
    }

    @PostMapping("/{id}/likes")
    public PostResponseDTO like(@PathVariable Long id) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        postService.likePost(id, actingUser);
        Post updatedPost = postService.getById(id);
        return postMapper.toResponse(updatedPost, actingUser);
    }

    @DeleteMapping("/{id}/likes")
    public PostResponseDTO unlike(@PathVariable Long id) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        postService.unlikePost(id, actingUser);
        Post updatedPost = postService.getById(id);
        return postMapper.toResponse(updatedPost, actingUser);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponseDTO comment(
            @PathVariable Long id,
            @Valid @RequestBody CommentCreateDTO dto
    ) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        Comment comment = new Comment();
        comment.setContent(dto.getContent());
        commentService.create(comment, id, actingUser);
        return commentMapper.toResponse(comment);
    }
}