package devpiolet.backend.security;

import devpiolet.backend.entity.User;
import devpiolet.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GithubOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserService userService;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. Fetch user attributes from GitHub's user info endpoint
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        Map<String, Object> attributes = oauth2User.getAttributes();

        // 2. Extract access token string
        String accessToken = userRequest.getAccessToken().getTokenValue();

        // 3. Extract requested scopes (or fallback to defaults)
        String scopes = (userRequest.getAccessToken().getScopes() != null && !userRequest.getAccessToken().getScopes().isEmpty())
                ? String.join(",", userRequest.getAccessToken().getScopes())
                : "read:user,repo";

        // 4. Upsert user into database via UserService
        User user = userService.upsertFromGithub(attributes, accessToken, scopes);

        // 5. Wrap inside AppUserPrincipal for SecurityContext
        return new AppUserPrincipal(user, attributes);
    }
}