package com.aileapro.api.ai;

import com.aileapro.ai.PromptTemplate;
import com.aileapro.persistence.PromptEntity;
import com.aileapro.persistence.PromptVersionEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Where features get their prompts.
 *
 * <p>The point of the indirection is traceability rather than tidiness. A feature asks for
 * {@code tutor.socratic} and gets back the current version <em>with its id</em>, which is then
 * written onto the message and onto the usage row. Six weeks later, "the tutor started giving
 * away answers" is a query rather than an argument.
 */
@Service
@Transactional(readOnly = true)
public class PromptCatalog {

    private final PromptRepository prompts;
    private final PromptVersionRepository versions;

    public PromptCatalog(PromptRepository prompts, PromptVersionRepository versions) {
        this.prompts = prompts;
        this.versions = versions;
    }

    /**
     * One rendered prompt, and the version that produced it.
     *
     * @param versionId what to record on the message and the usage row
     */
    public record Rendered(UUID versionId, int version, String text) {
    }

    /**
     * Renders the current version of a prompt.
     *
     * @throws IllegalStateException if the prompt is missing, which means the seed did not run
     *                               or the name is a typo — both of which are bugs, not states
     */
    public Rendered render(String name, Map<String, String> slots) {
        PromptVersionEntity version = latest(name);
        return new Rendered(
                version.getId(), version.getVersion(), PromptTemplate.render(version.getTemplate(), slots));
    }

    public PromptVersionEntity latest(String name) {
        PromptEntity prompt = prompts.findByName(name)
                .orElseThrow(() -> new IllegalStateException(
                        "No prompt named " + name + ". The AI seed has not run, or the name is wrong."));
        List<PromptVersionEntity> all = versions.findByPromptIdOrderByVersionDesc(prompt.getId());
        if (all.isEmpty()) {
            throw new IllegalStateException("Prompt " + name + " exists with no versions");
        }
        return all.get(0);
    }
}
