package com.letunglam.eventlistener;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class NewDeviceNotifierEventListenerProviderFactory implements EventListenerProviderFactory {

    // This ID is used in the Keycloak Admin Console Events -> Config -> Event Listeners
    public static final String PROVIDER_ID = "new-device-notifier";

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new NewDeviceNotifierEventListenerProvider(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void init(Config.Scope config) {
        // Load configuration if needed (e.g., max stored signatures)
        // int maxSignatures = config.getInt("maxSignatures", 20);
        // NewDeviceNotifierEventListenerProvider.MAX_STORED_SIGNATURES = maxSignatures; // If MAX_STORED_SIGNATURES is static
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Post init logic
    }

    @Override
    public void close() {
        // Cleanup logic
    }
}
