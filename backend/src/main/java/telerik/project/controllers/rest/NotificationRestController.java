package telerik.project.controllers.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import telerik.project.helpers.AuthenticationHelper;
import telerik.project.helpers.mappers.NotificationMapper;
import telerik.project.models.User;
import telerik.project.models.dtos.response.NotificationResponseDTO;
import telerik.project.models.filters.NotificationFilterOptions;
import telerik.project.services.contracts.NotificationService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "API for managing user notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationRestController {

    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    @Operation(summary = "Get user notifications", description = "Retrieve notifications for the authenticated user")
    @GetMapping
    public List<NotificationResponseDTO> getAll(
            @Parameter(description = "Filter by actor ID") @RequestParam(required = false) Long actorId,
            @Parameter(description = "Filter by read status") @RequestParam(required = false) Boolean isRead,
            @Parameter(description = "Filter by entity type") @RequestParam(required = false) String entityType,
            @Parameter(description = "Filter by action type") @RequestParam(required = false) String actionType,
            @Parameter(description = "Filter by created after date") @RequestParam(required = false) LocalDateTime createdAfter,
            @Parameter(description = "Filter by created before date") @RequestParam(required = false) LocalDateTime createdBefore,
            @Parameter(description = "Sort field") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort order") @RequestParam(required = false) String sortOrder,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") Integer size
    ) {
        // 1. Взимаме кой е логнат (Това е ПОЛУЧАТЕЛЯТ на известията)
        User receiver = AuthenticationHelper.getLoggedUser();

        NotificationFilterOptions filterOptions = new NotificationFilterOptions(
                actorId, isRead, entityType, actionType, createdAfter, createdBefore,
                sortBy, sortOrder, page, size
        );

        // 2. Викаме сървиза, подавайки ID-то на получателя
        return notificationService.getAll(receiver.getId(), filterOptions).stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Operation(summary = "Get notification by ID", description = "Retrieve a specific notification by its ID")
    @GetMapping("/{id}")
    public NotificationResponseDTO getById(@Parameter(description = "Notification ID") @PathVariable Long id) {
        User receiver = AuthenticationHelper.getLoggedUser();
        return notificationMapper.toResponse(notificationService.getById(receiver.getId(), id));
    }


    @Operation(summary = "Mark notification as read", description = "Mark a specific notification as read")
    @PutMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@Parameter(description = "Notification ID") @PathVariable Long id) {
        User receiver = AuthenticationHelper.getLoggedUser();
        notificationService.markAsRead(receiver.getId(), id);
    }

    @Operation(summary = "Mark all notifications as read", description = "Mark all notifications as read for the authenticated user")
    @PutMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead() {
        User receiver = AuthenticationHelper.getLoggedUser();
        notificationService.markAllAsRead(receiver.getId());
    }

    @Operation(summary = "Delete notification", description = "Delete a specific notification by its ID")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Notification ID") @PathVariable Long id) {
        User receiver = AuthenticationHelper.getLoggedUser();
        notificationService.delete(receiver.getId(), id);
    }
}