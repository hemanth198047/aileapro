package com.aileapro.api.project;

import com.aileapro.lab.FakeExecutorClient;

/**
 * Builds the sandbox harness that wraps learner source for a milestone check.
 *
 * <p>The real Docker executor runs the asserts. The fake executor only pattern-matches the
 * {@code aileapro_check:*} marker plus the required definitions — same honesty rule as lab
 * fixtures: missing code fails, and a canned pass is labelled development.
 */
public final class ProjectCheck {

    public static final String PASS_TOKEN = "AILEAPRO_PASS";
    public static final String FAIL_TOKEN = "AILEAPRO_FAIL";

    private ProjectCheck() {
    }

    public static String harness(String checkKey, String learnerSource) {
        String key = checkKey == null ? "" : checkKey.trim().toLowerCase();
        String body = learnerSource == null ? "" : learnerSource;
        return switch (key) {
            case "mse" -> """
                    # aileapro_check:mse
                    %s

                    _y_true = [3.0, -0.5, 2.0, 7.0]
                    _y_pred = [2.5, 0.0, 2.0, 8.0]
                    _ok = abs(mse(_y_true, _y_pred) - 0.375) < 1e-6
                    print("%s" if _ok else "%s")
                    """.formatted(body, PASS_TOKEN, FAIL_TOKEN);
            case "fit_line" -> """
                    # aileapro_check:fit_line
                    %s

                    _xs = [0.0, 1.0, 2.0, 3.0]
                    _ys = [1.0, 3.0, 5.0, 7.0]
                    _slope, _intercept = fit(_xs, _ys)
                    _ok = abs(_slope - 2.0) < 1e-6 and abs(_intercept - 1.0) < 1e-6
                    print("%s" if _ok else "%s")
                    """.formatted(body, PASS_TOKEN, FAIL_TOKEN);
            case "predict_report" -> """
                    # aileapro_check:predict_report
                    %s

                    _xs = [0.0, 1.0, 2.0, 3.0]
                    _ys = [1.0, 3.0, 5.0, 7.0]
                    _slope, _intercept = fit(_xs, _ys)
                    _yhat = [predict(x, _slope, _intercept) for x in _xs]
                    _ok = all(abs(a - b) < 1e-6 for a, b in zip(_yhat, _ys))
                    print("%s" if _ok else "%s")
                    print("slope=", _slope, "intercept=", _intercept)
                    """.formatted(body, PASS_TOKEN, FAIL_TOKEN);
            default -> throw new IllegalArgumentException("Unknown project check key: " + checkKey);
        };
    }

    public static boolean passed(String stdout) {
        if (stdout == null) {
            return false;
        }
        return stdout.contains(PASS_TOKEN) && !stdout.contains(FAIL_TOKEN);
    }

    /** Mentorship copy: hints and criteria, never a dumped solution. */
    public static String mentorComment(boolean passed, String hint) {
        if (passed) {
            return "The automated check passed. Good — keep the next milestone honest the same way.";
        }
        String base = "The automated check did not pass. Re-read the criteria and adjust your functions;";
        if (hint != null && !hint.isBlank()) {
            return base + " hint: " + hint.trim();
        }
        return base + " I will not dump a full solution.";
    }

    public static String sha256(String source) {
        return FakeExecutorClient.sha256(source);
    }
}
