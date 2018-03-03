package org.jahia.modules.vault.provider;

import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import com.google.common.collect.Sets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VaultDataSource implements ExternalDataSource, ExternalDataSource.CanCheckAvailability,
        ExternalDataSource.Initializable, ExternalDataSource.Writable, ExternalDataSource.SupportPrivileges {

    public static final String PROP_MOUNT_POINT = "mountPoint";
    public static final String NT_CONTENT_FOLDER = "jvep:contentFolder";
    public static final String NT_SECRET = "jvep:secret";
    public static final String NT_VAULT_MOUNT_POINT = "jvep:vaultMountPoint";
    public static final String PROP_OPEN_TIMEOUT = "openTimeout";
    public static final String PROP_PUBLIC_TOKEN = "publicToken";
    public static final String PROP_READ_TIMEOUT = "readTimeout";
    public static final String PROP_UNSEAL_KEYS = "unsealKeys";
    public static final String PROP_URL = "url";
    public static final String ATTR_VAULT_TOKEN = "x-vault-token";
    private static final Logger LOGGER = LoggerFactory.getLogger(VaultDataSource.class);
    private static final Pattern PATTERN_ID = Pattern.compile("[a-zA-Z0-9_-]+");
    private static final String PATH_SEPARATOR = "/";
    private static final String ROOT_IDENTIFIER = "root";
    private static final String VAULT_ROOT = "secret";
    private final List<String> unsealKeys = new ArrayList<>();
    private String publicToken;
    private String url;
    private Vault vault;
    private Long openTimeout;
    private Long readTimeout;

    public String authenticate(String username, String password) {
        try {
            return vault.auth().loginByLDAP(username, password).getAuthClientToken();
        } catch (VaultException ex) {
            final String errMsg = String.format("Impossible to authenticate user %s", username);
            LOGGER.error(errMsg, ex);
        }
        return null;
    }

    @Override
    public List<String> getChildren(String path) {
        final List<String> children = new ArrayList<>();
        try {
            children.addAll(vault.logical().list(getVaultPath(path), getCurrentUserToken()));
        } catch (VaultException ex) {
            LOGGER.error("Impossible to list '" + path + "'", ex);
        }
        return children;
    }

    @Override
    public ExternalData getItemByIdentifier(String identifier) throws ItemNotFoundException {
        final Map<String, String[]> properties = new HashMap<>();
        ExternalData externalData = null;
        if (ROOT_IDENTIFIER.equals(identifier)) {
            externalData = new ExternalData(identifier, PATH_SEPARATOR, NT_CONTENT_FOLDER, properties);
        }
        if (externalData == null) {
            throw new ItemNotFoundException(identifier);
        }
        return externalData;
    }

    @Override
    public ExternalData getItemByPath(String path) throws PathNotFoundException {

        final String[] splitPath = path.split(PATH_SEPARATOR);
        final int arrayLength = splitPath.length;
        if (arrayLength > 0 && (splitPath[arrayLength - 1].contains("."))) {
            splitPath[arrayLength - 1] = splitPath[arrayLength - 1].split("\\.")[0];
        }
        try {
            if (path.endsWith(Constants.ACL)) {
                throw new PathNotFoundException(path);
            }
            if (splitPath.length <= 1) {
                return getItemByIdentifier(ROOT_IDENTIFIER);
            } else {
                final String identifier = splitPath[splitPath.length - 1];
                final Matcher matcher = PATTERN_ID.matcher(identifier);
                if (!matcher.matches()) {
                    final String errMsg = "Identifier %s is incorrect, allowed characters %s";
                    LOGGER.warn(String.format(errMsg, identifier, PATTERN_ID.pattern()));
                }
                final HashMap<String, String[]> properties = new HashMap<>();
                LogicalResponse logicalResponse = null;
                try {
                    logicalResponse = vault.logical().read(getVaultPath(path), getCurrentUserToken());
                    for (Map.Entry<String, String> entry : logicalResponse.getData().entrySet()) {
                        properties.put(entry.getKey(), new String[]{entry.getValue()});
                    }
                    return new ExternalData(path, path, NT_SECRET, properties);
                } catch (VaultException ex) {
                    try {
                        final List<String> children = vault.logical().list(getVaultPath(path), getCurrentUserToken());
                        if (children.isEmpty()) {
                            throw new PathNotFoundException(ex);
                        } else {
                            return new ExternalData(path, path, NT_CONTENT_FOLDER, properties);
                        }
                    } catch (VaultException ex1) {
                        throw new PathNotFoundException(ex1);
                    }
                }
            }
        } catch (ItemNotFoundException ex) {
            throw new PathNotFoundException(ex);
        }
    }

    @Override
    public Set<String> getSupportedNodeTypes() {
        return Sets.newHashSet(
                NT_CONTENT_FOLDER,
                NT_SECRET
        );
    }

    @Override
    public boolean isAvailable() {
        try {
            return !vault.debug().health().getSealed();
        } catch (VaultException ex) {
            LOGGER.error("Vault is not unavailable", ex);
            return false;
        }
    }

    @Override
    public boolean isSupportsHierarchicalIdentifiers() {
        return true;
    }

    @Override
    public boolean isSupportsUuid() {
        return false;
    }

    @Override
    public boolean itemExists(String s) {
        return false;
    }

    @Override
    public void move(String oldPath, String newPath) {
        try {
            final ExternalData oldExternalData = getItemByPath(oldPath);
            final ExternalData newExternalData = new ExternalData(newPath, newPath, NT_SECRET, oldExternalData.getProperties());
            saveItem(newExternalData);
            removeItemByPath(oldPath);
        } catch (RepositoryException ex) {
            final String errMsg = String.format("Impossible to move from %s to %s", oldPath, newPath);
            LOGGER.error(errMsg, ex);
        }
    }

    @Override
    public void order(String path, List<String> children) {
    }

    @Override
    public void removeItemByPath(String path) throws RepositoryException {
        try {
            vault.logical().delete(getVaultPath(path), getCurrentUserToken());
        } catch (VaultException ex) {
            throw new RepositoryException("Impossible to delete path '" + path + "'", ex);
        }
    }

    @Override
    public void saveItem(ExternalData data) throws RepositoryException {
        final Map<String, Object> properties = new HashMap<>();
        for (Map.Entry<String, String[]> entry : data.getProperties().entrySet()) {
            final String[] value = entry.getValue();
            if (value != null && value.length > 0) {
                properties.put(entry.getKey(), value[0]);
            }
        }
        try {
            vault.logical().write(getVaultPath(data.getPath()), properties, getCurrentUserToken());
        } catch (VaultException ex) {
            throw new RepositoryException("Impossible to write path '" + data.getPath() + "'", ex);
        }
    }

    @Override
    public void start() {
        try {
            final VaultConfig config = new VaultConfig()
                    .address(url)
                    .token(publicToken)
                    .openTimeout(openTimeout.intValue())
                    .readTimeout(readTimeout.intValue())
                    .sslConfig(new SslConfig().build())
                    .build();
            vault = new Vault(config);
        } catch (VaultException ex) {
            LOGGER.error("Impossible to connect to Vault", ex);
        }
    }

    @Override
    public void stop() {
    }

    public void setOpenTimeout(Long openTimeout) {
        this.openTimeout = openTimeout;
    }

    public void setPublicToken(String publicToken) {
        this.publicToken = publicToken;
    }

    public void setReadTimeout(Long readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setUnsealKeys(String unsealKeys) {
        if (unsealKeys != null && !unsealKeys.isEmpty()) {
            this.unsealKeys.clear();
            this.unsealKeys.addAll(Arrays.asList(unsealKeys.split(";")));
        }
    }

    public void setUrl(String url) {
        this.url = url;
    }

    private String getCurrentUserToken() {
        String currentUserToken = null;
        try {
            JCRSessionWrapper sessionWrapper = JCRTemplate.getInstance().getSessionFactory().getCurrentUserSession(Constants.EDIT_WORKSPACE, Locale.ENGLISH, Locale.ENGLISH);
            final Object vaultToken = sessionWrapper.getAttribute(VaultDataSource.ATTR_VAULT_TOKEN);
            if (vaultToken instanceof String) {
                currentUserToken = vaultToken.toString();
            }
        } catch (RepositoryException ex) {
            LOGGER.error("Impossible to get current user session", ex);
        }
        if (currentUserToken == null) {
            currentUserToken = publicToken;
        }
        return currentUserToken;
    }

    private String getVaultPath(String path) {
        return path.replaceFirst(PATH_SEPARATOR, VAULT_ROOT + PATH_SEPARATOR);
    }

    @Override
    public String[] getPrivilegesNames(String username, String path) {

        try {
            final List<String> privilegesList = vault.logical().getCapabilitiesSelf(getVaultPath(path), getCurrentUserToken());
            final String[] privileges = new String[privilegesList.size()];
            for (int i = 0; i < privilegesList.size(); i++) {
                privileges[i] = "vault_" + privilegesList.get(i);
            }
            return privileges;
        } catch (VaultException ex) {
            final String errMsg = String.format("Impossible to get privileges for %s", path);
            LOGGER.error(errMsg, ex);
            return new String[0];
        }
    }
}
