package com.binava.stafffinance.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.url=jdbc:h2:mem:staff_finance_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class StaffFinanceApplicationTests {
    @Test
    void contextLoads() {
    }
}
