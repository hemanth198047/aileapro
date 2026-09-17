package com.aileapro.api.ai;

import com.aileapro.ai.ChatPort;
import com.aileapro.ai.FakeChatClient;
import com.aileapro.persistence.ModelEntity;
import com.aileapro.persistence.ModelProfileEntity;
import com.aileapro.persistence.ModelProfileModelEntity;
import com.aileapro.persistence.ModelProviderEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Turns "I need teaching" into an ordered list of things that can do it.
 *
 * <p>One rule here is worth more than the rest of the class. When a profile has any real
 * provider configured, the development fixtures are removed from its chain entirely — so a
 * production outage produces an honest failure rather than a fixture wearing a teacher's
 * voice. The fixtures serve only when nothing else is configured at all, which is the
 * situation on a laptop and in CI.
 */
@Service
@Transactional(readOnly = true)
public class ModelRouter {

    private final ModelProfileRepository profiles;
    private final ModelProfileModelRepository profileModels;
    private final ModelRepository models;
    private final ModelProviderRepository providers;
    private final ObjectMapper json;
    private final long fakeChunkDelayMillis;

    /** Clients are stateless and hold a connection pool, so they are built once per provider. */
    private final Map<String, ChatPort> clients = new ConcurrentHashMap<>();

    public ModelRouter(
            ModelProfileRepository profiles,
            ModelProfileModelRepository profileModels,
            ModelRepository models,
            ModelProviderRepository providers,
            ObjectMapper json,
            @Value("${aileapro.ai.fake-chunk-delay-ms:0}") long fakeChunkDelayMillis) {
        this.profiles = profiles;
        this.profileModels = profileModels;
        this.models = models;
        this.providers = providers;
        this.json = json;
        this.fakeChunkDelayMillis = fakeChunkDelayMillis;
    }

    /** One candidate: a client, the model it should ask for, and the params to ask with. */
    public record Route(
            ChatPort port,
            String providerSlug,
            String modelSlug,
            String wireName,
            ModelEntity model,
            double temperature,
            int maxOutputTokens) {
    }

    /**
     * The chain for a profile, best first.
     *
     * @throws IllegalStateException if the profile does not exist, which is a seed or typo bug
     */
    public List<Route> chain(String profileSlug) {
        ModelProfileEntity profile = profiles.findBySlug(profileSlug)
                .orElseThrow(() -> new IllegalStateException(
                        "No routing profile named " + profileSlug + ". The AI seed has not run, or a feature "
                                + "is asking for a profile nobody authored."));

        JsonNode params = params(profile);
        double temperature = params.path("temperature").asDouble(0.4);

        List<Route> real = new ArrayList<>();
        List<Route> fake = new ArrayList<>();
        for (ModelProfileModelEntity link : profileModels.findByIdProfileIdOrderBySort(profile.getId())) {
            ModelEntity model = models.findById(link.getId().getModelId()).orElse(null);
            if (model == null || !"enabled".equals(model.getStatus())) {
                continue;
            }
            ModelProviderEntity provider = providers.findById(model.getProviderId()).orElse(null);
            if (provider == null || !"enabled".equals(provider.getStatus())) {
                continue;
            }
            Optional<ChatPort> port = client(provider);
            if (port.isEmpty()) {
                continue;
            }
            int maxOutput = Math.min(
                    model.getMaxOutputTokens(), params.path("maxOutputTokens").asInt(model.getMaxOutputTokens()));
            Route route = new Route(
                    port.get(),
                    provider.getSlug(),
                    model.getSlug(),
                    model.getWireName(),
                    model,
                    temperature,
                    maxOutput);
            if (port.get().development()) {
                fake.add(route);
            } else {
                real.add(route);
            }
        }

        // The rule: fixtures are not a fallback. They are what runs when there is nothing else.
        return real.isEmpty() ? fake : real;
    }

    /** Whether any real model is configured anywhere. Used by the production startup check. */
    public boolean anyRealProviderConfigured() {
        return providers.findAll().stream()
                .filter(provider -> "enabled".equals(provider.getStatus()))
                .filter(provider -> !"fake".equals(provider.getApiKind()))
                .anyMatch(provider -> client(provider).isPresent());
    }

    /**
     * The client for a provider, or empty when it is not usable here.
     *
     * <p>A provider whose key is absent from the environment is not an error and not a warning
     * worth shouting about: it is a provider this deployment has not been given access to.
     */
    private Optional<ChatPort> client(ModelProviderEntity provider) {
        if ("fake".equals(provider.getApiKind())) {
            return Optional.of(clients.computeIfAbsent(
                    provider.getSlug(), slug -> new FakeChatClient(fakeChunkDelayMillis)));
        }
        String key = provider.getApiKeyEnv() == null ? null : System.getenv(provider.getApiKeyEnv());
        if (key == null || key.isBlank() || provider.getBaseUrl() == null) {
            return Optional.empty();
        }
        return Optional.of(clients.computeIfAbsent(
                provider.getSlug(),
                slug -> new HttpChatClient(slug, provider.getBaseUrl(), key, json)));
    }

    private JsonNode params(ModelProfileEntity profile) {
        try {
            return json.readTree(profile.getParams() == null ? "{}" : profile.getParams());
        } catch (Exception broken) {
            throw new IllegalStateException(
                    "Profile " + profile.getSlug() + " has unreadable params", broken);
        }
    }
}
