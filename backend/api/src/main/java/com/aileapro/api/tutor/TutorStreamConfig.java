package com.aileapro.api.tutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The threads tutor streams run on.
 *
 * <p>Virtual threads, because a streaming turn is almost entirely spent blocked on a socket.
 * With platform threads this would need a pool sized for the number of learners who might be
 * mid-question at once, and that number would be wrong: too small and a busy minute queues
 * learners behind each other, too large and idle threads hold memory for nothing. A virtual
 * thread per turn costs almost nothing while it waits.
 *
 * <p>Not conditional on being a web application, even though only the controller uses it. The
 * operator CLI runs (<code>gradle :api:seed</code>) start the same context with the web server
 * off, controllers are still beans there, and a controller whose dependency vanishes outside a
 * servlet stack turns a seed into a wiring failure.
 */
@Configuration
public class TutorStreamConfig {

    @Bean(destroyMethod = "shutdown")
    ExecutorService tutorStreamExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
