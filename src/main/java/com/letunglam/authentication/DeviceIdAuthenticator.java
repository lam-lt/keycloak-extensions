package com.letunglam.authentication;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.*;

/**
 * An authenticator that checks for a DEVICE_ID cookie and sets one if it doesn't exist.
 */
public class DeviceIdAuthenticator implements Authenticator {

    private static final Logger log = Logger.getLogger(DeviceIdAuthenticator.class);
    
    // Name of the cookie
    public static final String DEVICE_ID_COOKIE = "DEVICE_ID";
    
    // Auth note to store device ID for use by other authenticators or event listeners
    public static final String DEVICE_ID_NOTE = "device.id";
    
    // Default cookie max age (1 year in seconds)
    private static final int DEFAULT_MAX_AGE = 365 * 24 * 60 * 60;

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        log.info("DeviceIdAuthenticator.authenticate called");
        
        // Check if DEVICE_ID cookie already exists
        Cookie deviceIdCookie = context.getHttpRequest().getHttpHeaders().getCookies().get(DEVICE_ID_COOKIE);
        
        if (deviceIdCookie != null) {
            // Cookie exists, store the value in auth session note
            String existingDeviceId = deviceIdCookie.getValue();
            log.infof("Found existing DEVICE_ID cookie: %s", existingDeviceId);
            storeDeviceIdNote(context, existingDeviceId);
            context.success();
            return;
        }
        
        // No cookie found, create a new one
        String newDeviceId = generateDeviceId();
        log.infof("Setting new DEVICE_ID cookie: %s", newDeviceId);
        
        // Store in auth session
        storeDeviceIdNote(context, newDeviceId);
        
        // Set cookie in response
        int maxAge = getMaxAge(context.getRealm());
        NewCookie cookie = createDeviceIdCookie(context, newDeviceId, maxAge);
        
        // Create a response with the cookie and continue the flow
        Response response = Response.status(Response.Status.FOUND)
                .location(context.getRefreshExecutionUrl())
                .cookie(cookie)
                .build();
        
        // Challenge sends the response with the cookie and then continues the flow
        context.challenge(response);
    }

    private void storeDeviceIdNote(AuthenticationFlowContext context, String deviceId) {
        try {
            context.getAuthenticationSession().setAuthNote(DEVICE_ID_NOTE, deviceId);
        } catch (Exception e) {
            log.warn("Failed to store device ID in auth notes", e);
        }
    }

    private String generateDeviceId() {
        return UUID.randomUUID().toString();
    }

    private int getMaxAge(RealmModel realm) {
        // Could be made configurable from authenticator config
        return DEFAULT_MAX_AGE;
    }

    private NewCookie createDeviceIdCookie(AuthenticationFlowContext context, String deviceId, int maxAge) {
        return new NewCookie(
                DEVICE_ID_COOKIE,             // name
                deviceId,                     // value
                "/",                          // path - root path so it's available everywhere
                null,                         // domain - null means use the domain from the request
                "Device ID",                  // comment
                maxAge,                       // max age in seconds
                true,                         // secure - only send over HTTPS
                true);                        // httpOnly - not accessible from JavaScript
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // Nothing to do here, just continue the flow
        context.success();
    }

    @Override
    public boolean requiresUser() {
        // This authenticator does not require an identified user
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        // This authenticator is always configured
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // No required actions needed
    }

    @Override
    public void close() {
        // No resources to close
    }

    // Factory implementation
    public static class Factory implements AuthenticatorFactory {
        public static final String PROVIDER_ID = "device-id-cookie";
        private static final DeviceIdAuthenticator SINGLETON = new DeviceIdAuthenticator();
        
        private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
        
        /* Could add configuration properties here, like:
        static {
            ProviderConfigProperty property = new ProviderConfigProperty();
            property.setName("cookie.max.age");
            property.setLabel("Cookie Max Age");
            property.setType(ProviderConfigProperty.STRING_TYPE);
            property.setHelpText("Max age of the DEVICE_ID cookie in seconds (default: 1 year)");
            configProperties.add(property);
        }
        */

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
            return "Device ID Cookie";
        }

        @Override
        public String getHelpText() {
            return "Sets a persistent DEVICE_ID cookie if it doesn't already exist in the browser.";
        }

        @Override
        public String getReferenceCategory() {
            return "Device Tracking";
        }

        @Override
        public boolean isConfigurable() {
            return false; // Set to true if you add configuration properties
        }

        @Override
        public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
            return new AuthenticationExecutionModel.Requirement[] {
                    AuthenticationExecutionModel.Requirement.REQUIRED,
                    AuthenticationExecutionModel.Requirement.ALTERNATIVE,
                    AuthenticationExecutionModel.Requirement.DISABLED
            };
        }

        @Override
        public boolean isUserSetupAllowed() {
            return false;
        }

        @Override
        public List<ProviderConfigProperty> getConfigProperties() {
            return configProperties;
        }

        @Override
        public void init(Config.Scope config) {
            // No initialization needed
        }

        @Override
        public void postInit(KeycloakSessionFactory factory) {
            // No post-initialization needed
        }

        @Override
        public void close() {
            // No resources to close
        }
    }
}
