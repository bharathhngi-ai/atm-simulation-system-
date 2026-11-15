package com.bank.atm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AtmSimulationApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Spring context bootstraps successfully with test profile
    }
}
