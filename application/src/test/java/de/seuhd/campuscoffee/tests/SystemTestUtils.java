package de.seuhd.campuscoffee.tests;

import de.seuhd.campuscoffee.api.dtos.PosDto;
import de.seuhd.campuscoffee.api.dtos.ReviewDto;
import de.seuhd.campuscoffee.api.dtos.UserDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utility class for system tests.
 */
public class SystemTestUtils {

    /** Client bound to the running server for the current test; set via {@link #configureClient(int)}. */
    private static RestTestClient client;

    /**
     * Binds the shared {@link RestTestClient} to the running server on the given port.
     *
     * @param port the random server port for the current test
     */
    public static void configureClient(int port) {
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    /**
     * The client bound to the running server, for tests that call endpoints outside the CRUD helpers.
     *
     * @return the configured client
     */
    public static RestTestClient client() {
        return client;
    }

    /**
     * Creates and configures a PostgreSQL testcontainer.
     *
     * @return Configured PostgreSQLContainer instance
     */
    @SuppressWarnings("resource")
    public static PostgreSQLContainer<?> getPostgresContainer() {
        return new PostgreSQLContainer<>(
                DockerImageName.parse("postgres:17-alpine"))
                .withUsername("postgres")
                .withPassword("postgres")
                .withDatabaseName("postgres")
                .withReuse(false);
    }

    /**
     * Configures Spring datasource properties to use the provided PostgreSQL testcontainer.
     *
     * @param registry          DynamicPropertyRegistry to add properties to
     * @param postgresContainer PostgreSQLContainer instance
     */
    public static void configurePostgresContainers (DynamicPropertyRegistry registry, PostgreSQLContainer<?> postgresContainer) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    /**
     * Asserts that two objects are equal, ignoring specified fields.
     *
     * @param actual         the actual object
     * @param expected       the expected object
     * @param fieldsToIgnore fields to ignore during comparison
     * @param <T>            the type of the objects being compared
     */
    public static <T> void assertEqualsIgnoringFields(T actual, T expected, String... fieldsToIgnore) {
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields(fieldsToIgnore)
                .isEqualTo(expected);
    }

    /**
     * Asserts that two objects are equal, ignoring timestamp fields (createdAt, updatedAt).
     *
     * @param actual   the actual object
     * @param expected the expected object
     * @param <T>      the type of the objects being compared
     */
    public static <T> void assertEqualsIgnoringTimestamps(T actual, T expected) {
        assertEqualsIgnoringFields(actual, expected, "createdAt", "updatedAt");
    }

    /**
     * Asserts that two objects are equal, ignoring ID and timestamp fields.
     *
     * @param actual   the actual object
     * @param expected the expected object
     * @param <T>      the type of the objects being compared
     */
    public static <T> void assertEqualsIgnoringIdAndTimestamps(T actual, T expected) {
        assertEqualsIgnoringFields(actual, expected, "id", "createdAt", "updatedAt");
    }

    /**
     * Asserts that two collections contain the same elements (in any order), ignoring specified fields.
     *
     * @param actual         the actual collection
     * @param expected       the expected collection
     * @param fieldsToIgnore fields to ignore during comparison
     * @param <T>            the type of elements in the collections
     */
    public static <T> void assertEqualsIgnoringFields(List<T> actual, List<T> expected, String... fieldsToIgnore) {
        assertThat(actual)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields(fieldsToIgnore)
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    /**
     * Asserts that two collections contain the same elements (in any order), ignoring timestamp fields for
     * each element comparison.
     *
     * @param actual   the actual collection
     * @param expected the expected collection
     * @param <T>      the type of elements in the collections
     */
    public static <T> void assertEqualsIgnoringTimestamps(List<T> actual, List<T> expected) {
        assertEqualsIgnoringFields(actual, expected, "createdAt", "updatedAt");
    }

    /**
     * Generic utility for REST API testing over {@link RestTestClient}. Provides reusable methods for
     * common CRUD operations against the server bound by {@link #configureClient(int)}.
     *
     * @param basePath  The base path of the API endpoint
     * @param dtoClass  The DTO class of the entities being tested
     * @param idGetter  Function to extract the ID from the DTO
     */
    public record Requests<T>(
            String basePath,
            Class<T> dtoClass,
            Function<T, Long> idGetter) {

        /** The DTO body of a response, after asserting the expected status. */
        private T body(RestTestClient.ResponseSpec response, HttpStatus expected) {
            EntityExchangeResult<T> result = response.returnResult(dtoClass);
            assertThat(result.getStatus().value()).isEqualTo(expected.value());
            return result.getResponseBody();
        }

        /** The raw status code of a response, without asserting it. */
        private static int status(RestTestClient.ResponseSpec response) {
            return response.returnResult(byte[].class).getStatus().value();
        }

        /** The list body of a response, deserialized via the DTO array type, after asserting 200. */
        @SuppressWarnings("unchecked")
        private List<T> list(RestTestClient.ResponseSpec response) {
            Class<T[]> arrayType = (Class<T[]>) Array.newInstance(dtoClass, 0).getClass();
            EntityExchangeResult<T[]> result = response.returnResult(arrayType);
            assertThat(result.getStatus().value()).isEqualTo(HttpStatus.OK.value());
            T[] elements = result.getResponseBody();
            return elements == null ? List.of() : List.of(elements);
        }

        /**
         * Retrieves all entities via the API.
         *
         * @return List of DTOs representing all entities
         */
        public List<T> retrieveAll() {
            return list(client.get().uri(basePath).accept(MediaType.APPLICATION_JSON).exchange());
        }

        /**
         * Retrieves an entity by its ID via the API.
         *
         * @param id ID of the entity to retrieve
         * @return DTO representing the retrieved entity
         */
        public T retrieveById(Long id) {
            return body(client.get().uri(basePath + "/{id}", id).accept(MediaType.APPLICATION_JSON).exchange(),
                    HttpStatus.OK);
        }

        /**
         * Retrieves an entity by a filter parameter via the API.
         *
         * @param filterValue Value to filter by
         * @return DTO representing the retrieved entity
         */
        public T retrieveByFilter(String filterParameter, String filterValue) {
            return body(client.get()
                    .uri(basePath + "/filter?" + filterParameter + "={value}", filterValue)
                    .accept(MediaType.APPLICATION_JSON).exchange(), HttpStatus.OK);
        }

        /**
         * Filters by a parameter and returns the raw HTTP status code instead of asserting 200. A test
         * uses this to assert a 404 when no entity matches the filter value.
         *
         * @param filterParameter the filter query parameter name
         * @param filterValue     the value to filter by
         * @return the HTTP status code of the response
         */
        public int retrieveByFilterStatusCode(String filterParameter, String filterValue) {
            return status(client.get()
                    .uri(basePath + "/filter?" + filterParameter + "={value}", filterValue)
                    .accept(MediaType.APPLICATION_JSON).exchange());
        }

        /**
         * Creates multiple entities via the API and returns their DTOs.
         *
         * @param entityList List of DTOs to create
         * @return List of DTOs representing the created entities (including their IDs)
         */
        public List<T> create(List<T> entityList) {
            return entityList.stream()
                    .map(dto -> body(client.post().uri(basePath)
                            .contentType(MediaType.APPLICATION_JSON).body(dto).exchange(), HttpStatus.CREATED))
                    .toList();
        }

        /**
         * Creates entities via the API and returns the creation status codes.
         *
         * @param entityList List of DTOs to create
         * @return List of status codes from the creation requests
         */
        public List<Integer> createAndReturnStatusCodes(List<T> entityList) {
            return entityList.stream()
                    .map(dto -> status(client.post().uri(basePath)
                            .contentType(MediaType.APPLICATION_JSON).body(dto).exchange()))
                    .toList();
        }

        /**
         * Updates multiple entities via the API and returns their updated DTOs.
         *
         * @param entityList List of DTOs to update
         * @return List of DTOs representing the updated entities
         */
        public List<T> update(List<T> entityList) {
            return entityList.stream()
                    .map(dto -> body(client.put().uri(basePath + "/{id}", idGetter.apply(dto))
                            .contentType(MediaType.APPLICATION_JSON).body(dto).exchange(), HttpStatus.OK))
                    .toList();
        }

        /**
         * Deletes multiple entities by their IDs via the API and returns the corresponding status codes.
         *
         * @param idList List of IDs of the entities to delete
         * @return List of HTTP status codes resulting from each delete operation
         */
        public List<Integer> deleteAndReturnStatusCodes(List<Long> idList) {
            return idList.stream()
                    .map(id -> status(client.delete().uri(basePath + "/{id}", id).exchange()))
                    .toList();
        }

        /**
         * Retrieves entities matching several filter query parameters via the API.
         * Unlike {@link #retrieveByFilter(String, String)}, this returns a list (the reviews
         * filter endpoint returns all matching reviews rather than a single entity).
         *
         * @param params query parameter name/value pairs to filter by
         * @return list of DTOs matching the filter criteria
         */
        public List<T> retrieveByFilter(Map<String, Object> params) {
            return list(client.get().uri(uriBuilder -> {
                uriBuilder.path(basePath + "/filter");
                params.forEach(uriBuilder::queryParam);
                return uriBuilder.build();
            }).accept(MediaType.APPLICATION_JSON).exchange());
        }

        /**
         * Retrieves an entity by its ID and returns the raw status code, so a test can assert a 404.
         *
         * @param id ID of the entity to retrieve
         * @return the HTTP status code of the response
         */
        public int retrieveByIdStatusCode(Long id) {
            return status(client.get().uri(basePath + "/{id}", id).accept(MediaType.APPLICATION_JSON).exchange());
        }

        /**
         * Updates an entity using an explicit path ID that may differ from the body ID, returning the
         * raw status code. A test uses this to assert a 400 when the path and body IDs disagree.
         *
         * @param pathId the ID to put in the request path
         * @param dto    the entity body to send
         * @return the HTTP status code of the response
         */
        public int updateWithPathIdAndReturnStatusCode(Long pathId, T dto) {
            return status(client.put().uri(basePath + "/{id}", pathId)
                    .contentType(MediaType.APPLICATION_JSON).body(dto).exchange());
        }

        /**
         * Updates multiple entities via the API and returns the resulting status codes, so a test can
         * assert a 404 when updating an entity that does not exist.
         *
         * @param entityList List of DTOs to update
         * @return List of HTTP status codes resulting from each update operation
         */
        public List<Integer> updateAndReturnStatusCodes(List<T> entityList) {
            return entityList.stream()
                    .map(dto -> status(client.put().uri(basePath + "/{id}", idGetter.apply(dto))
                            .contentType(MediaType.APPLICATION_JSON).body(dto).exchange()))
                    .toList();
        }

        /**
         * Approves an entity on behalf of a user via {@code PUT /{id}/approve?user_id=...} (reviews).
         *
         * @param id     ID of the entity to approve
         * @param userId ID of the user performing the approval
         * @return the updated DTO
         */
        public T approve(Long id, Long userId) {
            return body(client.put().uri(basePath + "/{id}/approve?user_id={userId}", id, userId).exchange(),
                    HttpStatus.OK);
        }

        /**
         * Approves an entity and returns the raw status code, so a test can assert a 400 when a user
         * approves their own review or a 404 when the review does not exist.
         *
         * @param id     ID of the entity to approve
         * @param userId ID of the user performing the approval
         * @return the HTTP status code of the response
         */
        public int approveAndReturnStatusCode(Long id, Long userId) {
            return status(client.put().uri(basePath + "/{id}/approve?user_id={userId}", id, userId).exchange());
        }

        public static SystemTestUtils.Requests<PosDto> posRequests = new SystemTestUtils.Requests<>(
                "/api/pos",
                PosDto.class,
                PosDto::getId
        );

        public static SystemTestUtils.Requests<UserDto> userRequests = new SystemTestUtils.Requests<>(
                "/api/users",
                UserDto.class,
                UserDto::getId
        );

        public static SystemTestUtils.Requests<ReviewDto> reviewRequests = new SystemTestUtils.Requests<>(
                "/api/reviews",
                ReviewDto.class,
                ReviewDto::getId
        );
    }
}
