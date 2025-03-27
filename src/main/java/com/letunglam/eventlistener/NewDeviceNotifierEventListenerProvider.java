package com.letunglam.eventlistener;

import com.letunglam.authentication.CaptureFingerprintAuthenticator;
import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.*; // Includes RealmModel, UserModel, KeycloakSession, etc.

import java.util.*;
import java.util.stream.Collectors;

public class NewDeviceNotifierEventListenerProvider implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(NewDeviceNotifierEventListenerProvider.class);
    private final KeycloakSession session;

    // User attribute to store known device signatures (Fingerprints or Normalized UAs)
    private static final String USER_ATTRIBUTE_KNOWN_DEVICE_SIGNATURES = "knownDeviceSignatures";
    // Maximum number of signatures to store per user to prevent attribute bloat
    private static final int MAX_STORED_SIGNATURES = 20; // Make this configurable?

    public NewDeviceNotifierEventListenerProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        // Only react to successful user logins
        if (!EventType.LOGIN.equals(event.getType())) {
            return;
        }

        // Basic validation
        if (event.getRealmId() == null || event.getUserId() == null) {
            log.warn("Login event missing RealmId or UserId.");
            return;
        }

        RealmModel realm = session.realms().getRealm(event.getRealmId());
        UserModel user = session.users().getUserById(realm, event.getUserId());

        if (realm == null || user == null) {
            log.warnf("Realm (%s) or User (%s) not found for login event.", event.getRealmId(), event.getUserId());
            return;
        }

        // Optional: Skip service accounts if desired
        // if (user.getServiceAccountClientLink() != null) {
        //     log.debugf("Skipping login event for service account user: %s", user.getUsername());
        //     return;
        // }

        String ipAddress = event.getIpAddress(); // Still useful for the notification email context
        String userAgent = session.getContext().getRequestHeaders().getHeaderString("User-Agent");
        String currentSignature = null;
        String deviceInfoForEmail = "Unknown Device";
        boolean isSignatureBasedOnFingerprint = false;

        // --- Attempt to get Fingerprint from Event Details ---
        // This relies on Keycloak propagating the auth note set by the authenticator. VERIFY THIS WORKS.
        String fingerprint = null;
        Map<String, String> details = event.getDetails();
        if (details != null) {
            fingerprint = details.get(CaptureFingerprintAuthenticator.FINGERPRINT_AUTH_NOTE);
            if (fingerprint != null && !fingerprint.trim().isEmpty()) {
                log.infof("Using fingerprint found in event details for user %s.", user.getUsername());
                currentSignature = fingerprint; // Use raw fingerprint as signature
                deviceInfoForEmail = "Browser Fingerprint: [" + fingerprint.substring(0, Math.min(fingerprint.length(), 10)) + "...]";
                isSignatureBasedOnFingerprint = true;
            } else {
                log.debugf("Fingerprint auth note '%s' not found or empty in login event details for user %s.",
                        CaptureFingerprintAuthenticator.FINGERPRINT_AUTH_NOTE, user.getUsername());
            }
        } else {
            log.debugf("Event details map is null for user %s login. Cannot check for fingerprint.", user.getUsername());
        }

        // --- Fallback to Normalized User-Agent if Fingerprint is missing ---
        // **DO NOT INCLUDE IP ADDRESS IN THE SIGNATURE** due to dynamic IPs.
        if (currentSignature == null) {
            if (userAgent != null && !userAgent.trim().isEmpty()) {
                currentSignature = normalizeUserAgent(userAgent);
                log.infof("Falling back to normalized User-Agent signature for user %s: %s", user.getUsername(), currentSignature);
                deviceInfoForEmail = "Browser/OS: " + userAgent; // Can provide full UA in email
            } else {
                log.warnf("Cannot create device signature for user %s: Fingerprint missing and User-Agent is also missing or empty.", user.getUsername());
                // Cannot identify the device, so we cannot determine if it's new. Abort.
                return;
            }
        }

        // --- Check against known device signatures stored on the user ---
        List<String> knownSignatures = user.getAttributeStream(USER_ATTRIBUTE_KNOWN_DEVICE_SIGNATURES)
                .collect(Collectors.toList());

        boolean isNewDevice = !knownSignatures.contains(currentSignature);

        if (isNewDevice) {
            log.infof("New device signature detected for user %s. Signature: %s (Type: %s)",
                    user.getUsername(), currentSignature, isSignatureBasedOnFingerprint ? "Fingerprint" : "UserAgent");

            // 1. Add the new signature to the user's attributes
            List<String> updatedKnownSignatures = new ArrayList<>(knownSignatures);
            updatedKnownSignatures.add(currentSignature);

            // Limit the number of stored signatures to prevent attribute bloat
            while (updatedKnownSignatures.size() > MAX_STORED_SIGNATURES) {
                log.debugf("Max signatures (%d) reached for user %s. Removing oldest: %s",
                        MAX_STORED_SIGNATURES, user.getUsername(), updatedKnownSignatures.get(0));
                updatedKnownSignatures.remove(0); // Remove the oldest signature
            }
            user.setAttribute(USER_ATTRIBUTE_KNOWN_DEVICE_SIGNATURES, updatedKnownSignatures);
            log.debugf("Updated known signatures for user %s. Count: %d", user.getUsername(), updatedKnownSignatures.size());

            // 2. Send Notification Email
            sendNewDeviceNotification(realm, user, ipAddress, deviceInfoForEmail, event.getTime());

        } else {
            log.infof("Known device signature detected for user %s. Signature: %s", user.getUsername(), currentSignature);
            // Optional: Could update a 'last seen' timestamp if storing more complex data, but not needed for this simple case.
        }
    }

    /**
     * Creates a simplified, normalized representation of the User-Agent string.
     * Replace with a robust library (like YAUAA) for production use if high accuracy is needed.
     * IMPORTANT: This should aim for stability across minor browser updates.
     *
     * @param userAgent Raw User-Agent string
     * @return Normalized string or a default value
     */
    private String normalizeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "UA_Unknown";
        }
        // VERY Basic example - highly recommend a proper library
        String uaLower = userAgent.toLowerCase();
        try {
            if (uaLower.contains("firefox/")) return "UA_Firefox";
            if (uaLower.contains("chrome/") && !uaLower.contains("edg/")) return "UA_Chrome";
            if (uaLower.contains("safari/") && !uaLower.contains("chrome/") && !uaLower.contains("edg/")) return "UA_Safari";
            if (uaLower.contains("edg/")) return "UA_Edge";
            if (uaLower.contains("msie") || uaLower.contains("trident")) return "UA_IE";
        } catch (Exception e) {
            log.warnf(e, "Error normalizing User-Agent: %s", userAgent);
        }
        // Fallback: Use a significant prefix if no major browser detected
        return "UA_Other_" + userAgent.substring(0, Math.min(userAgent.length(), 40)).replaceAll("[^a-zA-Z0-9]", "_");
    }

    /**
     * Sends the notification email to the user.
     */
    private void sendNewDeviceNotification(RealmModel realm, UserModel user, String ipAddress, String deviceInfo, long eventTime) {
        if (user.getEmail() == null || user.getEmail().isBlank() || !Boolean.TRUE.equals(user.isEmailVerified())) {
            log.warnf("Cannot send new device notification to user %s: Email missing, blank, or not verified.", user.getUsername());
            return;
        }

        try {
            EmailTemplateProvider emailProvider = session.getProvider(EmailTemplateProvider.class);
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("username", user.getUsername());
            attributes.put("ipAddress", ipAddress != null ? ipAddress : "Not Available");
            attributes.put("deviceInfo", deviceInfo); // Fingerprint hash prefix or Normalized UA
            attributes.put("timestamp", new Date(eventTime * 1000L).toString()); // Convert Keycloak seconds to ms

            // Optional: Add GeoIP lookup info here if implemented separately
            // attributes.put("location", getGeoLocation(ipAddress));

            // Use custom template name and subject key (defined in theme)
            emailProvider.setRealm(realm)
                    .setUser(user)
                    .send("newDeviceLoginSubject", "new-device-login-email.ftl", attributes);

            log.infof("Sent new device login notification to user %s for device signature.", user.getUsername());

        } catch (EmailException e) {
            log.errorf(e, "Failed to send new device notification email to user %s", user.getUsername());
        } catch (Exception e) { // Catch broader exceptions during email sending
            log.errorf(e, "Unexpected error sending new device notification email to user %s", user.getUsername());
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Not processing admin events
    }

    @Override
    public void close() {
        // No resources to close
    }
}
