package com.binava.stafffinance;

import com.binava.stafffinance.config.DemoDataSeeder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableJpaAuditing
public class StaffFinanceApplication {
    public static void main(String[] args) {
        SpringApplication.run(StaffFinanceApplication.class, args);
    }

    @Bean
    public CommandLineRunner seedDemoData(DemoDataSeeder seeder, com.binava.stafffinance.loan.service.LoanScheduleUpgradeService schedules) {
        return args -> { seeder.seed(); schedules.upgrade(); };
    }
}
