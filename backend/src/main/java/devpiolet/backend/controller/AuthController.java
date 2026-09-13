package devpiolet.backend.controller;

import devpiolet.backend.dto.UserResponse;
import devpiolet.backend.entity.User;
import devpiolet.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CurrentUser currentUser;

    /**
     * Returns the GitHub OAuth2 authorization endpoint URL for frontend redirection.
     */
    @GetMapping("/login-url")
    public ResponseEntity<Map<String, String>> getLoginUrl() {
        return ResponseEntity.ok(Map.of("url", "/oauth2/authorization/github"));
    }

    /**
     * Returns profile details for the currently authenticated session user.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        User user = currentUser.require();

        UserResponse response = new UserResponse(
                user.getId(),
                user.getGhId(),
                user.getGithubUsername(),
                user.getDisplayName(),
                user.getAvatarUrl()
        );

        return ResponseEntity.ok(response);
    }
}