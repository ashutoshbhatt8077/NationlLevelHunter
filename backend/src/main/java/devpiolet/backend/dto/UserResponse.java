package devpiolet.backend.dto;

import java.util.UUID;

public record UserResponse(
        UUID id,
        Long ghId,
        String githubUsername,
        String displayName,
        String avatarUrl
) {}