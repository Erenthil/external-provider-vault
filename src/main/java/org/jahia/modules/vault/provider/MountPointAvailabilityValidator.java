package org.jahia.modules.vault.provider;

import javax.validation.constraints.NotNull;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.validation.JCRNodeValidator;

public class MountPointAvailabilityValidator implements JCRNodeValidator {

    private final JCRNodeWrapper node;

    public MountPointAvailabilityValidator(JCRNodeWrapper node) {
        this.node = node;
    }

    @NotNull
    public String getOpenTimeout() {
        return node.getPropertyAsString(VaultDataSource.PROP_OPEN_TIMEOUT);
    }

    @NotNull
    public String getPublicToken() {
        return node.getPropertyAsString(VaultDataSource.PROP_PUBLIC_TOKEN);
    }

    @NotNull
    public String getReadTimeout() {
        return node.getPropertyAsString(VaultDataSource.PROP_READ_TIMEOUT);
    }

    public String getUnsealKeys() {
        return node.getPropertyAsString(VaultDataSource.PROP_UNSEAL_KEYS);
    }

    @NotNull
    public String getUrl() {
        return node.getPropertyAsString(VaultDataSource.PROP_URL);
    }

}
