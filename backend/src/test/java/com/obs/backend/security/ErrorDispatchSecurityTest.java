package com.obs.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * MockMvc doesn't dispatch through a real servlet container, so it never exercises Spring's
 * internal forward to /error when @Valid rejects a request — it can't catch a regression here.
 * This uses a real HTTP round-trip (random port, plain JDK HttpClient) specifically to prove
 * that forward isn't itself blocked by authorizeHttpRequests().anyRequest().authenticated(),
 * which previously turned every validation failure into an opaque 403 with no body.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ErrorDispatchSecurityTest {

    @LocalServerPort private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void aValidationFailureIsReportedAsBadRequestNotAnOpaqueForbidden() throws Exception {
        String invalidBody =
                """
                {
                  "lastName": "Doe",
                  "password": "x",
                  "nidNumber": "",
                  "nidExpiryDate": "2020-01-01",
                  "dateOfBirth": "1990-01-01",
                  "gender": "FEMALE",
                  "phone": "+855-12-345-678"
                }
                """;

        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).isNotBlank();
    }
}
