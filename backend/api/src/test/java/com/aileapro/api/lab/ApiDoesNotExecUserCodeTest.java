package com.aileapro.api.lab;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Documented proof that the API module never spawns a process for learner source.
 *
 * <p>The executor module is allowed to use {@code ProcessBuilder} for Docker. The API must
 * talk to it over HTTP (or use the fake client). A grep of the API sources is the gate the
 * master plan asks for — if this fails, user code is one bad merge from running in the wrong JVM.
 */
class ApiDoesNotExecUserCodeTest {

    @Test
    void apiSourcesNeverMentionProcessBuilderOrRuntimeExec() throws IOException {
        Path apiMain = Path.of("src/main/java/com/aileapro/api");
        assertThat(apiMain).exists();

        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(apiMain)) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    String text = Files.readString(path, StandardCharsets.UTF_8);
                    if (text.contains("ProcessBuilder")
                            || text.contains("Runtime.getRuntime().exec")
                            || text.contains("Runtime.exec")) {
                        offenders.add(path.toString());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        assertThat(offenders)
                .as("API must not spawn processes for learner source; use ExecutorPort instead")
                .isEmpty();
    }
}
