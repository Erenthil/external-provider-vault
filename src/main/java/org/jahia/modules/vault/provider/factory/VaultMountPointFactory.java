package org.jahia.modules.vault.provider.factory;

import java.io.Serializable;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import org.hibernate.validator.constraints.NotEmpty;
import org.jahia.modules.external.admin.mount.AbstractMountPointFactory;
import org.jahia.modules.external.admin.mount.validator.LocalJCRFolder;
import org.jahia.modules.vault.provider.VaultDataSource;
import org.jahia.services.content.JCRNodeWrapper;

public class VaultMountPointFactory extends AbstractMountPointFactory implements Serializable {

    @NotEmpty
    private String name;
    @LocalJCRFolder
    private String localPath;
    @NotEmpty
    private String openTimeout;
    @NotEmpty
    private String publicToken;
    @NotEmpty
    private String readTimeout;
    @NotEmpty
    private String url;
    private String unsealKeys = null;

    public VaultMountPointFactory() {
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public void populate(JCRNodeWrapper nodeWrapper) throws RepositoryException {
        super.populate(nodeWrapper);
        this.name = getName(nodeWrapper.getName());
        try {
            this.localPath = nodeWrapper.getProperty(VaultDataSource.PROP_MOUNT_POINT).getNode().getPath();
        } catch (PathNotFoundException e) {
            // no local path defined for this mount point
        }
        this.url = nodeWrapper.getPropertyAsString(VaultDataSource.PROP_URL);
        this.publicToken = nodeWrapper.getPropertyAsString(VaultDataSource.PROP_PUBLIC_TOKEN);
        this.readTimeout = nodeWrapper.getPropertyAsString(VaultDataSource.PROP_READ_TIMEOUT);
        this.openTimeout = nodeWrapper.getPropertyAsString(VaultDataSource.PROP_OPEN_TIMEOUT);
        this.unsealKeys = nodeWrapper.getPropertyAsString(VaultDataSource.PROP_UNSEAL_KEYS);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getLocalPath() {
        return localPath;
    }

    @Override
    public String getMountNodeType() {
        return VaultDataSource.NT_VAULT_MOUNT_POINT;
    }

    @Override
    public void setProperties(JCRNodeWrapper mountNode) throws RepositoryException {
        mountNode.setProperty(VaultDataSource.PROP_URL, url);
        mountNode.setProperty(VaultDataSource.PROP_PUBLIC_TOKEN, publicToken);
        mountNode.setProperty(VaultDataSource.PROP_READ_TIMEOUT, readTimeout);
        mountNode.setProperty(VaultDataSource.PROP_OPEN_TIMEOUT, openTimeout);
        if (unsealKeys != null && !unsealKeys.isEmpty()) {
            mountNode.setProperty(VaultDataSource.PROP_UNSEAL_KEYS, unsealKeys);
        }
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public void setOpenTimeout(String openTimeout) {
        this.openTimeout = openTimeout;
    }

    public void setPublicToken(String publicToken) {
        this.publicToken = publicToken;
    }

    public void setReadTimeout(String readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setUnsealKeys(String unsealKeys) {
        this.unsealKeys = unsealKeys;
    }

    public String getOpenTimeout() {
        return openTimeout;
    }

    public String getPublicToken() {
        return publicToken;
    }

    public String getReadTimeout() {
        return readTimeout;
    }

    public String getUnsealKeys() {
        return unsealKeys;
    }

}
