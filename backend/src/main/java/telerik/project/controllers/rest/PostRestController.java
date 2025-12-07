package telerik.project.controllers.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "API for managing forum posts")
public class PostRestController {

    private final PostService postService;
    private final CommentService commentService;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    @Operation(summary = "Get all posts", description = "Retrieve all posts with optional filtering and pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved posts"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    @GetMapping
    public List<PostResponseDTO> getAll(
            @Parameter(description = "Filter by post title") @RequestParam(required = false) String title,
            @Parameter(description = "Filter by keyword in content") @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter by author ID") @RequestParam(required = false) Long authorId,
            @Parameter(description = "Filter by tag name") @RequestParam(required = false) String tagName,
            @Parameter(description = "Filter by deletion status") @RequestParam(required = false) Boolean isDeleted,
            @Parameter(description = "Sort field (e.g., createdAt, likesCount)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort order (asc/desc)") @RequestParam(required = false) String sortOrder,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Integer size
    ) {
        User currentUser = AuthenticationHelper.tryGetLoggedUser();

        PostFilterOptions filterOptions = new PostFilterOptions(
                title, keyword, authorId, tagName, isDeleted,
                sortBy, sortOrder, page, size);

        return postService.getAll(filterOptions).stream()
                .map(post -> postMapper.toResponse(post, currentUser))
                .collect(Collectors.toList());
    }

    @Operation(summary = "Get post by ID", description = "Retrieve a specific post by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Post found"),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    @GetMapping("/{id}")
    public PostResponseDTO getById(@Parameter(description = "Post ID") @PathVariable Long id) {
        User currentUser = AuthenticationHelper.tryGetLoggedUser();
        Post post = postService.getById(id);
        return postMapper.toResponse(post, currentUser);
    }

    @Operation(summary = "Get latest posts", description = "Retrieve the most recent posts")
    @GetMapping("/latest")
    public List<PostResponseDTO> getLatest(
            @Parameter(description = "Maximum number of posts to return") @RequestParam(defaultValue = "10") int limit) {
        User currentUser = AuthenticationHelper.tryGetLoggedUser();
        return postService.getMostRecent().stream()
                .limit(limit)
                .map(post -> postMapper.toResponse(post, currentUser))
                .collect(Collectors.toList());
    }

    @Operation(summary = "Get top commented posts", description = "Retrieve posts with most comments")
    @GetMapping("/top-commented")
    public List<PostResponseDTO> getTopCommented(
            @Parameter(description = "Maximum number of posts to return") @RequestParam(defaultValue = "10") int limit) {
        User currentUser = AuthenticationHelper.tryGetLoggedUser();
        return postService.getMostCommented().stream()
                .limit(limit)
                .map(post -> postMapper.toResponse(post, currentUser))
                .collect(Collectors.toList());
    }

    @Operation(summary = "Get post comments", description = "Retrieve all comments for a specific post")
    @GetMapping("/{postId}/comments")
    public List<CommentResponseDTO> getComments(
            @Parameter(description = "Post ID") @PathVariable Long postId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Integer size) {
        CommentFilterOptions filterOptions = new CommentFilterOptions(postId, null, null, null, null, null, page, size);
        return commentService.getAll(filterOptions).stream()
                .map(commentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Create a new post", description = "Create a new forum post (requires authentication)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Post created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponseDTO create(@Valid @RequestBody PostCreateDTO dto) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        Post post = postService.create(dto, actingUser);
        return postMapper.toResponse(post, actingUser);
    }

    @Operation(summary = "Update a post", description = "Update an existing post (requires authentication and ownership)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Post updated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the owner"),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    @PutMapping("/{id}")
    public PostResponseDTO update(
            @Parameter(description = "Post ID") @PathVariable Long id,
            @Valid @RequestBody PostUpdateDTO dto
    ) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        postService.update(id, dto, actingUser);
        return postMapper.toResponse(postService.getById(id), actingUser);
    }

    @Operation(summary = "Delete a post", description = "Delete a post (requires authentication and ownership or admin role)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Post deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Post ID") @PathVariable Long id) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        postService.delete(id, actingUser);
    }

    @Operation(summary = "Like a post", description = "Add a like to a post (requires authentication)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/likes")
    public PostResponseDTO like(@Parameter(description = "Post ID") @PathVariable Long id) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        postService.likePost(id, actingUser);
        Post updatedPost = postService.getById(id);
        return postMapper.toResponse(updatedPost, actingUser);
    }

    @Operation(summary = "Unlike a post", description = "Remove a like from a post (requires authentication)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}/likes")
    public PostResponseDTO unlike(@Parameter(description = "Post ID") @PathVariable Long id) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        postService.unlikePost(id, actingUser);
        Post updatedPost = postService.getById(id);
        return postMapper.toResponse(updatedPost, actingUser);
    }

    @Operation(summary = "Toggle like on a post", description = "Toggle like status on a post (requires authentication)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(@Parameter(description = "Post ID") @PathVariable Long id) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        Post post = postService.toggleLike(id, actingUser);

        boolean likedByCurrentUser = post.getLikedByUsers().stream()
                .anyMatch(user -> user.getId().equals(actingUser.getId()));

        Map<String, Object> response = new HashMap<>();
        response.put("likedByCurrentUser", likedByCurrentUser);
        response.put("likesCount", post.getLikedByUsers().size());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Add comment to post", description = "Create a new comment on a post (requires authentication)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Comment created successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponseDTO comment(
            @Parameter(description = "Post ID") @PathVariable Long id,
            @Valid @RequestBody CommentCreateDTO dto
    ) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        Comment comment = new Comment();
        comment.setContent(dto.getContent());
        commentService.create(comment, id, actingUser);
        return commentMapper.toResponse(comment);
    }
}
