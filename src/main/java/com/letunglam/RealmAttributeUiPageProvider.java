package com.letunglam;

import java.util.List;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.ui.extend.UiPageProvider;

public class RealmAttributeUiPageProvider implements UiPageProvider {

    private final KeycloakSession session;
    
    public RealmAttributeUiPageProvider(KeycloakSession session) {
        this.session = session;
    }
    
    @Override
    public void close() {
        // no-op
    }
    
    @Override
    public String getHelpText() {
        return "Realm Attribute UI Page Provider";
    }
    
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return null;
    }
}
