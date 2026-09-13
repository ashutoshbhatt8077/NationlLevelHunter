package devpiolet.backend.service;

import devpiolet.backend.entity.User;
import devpiolet.backend.exception.NotFoundException;
import devpiolet.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TextEncryptor tokenEncryptor;

    /**
     * Retrieves a user by their database UUID or throws NotFoundException.
     */
    @Transactional(readOnly = true)
    public User requireById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }

    /**
     * Decrypts the stored GitHub access token for API interactions.
     */
    public String decryptAccessToken(User user) {
        if (user.getAccessToken() == null) {
            return null;
        }
        return tokenEncryptor.decrypt(user.getAccessToken());
    }

    /**
     * Upserts user record upon GitHub OAuth2 authentication callback.
     */
    @Transactional
    public User upsertFromGithub(Map<String, Object> attributes, String accessToken, String scopes) {
        Long ghId = toLong(attributes.get("id"));
        String login = (String) attributes.get("login");
        String name = (String) attributes.get("name");
        String displayName = (name != null && !name.isBlank()) ? name : login;
        String avatarUrl = (String) attributes.get("avatar_url");
        String encryptedToken = tokenEncryptor.encrypt(accessToken);

        User user = userRepository.findByGhId(ghId)
                .orElseGet(() -> User.builder().ghId(ghId).build());

        user.setGithubUsername(login);
        user.setDisplayName(displayName);
        user.setAvatarUrl(avatarUrl);
        user.setAccessToken(encryptedToken);
        user.setTokenScopes(scopes);

        return userRepository.save(user);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str) {
            return Long.parseLong(str);
        }
        throw new IllegalArgumentException("Cannot convert " + value + " to Long");
    }
}