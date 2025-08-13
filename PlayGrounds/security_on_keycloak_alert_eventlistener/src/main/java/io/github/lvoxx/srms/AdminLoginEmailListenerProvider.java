package io.github.lvoxx.srms;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;

/**
 * Custom EventListenerProvider to send an email alert to the admin when someone
 * logs into the Keycloak admin console.
 */
public class AdminLoginEmailListenerProvider implements EventListenerProvider {

    private final KeycloakSession session;
    private final String adminEmail;
    private static final String EMAIL_SUBJECT = "Alert: Admin Console Login Detected";
    private static final String EMAIL_FILE = "email-template.html";

    public AdminLoginEmailListenerProvider(KeycloakSession session, String adminEmail) {
        this.session = session;
        this.adminEmail = adminEmail;
    }

    @Override
    public void onEvent(Event event) {
        // Check for LOGIN event and verify if it’s for the admin console
        if (event.getType() == EventType.LOGIN && isAdminConsoleLogin(event)) {
            // Collect relevant information from the event
            String ipAddress = event.getDetails().getOrDefault("ip_address", "Unknown");
            String username = event.getDetails().getOrDefault("username", "Unknown");
            long loginTime = event.getTime();
            String serverUrl = session.getContext().getUri().getBaseUri().toString();

            // Read HTML template from resources
            String htmlTemplate = readTemplateFromResource(EMAIL_FILE);

            // Replace placeholders in the template
            String emailBody = htmlTemplate
                    .replace("${username}", username)
                    .replace("${ipAddress}", ipAddress)
                    .replace("${loginTime}", new Date(loginTime).toString())
                    .replace("${serverUrl}", serverUrl);

            // Send the email
            sendEmail(emailBody);
        }
    }

    /**
     * Determines if the login event is for the admin console.
     *
     * @param event The Keycloak event.
     * @return True if the login is for the admin console, false otherwise.
     */
    private boolean isAdminConsoleLogin(Event event) {
        // Check if the client ID is related to the admin console
        String clientId = event.getClientId();
        return "security-admin-console".equals(clientId) || "master-realm".equals(clientId) || "srms".equals(clientId);
    }

    @Override
    public void onEvent(AdminEvent event, boolean arg1) {
    }

    /**
     * Reads the HTML email template from the classpath resources.
     *
     * @param resourceName The name of the resource file.
     * @return The content of the template as a string.
     */
    private String readTemplateFromResource(String resourceName) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new RuntimeException("Resource not found: " + resourceName);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read email template", e);
        }
    }

    /**
     * Sends the email using Keycloak's EmailSenderProvider.
     *
     * @param htmlBody The HTML body of the email.
     */
    private void sendEmail(String htmlBody) {
        EmailSenderProvider emailSender = session.getProvider(EmailSenderProvider.class);
        try {
            emailSender.send(null, adminEmail, EMAIL_SUBJECT, "Admin login alert (plain text fallback)", htmlBody);
        } catch (EmailException e) {
            // Log the error; in production, handle appropriately (e.g., logging framework)
            System.err.println("Failed to send admin login alert email: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        // No resources to close
    }
}
