package telerik.project.models.dtos.response;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime; // Import LocalDateTime

@Getter
@Setter
public class UserResponseDTO {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String profilePhotoURL;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
