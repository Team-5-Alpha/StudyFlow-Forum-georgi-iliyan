package telerik.project.controllers.rest;

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
public class NotificationRestController {

    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    @GetMapping
    public List<NotificationResponseDTO> getAll(
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) LocalDateTime createdAfter,
            @RequestParam(required = false) LocalDateTime createdBefore,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
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

    @GetMapping("/{id}")
    public NotificationResponseDTO getById(@PathVariable Long id) {
        User receiver = AuthenticationHelper.getLoggedUser();
        return notificationMapper.toResponse(notificationService.getById(receiver.getId(), id));
    }

    @PutMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable Long id) {
        User receiver = AuthenticationHelper.getLoggedUser();
        notificationService.markAsRead(receiver.getId(), id);
    }

    @PutMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead() {
        User receiver = AuthenticationHelper.getLoggedUser();
        notificationService.markAllAsRead(receiver.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        User receiver = AuthenticationHelper.getLoggedUser();
        notificationService.delete(receiver.getId(), id);
    }
}