package com.aileapro.api.ai.seed;

import com.aileapro.ai.PromptTemplate;
import com.aileapro.api.ai.ModelProfileModelRepository;
import com.aileapro.api.ai.ModelProfileRepository;
import com.aileapro.api.ai.ModelProviderRepository;
import com.aileapro.api.ai.ModelRepository;
import com.aileapro.api.ai.PromptRepository;
import com.aileapro.api.ai.PromptVersionRepository;
import com.aileapro.api.ai.seed.AiSeedModel.Catalogue;
import com.aileapro.api.ai.seed.AiSeedModel.PromptFile;
import com.aileapro.api.ai.seed.AiSeedModel.SeedModel;
import com.aileapro.api.ai.seed.AiSeedModel.SeedProfile;
import com.aileapro.api.ai.seed.AiSeedModel.SeedPrompt;
import com.aileapro.api.ai.seed.AiSeedModel.SeedPromptVersion;
import com.aileapro.api.ai.seed.AiSeedModel.SeedProvider;
import com.aileapro.persistence.ModelEntity;
import com.aileapro.persistence.ModelProfileEntity;
import com.aileapro.persistence.ModelProfileModelEntity;
import com.aileapro.persistence.ModelProviderEntity;
import com.aileapro.persistence.PromptEntity;
import com.aileapro.persistence.PromptVersionEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Loads the model catalogue and the prompt catalogue.
 *
 * <p>Idempotent by slug and by name, like the other seeders, with one rule that is not
 * cosmetic: the text of an existing prompt version is never updated. If a template in the file
 * no longer hashes to what the database holds, the seed fails and names the prompt, because
 * the alternative is that every {@code ai_usage} row pointing at version 1 silently starts
 * referring to text that was never used to produce it.
 */
@Component
public class AiSeeder {

    private static final Logger log = LoggerFactory.getLogger(AiSeeder.class);
    private static final String CATALOGUE = "ai/catalogue.yml";
    private static final String PROMPTS = "prompts/prompts.yml";

    private final ModelProviderRepository providers;
    private final ModelRepository models;
    private final ModelProfileRepository profiles;
    private final ModelProfileModelRepository profileModels;
    private final PromptRepository prompts;
    private final PromptVersionRepository promptVersions;
    private final ObjectMapper yaml;

    public AiSeeder(
            ModelProviderRepository providers,
            ModelRepository models,
            ModelProfileRepository profiles,
            ModelProfileModelRepository profileModels,
            PromptRepository prompts,
            PromptVersionRepository promptVersions) {
        this.providers = providers;
        this.models = models;
        this.profiles = profiles;
        this.profileModels = profileModels;
        this.prompts = prompts;
        this.promptVersions = promptVersions;
        this.yaml = JsonMapper.builder(new YAMLFactory()).build();
    }

    public record SeedReport(int providers, int models, int profiles, int prompts, int promptVersions) {
        @Override
        public String toString() {
            return "providers=%d models=%d profiles=%d prompts=%d promptVersions=%d"
                    .formatted(providers, models, profiles, prompts, promptVersions);
        }
    }

    @Transactional
    public SeedReport seed() {
        Catalogue catalogue = read(CATALOGUE, Catalogue.class);
        PromptFile promptFile = read(PROMPTS, PromptFile.class);

        for (SeedProvider provider : nullSafe(catalogue.providers())) {
            upsertProvider(provider);
        }
        for (SeedModel model : nullSafe(catalogue.models())) {
            upsertModel(model);
        }
        for (SeedProfile profile : nullSafe(catalogue.profiles())) {
            upsertProfile(profile);
        }

        int versionCount = 0;
        for (SeedPrompt prompt : nullSafe(promptFile.prompts())) {
            versionCount += upsertPrompt(prompt);
        }

        SeedReport report = new SeedReport(
                nullSafe(catalogue.providers()).size(),
                nullSafe(catalogue.models()).size(),
                nullSafe(catalogue.profiles()).size(),
                nullSafe(promptFile.prompts()).size(),
                versionCount);
        log.info("AI catalogue seeded: {}", report);
        return report;
    }

    private void upsertProvider(SeedProvider seed) {
        ModelProviderEntity row = providers.findBySlug(seed.slug()).orElseGet(() -> {
            ModelProviderEntity fresh = new ModelProviderEntity();
            fresh.setId(UUID.randomUUID());
            fresh.setSlug(seed.slug());
            return fresh;
        });
        row.setTitle(seed.title());
        row.setApiKind(seed.apiKind() == null ? "openai_compatible" : seed.apiKind());
        row.setBaseUrl(seed.baseUrl());
        row.setApiKeyEnv(seed.apiKeyEnv());
        providers.save(row);
    }

    private void upsertModel(SeedModel seed) {
        ModelProviderEntity provider = providers.findBySlug(seed.provider())
                .orElseThrow(() -> new IllegalStateException(
                        "Model " + seed.slug() + " names provider " + seed.provider() + ", which is not in "
                                + CATALOGUE));

        ModelEntity row = models.findBySlug(seed.slug()).orElseGet(() -> {
            ModelEntity fresh = new ModelEntity();
            fresh.setId(UUID.randomUUID());
            fresh.setSlug(seed.slug());
            return fresh;
        });
        row.setProviderId(provider.getId());
        row.setWireName(seed.wireName() == null ? seed.slug() : seed.wireName());
        if (seed.capabilities() != null && !seed.capabilities().isEmpty()) {
            row.setCapabilities(seed.capabilities().toArray(String[]::new));
        }
        if (seed.contextTokens() != null) {
            row.setContextTokens(seed.contextTokens());
        }
        if (seed.maxOutputTokens() != null) {
            row.setMaxOutputTokens(seed.maxOutputTokens());
        }
        row.setPriceInPerMtok(seed.priceInPerMtok() == null ? BigDecimal.ZERO : seed.priceInPerMtok());
        row.setPriceOutPerMtok(seed.priceOutPerMtok() == null ? BigDecimal.ZERO : seed.priceOutPerMtok());
        models.save(row);
    }

    private void upsertProfile(SeedProfile seed) {
        ModelProfileEntity row = profiles.findBySlug(seed.slug()).orElseGet(() -> {
            ModelProfileEntity fresh = new ModelProfileEntity();
            fresh.setId(UUID.randomUUID());
            fresh.setSlug(seed.slug());
            return fresh;
        });
        row.setTitle(seed.title());
        row.setPurpose(seed.purpose() == null ? seed.title() : seed.purpose().trim());
        row.setParams(writeJson(seed.params()));
        profiles.save(row);

        // The chain is replaced wholesale: the file is the source of truth for which models
        // serve a profile and in what order, and a leftover row would quietly outrank the file.
        profileModels.deleteByIdProfileId(row.getId());
        profileModels.flush();
        List<ModelProfileModelEntity> chain = new ArrayList<>();
        int sort = 0;
        for (String modelSlug : nullSafe(seed.models())) {
            ModelEntity model = models.findBySlug(modelSlug)
                    .orElseThrow(() -> new IllegalStateException(
                            "Profile " + seed.slug() + " names model " + modelSlug + ", which is not in "
                                    + CATALOGUE));
            chain.add(new ModelProfileModelEntity(row.getId(), model.getId(), sort++));
        }
        if (chain.isEmpty()) {
            throw new IllegalStateException("Profile " + seed.slug() + " lists no models, so nothing can serve it");
        }
        profileModels.saveAll(chain);
    }

    /** @return how many versions this prompt now has */
    private int upsertPrompt(SeedPrompt seed) {
        PromptEntity row = prompts.findByName(seed.name()).orElseGet(() -> {
            PromptEntity fresh = new PromptEntity();
            fresh.setId(UUID.randomUUID());
            fresh.setName(seed.name());
            return fresh;
        });
        row.setPurpose(seed.purpose() == null ? seed.name() : seed.purpose().trim());
        if (seed.owner() != null) {
            row.setOwner(seed.owner());
        }
        prompts.save(row);

        int count = 0;
        for (SeedPromptVersion version : nullSafe(seed.versions())) {
            if (version.version() == null || version.template() == null || version.template().isBlank()) {
                throw new IllegalStateException("Prompt " + seed.name() + " has a version with no number or text");
            }
            String template = version.template().strip();
            String hash = PromptTemplate.hash(template);

            PromptVersionEntity existing =
                    promptVersions.findByPromptIdAndVersion(row.getId(), version.version()).orElse(null);
            if (existing == null) {
                PromptVersionEntity fresh = new PromptVersionEntity();
                fresh.setId(UUID.randomUUID());
                fresh.setPromptId(row.getId());
                fresh.setVersion(version.version());
                fresh.setTemplate(template);
                fresh.setHash(hash);
                fresh.setNotes(version.notes());
                promptVersions.save(fresh);
            } else if (!existing.getHash().equals(hash)) {
                throw new IllegalStateException(
                        "Prompt " + seed.name() + " version " + version.version() + " has been edited in "
                                + PROMPTS + ". Add a new version instead: every ai_usage row that cites this "
                                + "one was produced by the old text, and editing it in place makes those rows "
                                + "point at something that never ran.");
            } else {
                existing.setNotes(version.notes());
                promptVersions.save(existing);
            }
            count++;
        }
        if (count == 0) {
            throw new IllegalStateException("Prompt " + seed.name() + " has no versions");
        }
        return count;
    }

    private <T> T read(String location, Class<T> type) {
        ClassPathResource resource = new ClassPathResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException("Missing " + location + " on the classpath");
        }
        try (InputStream in = resource.getInputStream()) {
            return yaml.readValue(in, type);
        } catch (IOException broken) {
            throw new UncheckedIOException("Could not read " + location, broken);
        }
    }

    private String writeJson(Map<String, Object> params) {
        try {
            return new ObjectMapper().writeValueAsString(params == null ? Map.of() : params);
        } catch (com.fasterxml.jackson.core.JsonProcessingException impossible) {
            throw new IllegalStateException("Could not serialise profile params", impossible);
        }
    }

    private static <T> List<T> nullSafe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
