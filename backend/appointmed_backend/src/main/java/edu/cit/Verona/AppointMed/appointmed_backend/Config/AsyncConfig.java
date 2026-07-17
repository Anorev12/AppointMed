package edu.cit.Verona.AppointMed.appointmed_backend.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
 
import java.util.concurrent.Executor;
 
/**
 * Dedicated thread pool for background work — currently just outbound
 * notification emails (see NotificationService's @Async methods).
 *
 * Without this, @Async falls back to Spring's SimpleAsyncTaskExecutor,
 * which spins up a brand-new thread per call with no limit. A named pool
 * here keeps that bounded and makes it easy to tune/monitor later.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
 
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notif-async-");
        executor.initialize();
        return executor;
    }
}
 