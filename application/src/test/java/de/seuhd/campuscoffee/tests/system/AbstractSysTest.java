package de.seuhd.campuscoffee.tests.system;

import de.seuhd.campuscoffee.api.mapper.PosDtoMapper;
import de.seuhd.campuscoffee.api.mapper.ReviewDtoMapper;
import de.seuhd.campuscoffee.api.mapper.UserDtoMapper;
import de.seuhd.campuscoffee.domain.ports.api.PosService;
import de.seuhd.campuscoffee.domain.ports.api.ReviewService;
import de.seuhd.campuscoffee.domain.ports.api.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static de.seuhd.campuscoffee.tests.SystemTestUtils.configureClient;
import static de.seuhd.campuscoffee.tests.SystemTestUtils.configurePostgresContainers;
import static de.seuhd.campuscoffee.tests.SystemTestUtils.getPostgresContainer;

/**
 * Abstract base class for system tests.
 * Sets up the Spring Boot test context, manages the PostgreSQL testcontainer, and configures REST Assured.
 */
@SpringBootTest(
        classes = de.seuhd.campuscoffee.Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public abstract class AbstractSysTest {
    protected static final PostgreSQLContainer<?> postgresContainer;

    static {
        // share the same testcontainers instance across all system tests
        postgresContainer = getPostgresContainer();
        postgresContainer.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        configurePostgresContainers(registry, postgresContainer);
    }

    @Autowired
    protected PosService posService;

    @Autowired
    protected UserService userService;

    @Autowired
    protected ReviewService reviewService;

    @Autowired
    protected PosDtoMapper posDtoMapper;

    @Autowired
    protected UserDtoMapper userDtoMapper;

    @Autowired
    protected ReviewDtoMapper reviewDtoMapper;

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void beforeEach() {
        // reviews reference POS and users via foreign keys, so they must be cleared first
        reviewService.clear();
        posService.clear();
        userService.clear();
        configureClient(port);
    }

    @AfterEach
    void afterEach() {
        reviewService.clear();
        posService.clear();
        userService.clear();
    }
}