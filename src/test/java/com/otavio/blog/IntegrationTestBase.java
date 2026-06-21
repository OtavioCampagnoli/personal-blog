package com.otavio.blog;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests.
 * Uses H2 in-memory DB and test profile.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {
    // TODO: Add shared test utilities here (e.g., test data builders, JWT token generator)
}
