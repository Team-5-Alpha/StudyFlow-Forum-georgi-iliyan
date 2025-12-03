package telerik.project.models.dtos.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class PostResponseDTO {
    private Long id;

    private String title;
    private String content;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private UserSummaryDTO author;

    @JsonProperty("isLiked")
    private boolean isLiked;

    private int likesCount;

    private Set<String> tags;
}
