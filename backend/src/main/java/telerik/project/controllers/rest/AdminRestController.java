package telerik.project.controllers.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import telerik.project.helpers.AuthenticationHelper;
import telerik.project.helpers.AuthorizationHelper;
import telerik.project.helpers.mappers.UserMapper;
import telerik.project.models.User;
import telerik.project.models.dtos.response.AdminResponseDTO;
import telerik.project.models.dtos.response.AdminUserResponseDTO;
import telerik.project.models.dtos.update.AdminUpdateDTO;
import telerik.project.models.filters.UserFilterOptions;
import telerik.project.services.contracts.UserService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/admin/users")
@Tag(name = "Admin", description = "Admin API for managing users (requires admin role)")
@SecurityRequirement(name = "bearerAuth")
public class AdminRestController {

    private final UserService userService;
    private final UserMapper userMapper;

    @Operation(summary = "Search users (Admin)", description = "Admin endpoint to search users with detailed info")
    @GetMapping
    public List<AdminUserResponseDTO> search(
            @Parameter(description = "Filter by username") @RequestParam(required = false) String username,
            @Parameter(description = "Filter by first name") @RequestParam(required = false) String firstName,
            @Parameter(description = "Filter by last name") @RequestParam(required = false) String lastName,
            @Parameter(description = "Filter by email") @RequestParam(required = false) String email,
            @Parameter(description = "Filter by blocked status") @RequestParam(required = false) Boolean isBlocked,
            @Parameter(description = "Sort field") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort order") @RequestParam(required = false) String sortOrder,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Integer size
    ) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        AuthorizationHelper.validateAdmin(actingUser);

        UserFilterOptions filterOptions = new UserFilterOptions(
                username, firstName, lastName, email,
                isBlocked, sortBy, sortOrder, page, size
        );

        return userService.getAll(filterOptions).stream()
                .map(userMapper::toAdminUserResponse)
                .toList();
    }

    @Operation(summary = "Get user details (Admin)")
    @GetMapping("/{id}")
    public AdminResponseDTO getById(@Parameter(description = "User ID") @PathVariable Long id) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        AuthorizationHelper.validateAdmin(actingUser);

        return userMapper.toAdminResponse(userService.getById(id));
    }

    @Operation(summary = "Update user (Admin)")
    @PutMapping("/{id}")
    public AdminResponseDTO update(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Valid @RequestBody AdminUpdateDTO dto
    ) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        AuthorizationHelper.validateAdmin(actingUser);

        User target = userService.getById(id);
        userMapper.updateAdmin(target, dto);
        userService.update(id, target, actingUser);

        return userMapper.toAdminResponse(userService.getById(id));
    }

    @Operation(summary = "Block user (Admin)")
    @PostMapping("/{id}/block")
    public void blockUser(@Parameter(description = "User ID") @PathVariable Long id) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        AuthorizationHelper.validateAdmin(actingUser);

        userService.blockUser(id, actingUser);
    }

    @Operation(summary = "Unblock user (Admin)")
    @DeleteMapping("/{id}/block")
    public void unblockUser(@Parameter(description = "User ID") @PathVariable Long id) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        AuthorizationHelper.validateAdmin(actingUser);

        userService.unblockUser(id, actingUser);
    }

    @Operation(summary = "Promote user to admin (Admin)")
    @PostMapping("/{id}/promote")
    public void promoteToAdmin(@Parameter(description = "User ID") @PathVariable Long id) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        AuthorizationHelper.validateAdmin(actingUser);

        userService.promoteToAdmin(id, actingUser);
    }
}