package de.jochenr.quarkus.integration.jms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.artemis.test.ArtemisTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTestResource(ArtemisTestResource.class) // https://quarkiverse.github.io/quarkiverse-docs/quarkus-artemis/dev/index.html#_test_framework
public class JmsRouteTest {

    @Test
    public void testHelloEndpoint() {

        RestAssured.given()
                .when().get("/say/hello/jochen")
                .then()
                .statusCode(200);
    }

}