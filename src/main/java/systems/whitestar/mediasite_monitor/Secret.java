package systems.whitestar.mediasite_monitor;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.response.LogicalResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.extern.log4j.Log4j;

import javax.inject.Singleton;
import java.util.Map;

/**
 * Manage Secrets in the Vault.
 * Connection and authentication settings for the vault are set via Environment Variables to prevent them from being
 * included in the code base accidentally.
 * <p>
 * VAULT_ADDR = Vault Address, including protocol (HTTP/S)
 * VAULT_ROLE = For authentication via AppRole, Role ID
 * VAULT_SECRET = AppRole Secret
 * WWW_APP = App (Secret Key) Name in Vault
 *
 * @author Tom Paulus
 * Created on 9/30/17.
 */
@Singleton
@Log4j
public class Secret {
    private static Vault vault = null;
    private static Secret secret = null;
    private String mVaultAddr;
    private String mRoleId;
    private String mSecretId;
    private String mAppName;
    private VaultConfig vaultConfig;

    Secret() throws VaultException {
        mVaultAddr = System.getenv("VAULT_ADDR");
        if (mVaultAddr.endsWith("/")) mVaultAddr = mVaultAddr.substring(0, mVaultAddr.length() - 2);
        mRoleId = System.getenv("VAULT_ROLE");
        mSecretId = System.getenv("VAULT_SECRET");
        mAppName = System.getenv("MS_APP");

        initializeVaultConnector();
    }

    public static Secret getInstance() {
        try {
            if (secret == null) secret = new Secret();
            return secret;
        } catch (VaultException e) {
            log.fatal("Could not connect to Vault", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Get a Secret String from the Default App Name
     *
     * @param secretName {@link String} Parameter Name
     * @return {@link String} Secret Value
     */
    public String getSecret(String secretName) {
        return getSecret(mAppName, secretName, String.class);
    }

    /**
     * Get a Secret from the Default App Name
     *
     * @param secretName {@link String} Parameter Name
     * @param type       {@link T} Retrieved Object Type
     * @param <T>        Retrieved Object Type
     * @return {@link T} Secret Value
     */
    public <T> T getSecret(String secretName, Class<T> type) {
        return getSecret(mAppName, secretName, type);
    }

    /**
     * Get a Secret String from a given app name
     *
     * @param appName    {@link String} App Name
     * @param secretName {@link String} Parameter Name
     * @return {@link String} Secret Value
     */
    public String getSecret(String appName, String secretName) {
        return getSecret(appName, secretName, String.class);
    }


    /**
     * Get a Secret String from a given app name
     *
     * @param appName    {@link String} App Name
     * @param secretName {@link String} Parameter Name
     * @param type       {@link T} Retrieved Object Type
     * @param <T>        Retrieved Object Type
     * @return {@link T} Secret Value
     */
    public <T> T getSecret(String appName, String secretName, Class<T> type) {
        return getSecret(appName, secretName, type, true);
    }

    private <T> T getSecret(String appName, String secretName, Class<T> type, boolean retry) {
        try {
            String response = vault.logical()
                    .read("secret/" + appName)
                    .getData().get(secretName);

            try {
                return new Gson().fromJson(response, type);
            } catch (JsonSyntaxException e) {
                //noinspection unchecked
                return (T) response;
            }

        } catch (VaultException e) {
            if (e.getHttpStatusCode() == 403) {
                log.info("Vault Token may be expired, or the Role permissions don't allow for us to access this option.");
                try {
                    authenticate();
                } catch (VaultException e1) {
                    log.error("Could not authenticate with the vault", e1);
                    throw new RuntimeException(e1);
                }

                if (retry) {
                    return getSecret(appName, secretName, type, false);
                } else {
                    throw new RuntimeException(e);
                }

            } else {
                log.error(String.format("Secret with name \"%s\" in app \"%s\" is not defined", secretName, appName));
                throw new RuntimeException(e);
            }
        }

    }

    /**
     * Get all secrets for the default application
     *
     * @return {@link Map} Secret Map
     */
    public Map<String, String> getApplication() {
        return getApplication(mAppName);
    }

    /**
     * Get all secrets for the default application
     *
     * @return {@link Map} Secret Map
     */
    public Map<String, String> getApplication(String appName) {
        try {
            return vault.logical()
                    .read("secret/" + appName)
                    .getData();
        } catch (VaultException e) {
            log.error(String.format("App \"%s\" is not defined", appName));
            throw new RuntimeException(e);
        }
    }

    /**
     * Set a Secret Value of the default app
     *
     * @param secret {@link Map} Key, Value Mappings
     * @return {@link LogicalResponse} Vault Response
     */
    public LogicalResponse setSecret(Map<String, Object> secret) {
        return setSecret(mAppName, secret);
    }

    /**
     * Set a Secret Value of a given app
     *
     * @param appName {@link String} App Name
     * @param secret  {@link Map} Key, Value Mappings
     * @return {@link LogicalResponse} Vault Response
     */
    public LogicalResponse setSecret(String appName, Map<String, Object> secret) {
        final Gson gson = new Gson();

        log.debug("App Name: " + appName);
        log.debug("Secret Payload: " + gson.toJson(secret));

        for (final Map.Entry<String, Object> pair : secret.entrySet()) {
            final Object value = pair.getValue();
            if (value != null && !(value instanceof String)) {
                pair.setValue(gson.toJson(value));
            }
        }

        try {
            return vault.logical()
                    .write("secret/" + appName, secret);
        } catch (VaultException e) {
            log.error(String.format("Problem writing secret %s", appName));
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete an App from the Vault
     *
     * @param appName {@link String } App Name
     * @return {@link LogicalResponse} Vault Response
     */
    LogicalResponse deleteSecret(String appName) {
        log.warn(String.format("Deleting Secret %s", appName));
        try {
            return vault.logical().delete("secret/" + appName);
        } catch (VaultException e) {
            log.error(String.format("Problem deleting secret %s", appName));
            throw new RuntimeException(e);
        }
    }

    private void initializeVaultConnector() throws VaultException {
        vaultConfig = new VaultConfig()
                .address(mVaultAddr)
                .build();

        vault = new Vault(vaultConfig);

        authenticate();
    }

    private void authenticate() throws VaultException {
        AuthResponse response = vault.auth().loginByAppRole("approle", mRoleId, mSecretId);
        vaultConfig.token(response.getAuthClientToken());

        vault = new Vault(vaultConfig);
    }
}
