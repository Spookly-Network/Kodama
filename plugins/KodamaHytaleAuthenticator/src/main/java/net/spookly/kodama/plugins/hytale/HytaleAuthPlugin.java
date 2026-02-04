package net.spookly.kodama.plugins.hytale;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.spookly.kodama.brain.plugin.BrainPrepareInstanceContext;
import net.spookly.kodama.brain.plugin.BrainPrepareInstanceMutation;
import net.spookly.kodama.brain.plugin.KodamaBrainPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HytaleAuthPlugin implements KodamaBrainPlugin {

    private static final Logger logger = LoggerFactory.getLogger(HytaleAuthPlugin.class);

    private final HytaleAuthClient authClient;

    public HytaleAuthPlugin() {
        this(new HytaleAuthClient(HytaleAuthConfig.fromEnvironment()));
    }

    HytaleAuthPlugin(HytaleAuthClient authClient) {
        this.authClient = Objects.requireNonNull(authClient, "authClient");
    }

    @Override
    public String id() {
        return "hytale-auth";
    }

    @Override
    public BrainPrepareInstanceMutation onPrepareInstance(BrainPrepareInstanceContext context) {
        if (context == null) {
            throw new IllegalStateException("prepare context is required");
        }
        HytaleAuthClient.HytaleSession session = authClient.createSession();
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("HYTALE_SERVER_SESSION_TOKEN", session.sessionToken());
        variables.put("HYTALE_SERVER_IDENTITY_TOKEN", session.identityToken());
        logger.info("Issued Hytale session tokens for instanceId={}", context.instanceId());
        return new BrainPrepareInstanceMutation(variables, Set.of());
    }
}
