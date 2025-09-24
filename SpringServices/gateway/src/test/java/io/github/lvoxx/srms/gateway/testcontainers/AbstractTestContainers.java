package io.github.lvoxx.srms.gateway.testcontainers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SuppressWarnings({ "resource", "unused" })
public class AbstractTestContainers {

        private static final Logger log = LoggerFactory.getLogger(AbstractTestContainers.class);

        @Container
        protected static final GenericContainer<?> redis = new GenericContainer<>(
                        ETestContainersVersion.REDIS.getVersion())
                        .withExposedPorts(6379)
                        .waitingFor(Wait.forListeningPort());

        @Container
        protected static final MockServerContainer customerServiceMock = new MockServerContainer(
                        DockerImageName.parse(ETestContainersVersion.WIREMOCK_SERVER.getVersion()));

        @Container
        protected static final MockServerContainer contactServiceMock = new MockServerContainer(
                        DockerImageName.parse(ETestContainersVersion.WIREMOCK_SERVER.getVersion()));

        @DynamicPropertySource
        static void registerProperties(DynamicPropertyRegistry registry) {
                registry.add("redisHost", redis::getHost);
                registry.add("redisPort", () -> redis.getMappedPort(6379).toString());
                registry.add("customerServiceUri",
                                () -> "http://" + customerServiceMock.getHost() + ":"
                                                + customerServiceMock.getServerPort());
                registry.add("contactServiceUri",
                                () -> "http://" + contactServiceMock.getHost() + ":"
                                                + contactServiceMock.getServerPort());
        }

        @BeforeAll
        static void setup() {
                redis.start();
        }

        @AfterAll
        static void tearDown() {
                redis.stop();
        }

}
