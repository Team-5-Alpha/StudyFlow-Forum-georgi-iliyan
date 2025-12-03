package telerik.project.controllers.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import telerik.project.helpers.AuthenticationHelper; // <--- НОВО
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
public class TagRestController {

    private final TagService tagService;
    private final TagMapper tagMapper;

    @GetMapping
    public List<TagResponseDTO> getAllTags() {
        return tagService.getAll().stream()
                .map(tagMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public TagResponseDTO getTagById(@PathVariable Long id) {
        return tagMapper.toResponse(tagService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TagResponseDTO createTag(@Valid @RequestBody Tag tag) {
        // Дори да не ползваме юзъра в сървиса, изискваме автентикация за сигурност
        User actingUser = AuthenticationHelper.getLoggedUser();

        Tag created = tagService.createIfNotExists(tag.getName());
        return tagMapper.toResponse(created);
    }

    @PutMapping("/{id}")
    public TagResponseDTO update(
            @PathVariable Long id,
            @Valid @RequestBody TagUpdateDTO dto
    ) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        Tag updated = tagService.getById(id);

        tagMapper.updateTag(updated, dto);
        tagService.update(id, updated, actingUser);

        return tagMapper.toResponse(tagService.getById(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTag(@PathVariable Long id) {
        User actingUser = AuthenticationHelper.getLoggedUser();
        tagService.delete(id, actingUser);
    }
}