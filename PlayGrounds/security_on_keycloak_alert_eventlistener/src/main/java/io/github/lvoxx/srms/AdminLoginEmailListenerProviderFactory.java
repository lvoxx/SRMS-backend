package io.github.lvoxx.srms;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Factory for the AdminLoginEmailListenerProvider.
 */
public class AdminLoginEmailListenerProviderFactory implements EventListenerProviderFactory {
    public static final String PROVIDER_ID = "admin-login-email-listener";
    private String adminEmail;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new AdminLoginEmailListenerProvider(session, adminEmail);
    }

    @Override
    public void init(Config.Scope config) {
        // Load .env from classpath
        Dotenv dotenv = Dotenv.configure()
                .directory("/") // Tìm trong root của classpath
                .filename(".env") // Tên file
                .ignoreIfMissing()
                .load();
        adminEmail = dotenv.get("ADMIN_EMAIL", "admin@email.srms.com");

        // Fallback to environment variable if not found in .env
        if (adminEmail == null || adminEmail.trim().isEmpty()) {
            adminEmail = System.getenv("ADMIN_EMAIL");
        }

        // Fallback to default if still not found
        if (adminEmail == null || adminEmail.trim().isEmpty()) {
            adminEmail = "admin@example.com";
            System.err.println(
                    "ADMIN_EMAIL not configured in .env or environment variables. Using default: " + adminEmail);
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-init needed
    }

    @Override
    public void close() {
        // No resources to close
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
