package telerik.project.controllers.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import telerik.project.helpers.AuthenticationHelper;
import telerik.project.helpers.mappers.TagMapper;
import telerik.project.models.Tag;
import telerik.project.models.User;
import telerik.project.models.dtos.response.TagResponseDTO;
import telerik.project.models.dtos.update.TagUpdateDTO;
import telerik.project.services.contracts.TagService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tags", description = "API for managing post tags")
public class TagRestController {

    private final TagService tagService;
    private final TagMapper tagMapper;

    @Operation(summary = "Get all tags", description = "Retrieve all available tags")
    @GetMapping
    public List<TagResponseDTO> getAllTags() {
        return tagService.getAll().stream()
                .map(tagMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Get tag by ID")
    @GetMapping("/{id}")
    public TagResponseDTO getById(@Parameter(description = "Tag ID") @PathVariable Long id) {
        return tagMapper.toResponse(tagService.getById(id));
    }

    @Operation(summary = "Get tag by name")
    @GetMapping("/name/{name}")
    public TagResponseDTO getByName(@Parameter(description = "Tag name") @PathVariable String name) {
        Tag tag = tagService.getByName(name);
        return tagMapper.toResponse(tag);
    }

    @Operation(summary = "Create tag", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TagResponseDTO create(@Parameter(description = "Tag name") @RequestParam String name) {
        // Дори да не ползваме юзъра в сървиса, изискваме автентикация за сигурност
        User actingUser = AuthenticationHelper.getLoggedUser();

        Tag created = tagService.createIfNotExists(name);
        return tagMapper.toResponse(created);
    }

    @Operation(summary = "Update tag", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    public TagResponseDTO update(
            @Parameter(description = "Tag ID") @PathVariable Long id,
            @Valid @RequestBody TagUpdateDTO dto
    ) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        Tag updated = tagService.getById(id);

        tagMapper.updateTag(updated, dto);
        tagService.update(id, updated, actingUser);

        return tagMapper.toResponse(tagService.getById(id));
    }

    @Operation(summary = "Delete tag", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Tag ID") @PathVariable Long id) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        tagService.delete(id, actingUser);
    }
}