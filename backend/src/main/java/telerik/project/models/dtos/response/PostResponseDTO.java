package telerik.project.models.dtos.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Post response data transfer object")
public class PostResponseDTO {
    @Schema(description = "Post ID", example = "1")
    private Long id;

    @Schema(description = "Post title", example = "How to use Spring Boot?")
    private String title;

    @Schema(description = "Post content", example = "I'm learning Spring Boot and need help...")
    private String content;

    @Schema(description = "Post creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Post last update timestamp")
    private LocalDateTime updatedAt;

    @Schema(description = "Post author information")
    private UserSummaryDTO author;

    @Schema(description = "Whether the current user has liked this post")
    @JsonProperty("likedByCurrentUser")
    private boolean liked;

    @Schema(description = "Number of likes on this post", example = "42")
    private int likesCount;

    @Schema(description = "Set of tag names associated with this post", example = "[\"java\", \"spring-boot\", \"backend\"]")
    private Set<String> tags;
}
