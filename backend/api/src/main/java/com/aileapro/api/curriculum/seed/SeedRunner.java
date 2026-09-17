package com.aileapro.api.curriculum.seed;

import com.aileapro.api.assessment.seed.AssessmentSeeder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seeding is an operator action, not a UI feature and not something that happens on boot:
 * {@code gradle :api:seed} sets {@code aileapro.seed.run=true} and this runner applies the
 * authored files, then the process exits because the web server is disabled for that run.
 */
@Component
@ConditionalOnProperty(name = "aileapro.seed.run", havingValue = "true")
public class SeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

    private final CurriculumSeeder seeder;
    private final AssessmentSeeder assessmentSeeder;

    public SeedRunner(CurriculumSeeder seeder, AssessmentSeeder assessmentSeeder) {
        this.seeder = seeder;
        this.assessmentSeeder = assessmentSeeder;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Seeding curriculum from the authored files…");
        log.info("Curriculum seed complete: {}", seeder.seed());
        // Items hang off unit slugs and the misconception catalog is derived from blocks the
        // curriculum seed just wrote, so this cannot run first.
        log.info("Assessment seed complete: {}", assessmentSeeder.seed());
    }
}
