package com.letunglam;

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


public class AdminUiPage implements UiPageProvider, UiPageProviderFactory<ComponentModel> {

    private final String ID = "Attributes";

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
        return "Here you can store your Todo items";
    }

    @Override
    public void onCreate(KeycloakSession session, RealmModel realm, ComponentModel model) {
        String key = model.get("key");
        validateKeyUniqueness(session, realm, model, key);
        String value = model.get("value");
        realm.setAttribute(key, value);

        // Log
        System.out.println("Set realm attribute: " + key + " = " + value);
        System.out.println("Get realm attribute, key = " + key + ": " + realm.getAttribute(key));

    }

    public void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel model) {
        String key = model.get("key");
        validateKeyUniqueness(session, realm, model, key);
        String value = model.get("value");
        realm.setAttribute(key, value);

        // Log
        System.out.println("Updated realm attribute: " + key + " = " + value);
        System.out.println("Get realm attribute after update, key = " + key + ": " + realm.getAttribute(key));
    }

    private void validateKeyUniqueness(KeycloakSession session, RealmModel realm, ComponentModel model, String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key is required");
        }
        
        // Check if any other component has the same key
        boolean isDuplicate = realm.getComponentsStream(realm.getId(), UiPageProvider.class.getName())
                .filter(component -> !component.getId().equals(model.getId())) // Exclude the current component
                .anyMatch(component -> key.equals(component.get("key")));
        
        if (isDuplicate) {
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