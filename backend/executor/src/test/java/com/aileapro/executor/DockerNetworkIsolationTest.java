package com.aileapro.executor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 15 gate: lab containers always get {@code --network none}, which blocks
 * link-local metadata (169.254.169.254) without needing a live Docker daemon in CI.
 */
class DockerNetworkIsolationTest {

    @Test
    void dockerArgsAlwaysDisableNetwork() {
        ExecutorProperties props = new ExecutorProperties();
        DockerPythonRunner runner = new DockerPythonRunner(props);
        List<String> args = runner.dockerRunArgs("test-container");
        int network = args.indexOf("--network");
        assertThat(network).isGreaterThanOrEqualTo(0);
        assertThat(args.get(network + 1)).isEqualTo("none");
        assertThat(args).doesNotContain("169.254.169.254");
    }
}
