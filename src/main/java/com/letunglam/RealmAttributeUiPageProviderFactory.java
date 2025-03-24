package com.letunglam;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.ui.extend.UiPageProvider;
import org.keycloak.services.ui.extend.UiPageProviderFactory;

import java.util.List;

public class RealmAttributeUiPageProviderFactory implements UiPageProviderFactory<ComponentModel> {

    private static final Logger log = Logger.getLogger(RealmAttributeUiPageProviderFactory.class);
    private static final String ID = "Attributes";
    private KeycloakSession session;

    @Override
    public UiPageProvider create(KeycloakSession session) {
        return new RealmAttributeUiPageProvider(session);
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

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getHelpText() {
        return "Here you can store realm attributes";
    }

    @Override
    public void onCreate(KeycloakSession session, RealmModel realm, ComponentModel model) {
        String key = model.get("key");
        validateKeyUniqueness(realm, model, key);
        String value = model.get("value");
        realm.setAttribute(key, value);
    }

    @Override
    public void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel) {
        String oldKey = oldModel.get("key");
        String newKey = newModel.get("key");
        if (!oldKey.equals(newKey)) {
            validateKeyUniqueness(realm, newModel, newKey);
            realm.removeAttribute(oldKey);
        }
        String value = newModel.get("value");
        realm.setAttribute(newKey, value);
    }

    @Override
    public void preRemove(KeycloakSession session, RealmModel realm, ComponentModel model) {
        String key = model.get("key");
        realm.removeAttribute(key);
    }

    private void validateKeyUniqueness(RealmModel realm, ComponentModel model, String key) {
        if (key == null || key.trim().isEmpty()) {
            log.warn("Key is required");
            throw new IllegalArgumentException("Key is required");
        }
        
        // Check if any other component has the same key
        boolean isDuplicate = realm.getComponentsStream(realm.getId(), UiPageProvider.class.getName())
                .filter(component -> !component.getId().equals(model.getId())) // Exclude the current component
                .anyMatch(component -> key.equals(component.get("key")));
        
        if (isDuplicate) {
            log.warn("Component with key '" + key + "' already exists.");
            throw new IllegalArgumentException("Entry with key '" + key + "' already exists. Keys must be unique.");
        }

        if (realm.getAttribute(key) != null) {
            log.warn("Realm attribute with key '" + key + "' already exists.");
            throw new IllegalArgumentException("Entry with key '" + key + "' already exists. Keys must be unique.");
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("key")
                .label("Key")
                .helpText("Unique identifier for this entry")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name("value")
                .label("Value")
                .helpText("The value associated with this key")
                .type(ProviderConfigProperty.TEXT_TYPE)
                .add()
                .build();
    }
}
