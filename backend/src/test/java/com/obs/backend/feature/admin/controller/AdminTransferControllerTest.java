package com.obs.backend.feature.admin.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
import static com.obs.backend.feature.admin.AdminAuthTestSupport.adminLoginAndGetAccessToken;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.obs.backend.feature.auth.RecordingOtpSenderConfig;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig.RecordingOtpSender;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class AdminTransferControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;
    @PersistenceContext private EntityManager entityManager;

    @Test
    void adminCanListAndFilterTransfers() throws Exception {
        String customerToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-30-000-001", "correct-horse");
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);

        String acc1 = openAccount(customerToken, "SAVINGS", "USD");
        String acc2 = openAccount(customerToken, "CHECKING", "USD");
        fund(acc1, "1000.00");

        // Make a transfer
        mockMvc.perform(post("/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountId\": \"%s\", \"toAccountId\": \"%s\", \"amount\": 150.00, \"description\": \"Dinner\"}".formatted(acc1, acc2)))
                .andExpect(status().isCreated());

        // Make an external transfer
        mockMvc.perform(post("/transfers/external")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountId\": \"%s\", \"beneficiaryBankCode\": \"BANKKHPP\", \"beneficiaryAccountNumber\": \"1234567890\", \"currency\": \"USD\", \"amount\": 50.00, \"description\": \"Gift\"}".formatted(acc1)))
                .andExpect(status().isCreated());

        // Scoped to this test's own source account. The feed is system-wide, so an unscoped
        // assertion here passes only while the transfers table happens to be empty — running
        // seed_demo_data.sql against the same database is enough to break it.
        mockMvc.perform(get("/admin/transfers")
                        .param("account", acc1)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].fromAccountNumber").exists())
                .andExpect(jsonPath("$.totalElements").value(2));

        // Filter by minAmount
        mockMvc.perform(get("/admin/transfers")
                        .param("minAmount", "100.00")
                        .param("account", acc1)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].amount").value(150.0));

        // Filter by maxAmount
        mockMvc.perform(get("/admin/transfers")
                        .param("maxAmount", "80.00")
                        .param("account", acc1)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].amount").value(50.0));

        // Filter by status
        mockMvc.perform(get("/admin/transfers")
                        .param("status", "COMPLETED")
                        .param("account", acc1)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        // Filter by currency
        mockMvc.perform(get("/admin/transfers")
                        .param("currency", "KHR")
                        .param("account", acc1)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        // Filter by account ID
        mockMvc.perform(get("/admin/transfers")
                        .param("account", acc2)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].amount").value(150.0));
    }

    /**
     * The date range is the filter the monitor screen leads with and the only one the original test
     * never covered. Bounds are inclusive at both ends, and both are optional.
     */
    @Test
    void theDateRangeFilterIsInclusiveAndExcludesEverythingOutsideIt() throws Exception {
        Fixture fixture = threeTransfersOnKnownDates();

        // Only the middle transfer, by a range that starts and ends on its own date.
        listScoped(fixture, "startDate", "2026-08-10", "endDate", "2026-08-10")
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].amount").value(50.0));

        // An open-ended lower bound keeps the two most recent.
        listScoped(fixture, "startDate", "2026-08-10")
                .andExpect(jsonPath("$.content", hasSize(2)));

        // An open-ended upper bound keeps the two oldest.
        listScoped(fixture, "endDate", "2026-08-10")
                .andExpect(jsonPath("$.content", hasSize(2)));

        // A range before any of them keeps nothing.
        listScoped(fixture, "startDate", "2026-01-01", "endDate", "2026-01-31")
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    /** The screen sends one free-text box, so `account` has to take either form. */
    @Test
    void theAccountFilterTakesEitherAnAccountIdOrAnAccountNumber() throws Exception {
        Fixture fixture = threeTransfersOnKnownDates();

        listAs(fixture.adminToken(), "account", fixture.destinationId())
                .andExpect(jsonPath("$.content", hasSize(3)));

        listAs(fixture.adminToken(), "account", fixture.destinationNumber())
                .andExpect(jsonPath("$.content", hasSize(3)));

        listAs(fixture.adminToken(), "account", UUID.randomUUID().toString())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    /**
     * The FAILED rows the QR decline path writes (US-034) are the reason the status filter matters:
     * the feed is where a declined payment is visible at all.
     */
    @Test
    void theStatusFilterSeparatesFailedTransfersFromCompletedOnes() throws Exception {
        Fixture fixture = threeTransfersOnKnownDates();
        setStatusOfOldest("FAILED", fixture.destinationId());

        listScoped(fixture, "status", "FAILED")
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("FAILED"));

        listScoped(fixture, "status", "COMPLETED")
                .andExpect(jsonPath("$.content", hasSize(2)));

        // Unfiltered still shows both, so a decline is never silently hidden from the feed.
        listScoped(fixture).andExpect(jsonPath("$.content", hasSize(3)));
    }

    /** Newest first, and paged — the screen is a long-lived list over a growing table. */
    @Test
    void theFeedIsNewestFirstAndPaged() throws Exception {
        Fixture fixture = threeTransfersOnKnownDates();

        listScoped(fixture)
                .andExpect(jsonPath("$.content[0].amount").value(75.0))
                .andExpect(jsonPath("$.content[2].amount").value(25.0));

        listScoped(fixture, "page", "0", "size", "2")
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3));

        listScoped(fixture, "page", "1", "size", "2")
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].amount").value(25.0));
    }

    /** Three transfers dated 2026-08-05, 2026-08-10 and 2026-08-15, oldest first at 25/50/75. */
    private Fixture threeTransfersOnKnownDates() throws Exception {
        String customerToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-30-000-002", "correct-horse");
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);

        String source = openAccount(customerToken, "SAVINGS", "USD");
        String destination = openAccount(customerToken, "CHECKING", "USD");
        fund(source, "1000.00");

        transfer(customerToken, source, destination, "25.00");
        transfer(customerToken, source, destination, "50.00");
        transfer(customerToken, source, destination, "75.00");

        // Backdated directly: the endpoint has no way to set created_at, and the filter under test
        // is meaningless while every row shares one timestamp.
        backdateByAmount("25.00", "2026-08-05", destination);
        backdateByAmount("50.00", "2026-08-10", destination);
        backdateByAmount("75.00", "2026-08-15", destination);

        return new Fixture(adminToken, destination, accountNumberOf(destination));
    }

    private record Fixture(String adminToken, String destinationId, String destinationNumber) {}

    /** Scoped to the fixture's own accounts, for the same reason the test above is. */
    private ResultActions listScoped(Fixture fixture, String... params) throws Exception {
        String[] scoped = new String[params.length + 2];
        System.arraycopy(params, 0, scoped, 0, params.length);
        scoped[params.length] = "account";
        scoped[params.length + 1] = fixture.destinationId();
        return listAs(fixture.adminToken(), scoped);
    }

    private ResultActions listAs(String adminToken, String... params) throws Exception {
        MockHttpServletRequestBuilder request =
                get("/admin/transfers").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken);
        for (int i = 0; i < params.length; i += 2) {
            request = request.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(request).andExpect(status().isOk());
    }

    private void transfer(String token, String from, String to, String amount) throws Exception {
        mockMvc.perform(post("/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountId\": \"%s\", \"toAccountId\": \"%s\", \"amount\": %s}"
                                .formatted(from, to, amount)))
                .andExpect(status().isCreated());
    }

    private void backdateByAmount(String amount, String date, String destinationId) {
        entityManager.flush();
        entityManager
                .createNativeQuery("UPDATE transfers SET created_at = CAST(:date AS timestamptz)"
                        + " WHERE amount = :amount AND to_account_id = :destination")
                .setParameter("date", date + " 12:00:00+00")
                .setParameter("amount", new BigDecimal(amount))
                .setParameter("destination", UUID.fromString(destinationId))
                .executeUpdate();
        entityManager.clear();
    }

    private void setStatusOfOldest(String status, String destinationId) {
        entityManager.flush();
        entityManager
                .createNativeQuery("UPDATE transfers SET status = :status WHERE amount = 25.0000 AND to_account_id = :destination")
                .setParameter("status", status)
                .setParameter("destination", UUID.fromString(destinationId))
                .executeUpdate();
        entityManager.clear();
    }

    private String accountNumberOf(String accountId) {
        entityManager.flush();
        return (String) entityManager
                .createNativeQuery("SELECT account_number FROM accounts WHERE id = :id")
                .setParameter("id", UUID.fromString(accountId))
                .getSingleResult();
    }

    private String openAccount(String token, String accountType, String currency) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/accounts/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountType\": \"%s\", \"currency\": \"%s\"}"
                                        .formatted(accountType, currency)))
                .andExpect(status().isCreated())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private void fund(String accountId, String balance) {
        entityManager.flush();
        entityManager
                .createNativeQuery("UPDATE accounts SET balance = :balance WHERE id = :accountId")
                .setParameter("balance", new BigDecimal(balance))
                .setParameter("accountId", UUID.fromString(accountId))
                .executeUpdate();
        entityManager.clear();
    }
}
