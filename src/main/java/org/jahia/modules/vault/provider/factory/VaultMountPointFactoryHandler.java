package org.jahia.modules.vault.provider.factory;

import java.io.Serializable;
import java.util.Locale;
import javax.jcr.RepositoryException;
import org.jahia.modules.external.admin.mount.AbstractMountPointFactoryHandler;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.utils.i18n.Messages;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.webflow.execution.RequestContext;

public class VaultMountPointFactoryHandler extends AbstractMountPointFactoryHandler<VaultMountPointFactory> implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultMountPointFactoryHandler.class);
    private static final String BUNDLE = "resources.external-provider-vault";
    private VaultMountPointFactory vaultMountPointFactory;
    private String stateCode;
    private String messageKey;

    public void init(RequestContext requestContext) {
        vaultMountPointFactory = new VaultMountPointFactory();
        try {
            super.init(requestContext, vaultMountPointFactory);
        } catch (RepositoryException e) {
            LOGGER.error("Error retrieving mount point", e);
        }
        requestContext.getFlowScope().put("vaultFactory", vaultMountPointFactory);
    }

    public String getFolderList() {
        final JSONObject result = new JSONObject();
        try {
            final JSONArray folders = JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<JSONArray>() {
                @Override
                public JSONArray doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    return getSiteFolders(session.getWorkspace());
                }
            });

            result.put("folders", folders);
        } catch (RepositoryException e) {
            LOGGER.error("Error trying to retrieve local folders", e);
        } catch (JSONException e) {
            LOGGER.error("Error trying to construct JSON from local folders", e);
        }

        return result.toString();
    }

    public Boolean save(MessageContext messageContext, RequestContext requestContext) {
        stateCode = "SUCCESS";
        final Locale locale = LocaleContextHolder.getLocale();
        final boolean validVaultPoint = validateVault(vaultMountPointFactory);
        if (!validVaultPoint) {
            LOGGER.error("Error saving mount point : " + vaultMountPointFactory.getName() + "with the root : " + vaultMountPointFactory.getUrl());
            final MessageBuilder messageBuilder = new MessageBuilder().error().defaultText(Messages.get(BUNDLE, "serverSettings.vaultMountPointFactory.save.error", locale));
            messageContext.addMessage(messageBuilder.build());
            requestContext.getConversationScope().put("adminURL", getAdminURL(requestContext));
            return false;
        }
        try {
            final boolean available = super.save(vaultMountPointFactory);
            if (available) {
                stateCode = "SUCCESS";
                messageKey = "serverSettings.vaultMountPointFactory.save.success";
                requestContext.getConversationScope().put("adminURL", getAdminURL(requestContext));
                return true;
            } else {
                LOGGER.warn("Mount point availability problem : " + vaultMountPointFactory.getName() + "with the root : " + vaultMountPointFactory.getUrl() + "the mount point is created but unmounted");
                stateCode = "WARNING";
                messageKey = "serverSettings.vaultMountPointFactory.save.unavailable";
                requestContext.getConversationScope().put("adminURL", getAdminURL(requestContext));
                return true;
            }
        } catch (RepositoryException e) {
            LOGGER.error("Error saving mount point : " + vaultMountPointFactory.getName(), e);
            final MessageBuilder messageBuilder = new MessageBuilder().error().defaultText(Messages.get(BUNDLE, "serverSettings.vaultMountPointFactory.save.error", locale));
            messageContext.addMessage(messageBuilder.build());
        }
        return false;
    }

    private boolean validateVault(VaultMountPointFactory vaultMountPointFactory) {
        //TODO: check if the vault is accessible/unsealed.
        return true;
    }

    @Override
    public String getAdminURL(RequestContext requestContext) {
        final StringBuilder builder = new StringBuilder(super.getAdminURL(requestContext));
        if (stateCode != null && messageKey != null) {
            builder.append("?stateCode=").append(stateCode).append("&messageKey=").append(messageKey).append("&bundleSource=").append(BUNDLE);
        }
        return builder.toString();
    }
}
