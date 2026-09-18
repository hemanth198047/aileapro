package com.aileapro.lab;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FakeExecutorClientTest {

    @Test
    void demoSnippetPrintsTheList() {
        FakeExecutorClient client = new FakeExecutorClient();
        var result = client.run(new ExecutorPort.RunRequest(
                "python",
                "a = [1, 2, 3]\nb = a\nprint(a)\n",
                null,
                1000,
                128));
        assertThat(result.status()).isEqualTo("succeeded");
        assertThat(result.stdout()).contains("[1, 2, 3]");
        assertThat(result.stdout()).contains("Development implementation");
    }

    @Test
    void timeoutMarkerTimesOut() {
        FakeExecutorClient client = new FakeExecutorClient(5);
        var result = client.run(new ExecutorPort.RunRequest(
                "python",
                "print(1)\n# " + FakeExecutorClient.TIMEOUT_MARKER + "\n",
                null,
                50,
                128));
        assertThat(result.timedOut()).isTrue();
        assertThat(result.status()).isEqualTo("timed_out");
        assertThat(result.errorCode()).isEqualTo("sandbox_timeout");
    }

    @Test
    void projectMilestoneFixturesPassAndFailHonestly() {
        FakeExecutorClient client = new FakeExecutorClient();
        var pass = client.run(new ExecutorPort.RunRequest(
                "python",
                "# aileapro_check:mse\ndef mse(y_true, y_pred):\n    return 0.375\n",
                null,
                1000,
                128));
        assertThat(pass.stdout()).contains("AILEAPRO_PASS");

        var fail = client.run(new ExecutorPort.RunRequest(
                "python",
                "# aileapro_check:mse\ndef other():\n    return 1\n",
                null,
                1000,
                128));
        assertThat(fail.stdout()).contains("AILEAPRO_FAIL");
    }
}
