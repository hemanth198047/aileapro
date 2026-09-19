package com.aileapro.api.intelligence;

import com.aileapro.api.curriculum.SourceRepository;
import com.aileapro.persistence.SourceEntity;
import com.aileapro.persistence.SourceLinkCheckEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * HEAD/GET check of {@code sources.url}. Updates last_verified_at / http_status and appends
 * source_link_checks. Disabled in tests unless explicitly enabled.
 */
@Service
public class LinkCheckService {

    private final SourceRepository sources;
    private final SourceLinkCheckRepository checks;
    private final boolean enabled;
    private final HttpClient http;

    public LinkCheckService(
            SourceRepository sources,
            SourceLinkCheckRepository checks,
            @Value("${aileapro.link-check.enabled:true}") boolean enabled) {
        this.sources = sources;
        this.checks = checks;
        this.enabled = enabled;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    @Scheduled(cron = "${aileapro.link-check.cron:0 15 4 * * *}")
    public void scheduled() {
        if (!enabled) {
            return;
        }
        checkAll(40);
    }

    @Transactional
    public IntelligenceViews.LinkCheckReport checkAll(int limit) {
        if (!enabled) {
            return new IntelligenceViews.LinkCheckReport(
                    0, 0, 0, List.of(), "Link check disabled (aileapro.link-check.enabled=false).");
        }
        List<SourceEntity> all = sources.findAll();
        int cap = Math.max(1, Math.min(limit, all.size()));
        List<IntelligenceViews.LinkCheckRow> rows = new ArrayList<>();
        int ok = 0;
        int failed = 0;
        for (int i = 0; i < cap; i++) {
            SourceEntity source = all.get(i);
            IntelligenceViews.LinkCheckRow row = checkOne(source);
            rows.add(row);
            if (row.ok()) {
                ok++;
            } else {
                failed++;
            }
        }
        return new IntelligenceViews.LinkCheckReport(
                rows.size(),
                ok,
                failed,
                rows,
                "Checked up to " + cap + " sources. Failures are recorded; they do not invent replacements.");
    }

    private IntelligenceViews.LinkCheckRow checkOne(SourceEntity source) {
        Instant now = Instant.now();
        long start = System.nanoTime();
        Integer status = null;
        boolean ok = false;
        String error = null;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(source.getUrl()))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
            status = response.statusCode();
            if (status >= 400 || status == 405) {
                HttpRequest get = HttpRequest.newBuilder(URI.create(source.getUrl()))
                        .GET()
                        .timeout(Duration.ofSeconds(5))
                        .build();
                HttpResponse<Void> getResponse = http.send(get, HttpResponse.BodyHandlers.discarding());
                status = getResponse.statusCode();
            }
            ok = status != null && status >= 200 && status < 400;
            if (!ok) {
                error = "HTTP " + status;
            }
        } catch (Exception e) {
            error = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        int durationMs = (int) ((System.nanoTime() - start) / 1_000_000L);
        SourceLinkCheckEntity check = new SourceLinkCheckEntity();
        check.setId(UUID.randomUUID());
        check.setSourceId(source.getId());
        check.setCheckedAt(now);
        check.setHttpStatus(status);
        check.setOk(ok);
        check.setError(error);
        check.setDurationMs(durationMs);
        checks.save(check);
        source.setLastVerifiedAt(now);
        source.setHttpStatus(status);
        source.setUpdatedAt(now);
        sources.save(source);
        return new IntelligenceViews.LinkCheckRow(source.getSlug(), ok, status, error, durationMs);
    }
}
