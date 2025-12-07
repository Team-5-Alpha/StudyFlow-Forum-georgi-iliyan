package telerik.project.helpers;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import telerik.project.models.User;

import java.util.Optional;

public class AuthenticationHelper {

    public static User getLoggedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof User) {
            return (User) auth.getPrincipal();
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
    }

    public static User tryGetLoggedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof User) {
            return (User) auth.getPrincipal();
        }

        return null;
    }

    public static Optional<User> getCurrentUserSafely() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null ||
                    !authentication.isAuthenticated() ||
                    authentication instanceof AnonymousAuthenticationToken) {
                return Optional.empty();
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                return Optional.of((User) principal);
            }

            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}