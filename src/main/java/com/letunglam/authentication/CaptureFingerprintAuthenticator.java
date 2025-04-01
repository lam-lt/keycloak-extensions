package com.letunglam.authentication;

import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Collections;
import java.util.List;

public class CaptureFingerprintAuthenticator implements Authenticator {

    private static final Logger log = Logger.getLogger(CaptureFingerprintAuthenticator.class);

    // CRITICAL: Matches the 'name' attribute of the hidden input in login.ftl
    public static final String FINGERPRINT_PARAM = "device_fingerprint";

    // Key used to store the fingerprint in the authentication session notes
    public static final String FINGERPRINT_AUTH_NOTE = "browser.fingerprint";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        log.info("Authenticate triggered in CaptureFingerprintAuthenticator");
        try {
            MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
            if (formData == null) {
                log.warn("Form data is null, cannot retrieve fingerprint.");
                context.success(); // Proceed without fingerprint
                return;
            }

            String fingerprint = formData.getFirst(FINGERPRINT_PARAM);

            if (fingerprint != null && !fingerprint.trim().isEmpty()) {
                log.infof("Retrieved browser fingerprint (hash): %s", fingerprint); // Avoid logging full fingerprint if sensitive
                AuthenticationSessionModel authSession = context.getAuthenticationSession();
                if (authSession != null) {
                    // Store it in the AuthenticationSessionModel notes.
                    // The event listener *might* be able to access this via event details.
                    authSession.setAuthNote(FINGERPRINT_AUTH_NOTE, fingerprint);
                    log.infof("Stored fingerprint in auth session note [%s] for auth session [%s]",
                            FINGERPRINT_AUTH_NOTE, authSession.getParentSession().getId());
                } else {
                    log.warn("AuthenticationSessionModel is null, cannot store fingerprint note.");
                }
            } else {
                log.warnf("Browser fingerprint parameter '%s' not found or is empty in form submission.", FINGERPRINT_PARAM);
            }
        } catch (Exception e) {
            log.error("Error retrieving or storing browser fingerprint", e);
            // Do not block login flow on fingerprint error
        }
        // Always proceed to the next step
        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
    }

    @Override
    public boolean requiresUser() {
        return false; // Doesn't need user context directly
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true; // Generally applicable
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // No required actions
    }

    @Override
    public void close() {
        // No resources to close
    }

    // --- Factory Class ---
    public static class Factory implements AuthenticatorFactory {

        public static final String PROVIDER_ID = "capture-browser-fingerprint";
        private static final CaptureFingerprintAuthenticator SINGLETON = new CaptureFingerprintAuthenticator();

        @Override
        public String getId() {
            return PROVIDER_ID;
        }

        @Override
        public Authenticator create(KeycloakSession session) {
            return SINGLETON;
        }

        @Override
        public String getDisplayType() {
            return "Capture Browser Fingerprint";
        }

        @Override
        public String getHelpText() {
            return "Retrieves a browser fingerprint submitted via a hidden form field ('" + FINGERPRINT_PARAM + "') and stores it for potential use by other extensions (like event listeners).";
        }

        @Override
        public String getReferenceCategory() {
            return "Device Trust"; // Or Authentication
        }

        @Override
        public boolean isConfigurable() {
            return false; // Not configurable for now
        }

        @Override
        public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
            // ALTERNATIVE: Runs if possible, doesn't block if fails/missing. Good starting point.
            // REQUIRED: Would block login if this step fails hard (e.g., throws exception), but not if fingerprint is just missing.
            // DISABLED: Off.
            return REQUIREMENT_CHOICES; // Provides ALTERNATIVE, REQUIRED, DISABLED, etc.
        }

        @Override
        public boolean isUserSetupAllowed() {
            return false; // Users cannot configure this
        }

        @Override
        public List<ProviderConfigProperty> getConfigProperties() {
            return Collections.emptyList();
        }

        @Override
        public void init(Config.Scope config) {
        }

        @Override
        public void postInit(KeycloakSessionFactory factory) {
        }

        @Override
        public void close() {
        }
    }
}