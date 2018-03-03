package org.jahia.modules.vault.provider;

import javax.jcr.RepositoryException;
import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.*;

public class VaultProviderFactory implements ProviderFactory {

    @Override
    public String getNodeTypeName() {
        return VaultDataSource.NT_VAULT_MOUNT_POINT;
    }

    @Override
    public JCRStoreProvider mountProvider(JCRNodeWrapper mountPoint) throws RepositoryException {
        final ExternalContentStoreProvider provider = (ExternalContentStoreProvider) SpringContextSingleton.getBean("ExternalStoreProviderPrototype");
        provider.setKey(mountPoint.getIdentifier());
        provider.setMountPoint(mountPoint.getPath());

        final VaultDataSource dataSource = new VaultDataSource();
        dataSource.setUrl(mountPoint.getProperty(VaultDataSource.PROP_URL).getString());
        dataSource.setPublicToken(mountPoint.getProperty(VaultDataSource.PROP_PUBLIC_TOKEN).getString());
        dataSource.setOpenTimeout(Long.valueOf(mountPoint.getProperty(VaultDataSource.PROP_OPEN_TIMEOUT).getString()));
        dataSource.setReadTimeout(Long.valueOf(mountPoint.getProperty(VaultDataSource.PROP_READ_TIMEOUT).getString()));
        if (mountPoint.hasProperty(VaultDataSource.PROP_UNSEAL_KEYS)) {
            dataSource.setUnsealKeys(mountPoint.getProperty(VaultDataSource.PROP_UNSEAL_KEYS).getString());
        }

        provider.setDataSource(dataSource);
        provider.setDynamicallyMounted(true);
        provider.setSessionFactory(JCRSessionFactory.getInstance());
        try {
            provider.start();
        } catch (JahiaInitializationException e) {
            throw new RepositoryException(e);
        }
        return provider;
    }
}
