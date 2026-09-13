package devpiolet.backend.security;

import devpiolet.backend.entity.User;
import devpiolet.backend.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    /**
     * Extracts and returns the authenticated User entity from the Spring Security context.
     * Throws UnauthorizedException if the user is not authenticated or anonymous.
     */
    public User require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null 
                || !authentication.isAuthenticated() 
                || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new UnauthorizedException("User is not authenticated");
        }

        return principal.getUser();
    }
}