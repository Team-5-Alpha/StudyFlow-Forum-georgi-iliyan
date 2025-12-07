package telerik.project.controllers.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import telerik.project.helpers.AuthenticationHelper;
import telerik.project.helpers.mappers.PostMapper;
import telerik.project.helpers.mappers.UserMapper;
import telerik.project.models.User;
import telerik.project.models.dtos.create.UserCreateDTO;
import telerik.project.models.dtos.response.PostResponseDTO;
import telerik.project.models.dtos.response.UserResponseDTO;
import telerik.project.models.dtos.response.UserSummaryDTO;
import telerik.project.models.dtos.update.UserUpdateDTO;
import telerik.project.models.filters.UserFilterOptions;
import telerik.project.services.contracts.UserService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/users")
@Tag(name = "Users", description = "API for managing users and user profiles")
public class UserRestController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final PostMapper postMapper;

    @Operation(summary = "Search users", description = "Search and filter users with pagination")
    @GetMapping
    public List<UserResponseDTO> search(
            @Parameter(description = "Filter by username") @RequestParam(required = false) String username,
            @Parameter(description = "Filter by first name") @RequestParam(required = false) String firstName,
            @Parameter(description = "Filter by last name") @RequestParam(required = false) String lastName,
            @Parameter(description = "Filter by email") @RequestParam(required = false) String email,
            @Parameter(description = "Sort field") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort order") @RequestParam(required = false) String sortOrder,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Integer size
    ) {
        UserFilterOptions filterOptions = new UserFilterOptions(
                username, firstName, lastName, email,
                false, sortBy, sortOrder, page, size);

        return userService.getAll(filterOptions).stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/{id}")
    public UserResponseDTO getById(@Parameter(description = "User ID") @PathVariable Long id) {
        return userMapper.toResponse(userService.getById(id));
    }

    @Operation(summary = "Get user followers")
    @GetMapping("/{id}/followers")
    public List<UserSummaryDTO> getFollowers(@Parameter(description = "User ID") @PathVariable Long id) {
        return userService.getFollowers(id).stream()
                .map(userMapper::toSummary)
                .toList();
    }

    @Operation(summary = "Get user following")
    @GetMapping("/{id}/following")
    public List<UserSummaryDTO> getFollowing(@Parameter(description = "User ID") @PathVariable Long id) {
        return userService.getFollowing(id).stream()
                .map(userMapper::toSummary)
                .toList();
    }

    @Operation(summary = "Get user posts")
    @GetMapping("/{id}/posts")
    public List<PostResponseDTO> getPosts(@Parameter(description = "User ID") @PathVariable Long id) {
        return userService.getPostsByUser(id).stream()
                .map(postMapper::toResponse)
                .toList();
    }

    @Operation(summary = "Register new user", description = "Public registration endpoint")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDTO create(@Valid @RequestBody UserCreateDTO dto) {
        // ТОВА Е ПУБЛИЧНО (РЕГИСТРАЦИЯ) - НЕ ИСКАМЕ ТОКЕН
        User user = userMapper.fromCreateDTO(dto);
        userService.create(user);
        return userMapper.toResponse(user);
    }

    @Operation(summary = "Update user profile", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    public UserResponseDTO update(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Valid @RequestBody UserUpdateDTO dto
    ) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        User target = userService.getById(id);

        userMapper.updateUser(target, dto);
        userService.update(id, target, actingUser);

        return userMapper.toResponse(userService.getById(id));
    }

    @Operation(summary = "Delete user", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "User ID") @PathVariable Long id) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        userService.delete(id, actingUser);
    }

    @Operation(summary = "Follow user", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(@Parameter(description = "User ID") @PathVariable Long id) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        userService.followUser(id, actingUser);
    }

    @Operation(summary = "Unfollow user", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@Parameter(description = "User ID") @PathVariable Long id) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        userService.unfollowUser(id, actingUser);
    }
}