package telerik.project.controllers.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import telerik.project.helpers.AuthenticationHelper; // <--- НОВО
import telerik.project.helpers.mappers.CommentMapper;
import telerik.project.models.Comment;
import telerik.project.models.User;
import telerik.project.models.dtos.create.CommentCreateDTO;
import telerik.project.models.dtos.response.CommentResponseDTO;
import telerik.project.models.dtos.update.CommentUpdateDTO;
import telerik.project.models.filters.CommentFilterOptions;
import telerik.project.services.contracts.CommentService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "API for managing comments on posts")
public class CommentRestController {

    private final CommentService commentService;
    private final CommentMapper commentMapper;

    @Operation(summary = "Get all comments", description = "Retrieve comments with optional filtering")
    @GetMapping
    public List<CommentResponseDTO> getAll(
            @Parameter(description = "Filter by post ID") @RequestParam(required = false) Long postId,
            @Parameter(description = "Filter by author ID") @RequestParam(required = false) Long authorId,
            @Parameter(description = "Filter by parent comment ID") @RequestParam(required = false) Long parentCommentId,
            @Parameter(description = "Filter by deletion status") @RequestParam(required = false) Boolean isDeleted,
            @Parameter(description = "Sort field") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort order") @RequestParam(required = false) String sortOrder,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Integer size
    ) {
        CommentFilterOptions filterOptions = new CommentFilterOptions(
                postId, authorId, parentCommentId,
                isDeleted, sortBy, sortOrder, page, size);

        return commentService.getAll(filterOptions).stream()
                .map(commentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Get comment by ID")
    @GetMapping("/{id}")
    public CommentResponseDTO getById(@Parameter(description = "Comment ID") @PathVariable Long id) {
        return commentMapper.toResponse(commentService.getById(id));
    }

    @Operation(summary = "Get comment replies")
    @GetMapping("/{id}/replies")
    public List<CommentResponseDTO> getReplies(@Parameter(description = "Comment ID") @PathVariable Long id) {
        return commentService.getReplies(id).stream()
                .map(commentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Create comment reply", description = "Create a reply to an existing comment",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponseDTO create(
            @Parameter(description = "Parent comment ID") @PathVariable Long id,
            @Valid @RequestBody CommentCreateDTO dto
    ) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        Comment parent = commentService.getById(id);

        Comment reply = new Comment();
        reply.setContent(dto.getContent());
        reply.setParentComment(parent);

        commentService.create(reply, parent.getPost().getId(), actingUser);

        return commentMapper.toResponse(reply);
    }

    @Operation(summary = "Update comment", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    public CommentResponseDTO update(
            @Parameter(description = "Comment ID") @PathVariable Long id,
            @Valid @RequestBody CommentUpdateDTO dto
    ) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        Comment target = commentService.getById(id);

        commentMapper.updateComment(target, dto);
        commentService.update(id, target, actingUser);

        return commentMapper.toResponse(commentService.getById(id));
    }

    @Operation(summary = "Delete comment", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Comment ID") @PathVariable Long id) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        commentService.delete(id, actingUser);
    }

    @Operation(summary = "Like comment", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/likes")
    public void like(@Parameter(description = "Comment ID") @PathVariable Long id) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        commentService.likeComment(id, actingUser);
    }

    @Operation(summary = "Unlike comment", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}/likes")
    public void unlike(@Parameter(description = "Comment ID") @PathVariable Long id) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        commentService.unlikeComment(id, actingUser);
    }
}