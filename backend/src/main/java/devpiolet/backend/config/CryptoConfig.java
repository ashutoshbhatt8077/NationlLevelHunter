package devpiolet.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@Configuration
public class CryptoConfig {

    @Value("${app.crypto.token-encryptor-password:devpilot_local_secret}")
    private String password;

    @Value("${app.crypto.token-encryptor-salt:12345678}")
    private String salt;

    /**
     * Provides a standard hex-encoded 256-bit AES TextEncryptor
     * using the configured password and salt.
     */
    @Bean
    public TextEncryptor tokenEncryptor() {
        return Encryptors.text(password, salt);
    }
}