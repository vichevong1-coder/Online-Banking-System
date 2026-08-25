package com.obs.backend.feature.qr.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig.RecordingOtpSender;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-032 / US-033 / US-034, against {@code qr-payments-spec.md}.
 *
 * <p>Funding works the same way as {@code TransferControllerTest}: there is no
 * deposit endpoint, so opening balances go in with a native UPDATE bracketed by
 * flush/clear.
 *
 * <p>Merchants are inserted the same way rather than read from the demo seed:
 * {@code seed_demo_data.sql} deliberately sits outside {@code
 * spring.flyway.locations} and never runs in tests, so every merchant a test
 * needs is a fixture the test writes itself.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class QrControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    @PersistenceContext private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void myQrPayloadNamesTheCallersAccount() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-32-000-001", "correct-horse");
        String account = openAccount(token, "SAVINGS", "USD");
        String accountNumber = accountNumberOf(token, account);

        mockMvc.perform(
                        get("/qr/me").param("accountId", account).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(account))
                .andExpect(jsonPath("$.accountNumber").value(accountNumber))
                .andExpect(jsonPath("$.currency").value("USD"))
                // Parseable by the rule the spec fixes: OBS1:P:<account number>,
                // no amount — a personal code is an address, not an invoice.
                .andExpect(jsonPath("$.payload").value("OBS1:P:" + accountNumber));
    }

    @Test
    void myQrPayloadForSomebodyElsesAccountReturnsNotFound() throws Exception {
        String ownerToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-32-000-002", "correct-horse");
        String othersAccount = openAccount(ownerToken, "SAVINGS", "USD");

        String intruderToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-32-000-003", "correct-horse");

        // 404, not 403 — same rule as everywhere else on an account id.
        mockMvc.perform(
                        get("/qr/me")
                                .param("accountId", othersAccount)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ACCOUNT_NOT_FOUND"));
    }

    // NOT_SUPPORTED: the US-035 notification is written by an AFTER_COMMIT listener, which
    // never fires inside a test transaction that always rolls back. This method therefore
    // commits for real — it registers its own customer under a phone number no other test
    // uses, so the rows it leaves behind are inert.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Test
    void payingAPersonalQrMovesMoneyToSomebodyElsesAccount() throws Exception {
        // US-033. The destination is deliberately not the caller's: that is the
        // entire point of a personal QR.
        String payeeToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-33-000-001", "correct-horse");
        String payeeAccount = openAccount(payeeToken, "SAVINGS", "USD");
        String payload = qrPayload(payeeToken, payeeAccount);

        String payerToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-33-000-002", "correct-horse");
        String payerAccount = openAccount(payerToken, "CHECKING", "USD");
        fundCommitted(payerAccount, "500.0000");

        MvcResult result = mockMvc.perform(payRequest(payerToken, payload, payerAccount, "40.25", "USD", "Lunch"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(40.25))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.description").value("Lunch"))
                .andExpect(jsonPath("$.fromAccountId").value(payerAccount))
                .andExpect(jsonPath("$.toAccountId").value(payeeAccount))
                .andExpect(jsonPath("$.reference").exists())
                .andReturn();
        String transferId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(
                        get("/accounts/" + payerAccount + "/balance")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + payerToken))
                .andExpect(jsonPath("$.balance").value(459.75));
        mockMvc.perform(
                        get("/accounts/" + payeeAccount + "/balance")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + payeeToken))
                .andExpect(jsonPath("$.balance").value(40.25));

        // Both legs, one transfer — a QR payment is an ordinary internal transfer.
        assertLegCountForTransferCommitted(transferId, 2);
        mockMvc.perform(get("/transfers").header(HttpHeaders.AUTHORIZATION, "Bearer " + payerToken))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(transferId))
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"));

        // US-035: the shared notification seam, not one of its own.
        mockMvc.perform(get("/notifications").header(HttpHeaders.AUTHORIZATION, "Bearer " + payerToken))
                .andExpect(jsonPath("$.content[0].type").value("TRANSFER"))
                .andExpect(jsonPath("$.content[0].title").value("Transfer completed"));
    }

    @Test
    void payingAMerchantQrSettlesIntoTheMerchantsAccount() throws Exception {
        // US-034. The merchant's settlement account is an ordinary account, so
        // nothing about the money movement is special-cased.
        String merchantToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-34-000-001", "correct-horse");
        String settlementAccount = openAccount(merchantToken, "CHECKING", "USD");
        createMerchant("MERCH-TEST-COFFEE", "Angkor Coffee", settlementAccount, "ACTIVE");

        String payerToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-34-000-002", "correct-horse");
        String payerAccount = openAccount(payerToken, "SAVINGS", "USD");
        fund(payerAccount, "500.0000");

        MvcResult result = mockMvc.perform(
                        payRequest(payerToken, "OBS1:M:MERCH-TEST-COFFEE", payerAccount, "12.50", "USD", "Coffee"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(12.50))
                .andExpect(jsonPath("$.toAccountId").value(settlementAccount))
                .andReturn();
        String transferId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(
                        get("/accounts/" + payerAccount + "/balance")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + payerToken))
                .andExpect(jsonPath("$.balance").value(487.50));
        mockMvc.perform(
                        get("/accounts/" + settlementAccount + "/balance")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + merchantToken))
                .andExpect(jsonPath("$.balance").value(12.50));
        assertLegCountForTransfer(transferId, 2);
    }

    @Test
    void payingAnAlwaysDecliningMerchantWritesAFailedTransferAndMovesNoMoney() throws Exception {
        // US-034's centrepiece. The decline is data, not a hardcoded code: this
        // merchant is ALWAYS_DECLINES and everything else about it is ordinary.
        String merchantToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-34-000-003", "correct-horse");
        String settlementAccount = openAccount(merchantToken, "CHECKING", "USD");
        createMerchant("MERCH-TEST-DECLINE", "Riverside Books", settlementAccount, "ALWAYS_DECLINES");

        String payerToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-34-000-004", "correct-horse");
        String payerAccount = openAccount(payerToken, "SAVINGS", "USD");
        fund(payerAccount, "500.0000");
        String balanceBefore = balanceOf(payerToken, payerAccount);

        mockMvc.perform(payRequest(payerToken, "OBS1:M:MERCH-TEST-DECLINE", payerAccount, "30.00", "USD", "Books"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MERCHANT_DECLINED"));

        // Byte-identical, both ends: no debit, no credit, no ledger legs.
        org.assertj.core.api.Assertions.assertThat(balanceOf(payerToken, payerAccount))
                .isEqualTo(balanceBefore);
        mockMvc.perform(
                        get("/accounts/" + settlementAccount + "/balance")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + merchantToken))
                .andExpect(jsonPath("$.balance").value(0));
        mockMvc.perform(
                        get("/accounts/" + payerAccount + "/transactions")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + payerToken))
                .andExpect(jsonPath("$.content.length()").value(0));

        // But the decline is visible: a FAILED row in the payer's own history.
        // A decline that leaves no trace is not demoable.
        MvcResult history = mockMvc.perform(
                        get("/transfers").header(HttpHeaders.AUTHORIZATION, "Bearer " + payerToken))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("FAILED"))
                .andExpect(jsonPath("$.content[0].amount").value(30.00))
                .andExpect(jsonPath("$.content[0].toAccountId").value(settlementAccount))
                .andReturn();
        assertLegCountForTransfer(JsonPath.read(history.getResponse().getContentAsString(), "$.content[0].id"), 0);
    }

    @Test
    void aPayloadWithAFixedAmountRejectsADifferentOneAndAcceptsAnOmittedOne() throws Exception {
        String merchantToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-34-000-005", "correct-horse");
        String settlementAccount = openAccount(merchantToken, "CHECKING", "USD");
        createMerchant("MERCH-TEST-PRICED", "Angkor Coffee", settlementAccount, "ACTIVE");
        String priced = "OBS1:M:MERCH-TEST-PRICED:USD:12.50";

        String payerToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-34-000-006", "correct-horse");
        String payerAccount = openAccount(payerToken, "SAVINGS", "USD");
        fund(payerAccount, "500.0000");

        // Underpaying a displayed price is the thing this rule exists to stop.
        mockMvc.perform(payRequest(payerToken, priced, payerAccount, "10.00", "USD", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("QR_AMOUNT_MISMATCH"));
        // A different currency against a priced payload is the same disagreement.
        mockMvc.perform(payRequest(payerToken, priced, payerAccount, "12.50", "KHR", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("QR_AMOUNT_MISMATCH"));

        // Matching exactly is fine, and so is leaving it out — the payload said.
        mockMvc.perform(payRequest(payerToken, priced, payerAccount, "12.50", "USD", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(12.50));
        mockMvc.perform(payRequest(payerToken, priced, payerAccount, null, null, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(12.50))
                .andExpect(jsonPath("$.currency").value("USD"));

        mockMvc.perform(
                        get("/accounts/" + payerAccount + "/balance")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + payerToken))
                .andExpect(jsonPath("$.balance").value(475.00));
    }

    @Test
    void aPayloadWithoutAnAmountNeedsOneInTheRequest() throws Exception {
        String payeeToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-33-000-003", "correct-horse");
        String payload = qrPayload(payeeToken, openAccount(payeeToken, "SAVINGS", "USD"));

        String payerToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-33-000-004", "correct-horse");
        String payerAccount = openAccount(payerToken, "CHECKING", "USD");
        fund(payerAccount, "500.0000");

        // Reported as VALIDATION_FAILED because that is what it is — a field-level
        // omission Bean Validation could not see without the payload.
        mockMvc.perform(payRequest(payerToken, payload, payerAccount, null, null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
        mockMvc.perform(payRequest(payerToken, payload, payerAccount, "10.00", null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.currency").exists());
    }

    @Test
    void malformedPayloadsAreRejectedUnparsed() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-33-000-005", "correct-horse");
        String account = openAccount(token, "SAVINGS", "USD");
        fund(account, "500.0000");

        String[] malformed = {
            "hello",
            // Wrong marker: a payload not starting with OBS1 is rejected unparsed.
            "OBS0:P:900000000001",
            // Unknown type.
            "OBS1:X:900000000001",
            // Four fields: currency and amount come as a pair or not at all.
            "OBS1:P:900000000001:USD",
            "OBS1:P:900000000001:GBP:5.00",
            "OBS1:P:900000000001:USD:not-a-number",
            // More precision than NUMERIC(19,4) can hold.
            "OBS1:P:900000000001:USD:5.000005",
            "OBS1:P::USD:5.00"
        };
        for (String payload : malformed) {
            mockMvc.perform(payRequest(token, payload, account, "5.00", "USD", null))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("QR_PAYLOAD_INVALID"));
        }
    }

    @Test
    void payloadsNamingSomethingThatDoesNotExistAreRejected() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-33-000-006", "correct-horse");
        String account = openAccount(token, "SAVINGS", "USD");
        fund(account, "500.0000");

        // Well-formed but stale or fake: a 400, not a 404 — the request is fine,
        // the scanned code is not.
        mockMvc.perform(payRequest(token, "OBS1:P:999999999999", account, "5.00", "USD", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("QR_TARGET_NOT_FOUND"));
        mockMvc.perform(payRequest(token, "OBS1:M:MERCH-NOT-A-SHOP", account, "5.00", "USD", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("QR_TARGET_NOT_FOUND"));
    }

    @Test
    void payingYourOwnQrIsRejected() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-33-000-007", "correct-horse");
        String account = openAccount(token, "SAVINGS", "USD");
        fund(account, "500.0000");

        // Scanning your own code is a mis-scan.
        mockMvc.perform(payRequest(token, qrPayload(token, account), account, "5.00", "USD", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("SAME_ACCOUNT_TRANSFER"));
    }

    @Test
    void payingAcrossCurrenciesIsRejectedRatherThanConverted() throws Exception {
        // The one rejection a reviewer might mistake for a bug, so it is worth
        // demoing: a USD account cannot pay a KHR merchant.
        String merchantToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-34-000-007", "correct-horse");
        String settlementAccount = openAccount(merchantToken, "CHECKING", "KHR");
        createMerchant("MERCH-TEST-PSAR", "Psar Thmei Market", settlementAccount, "ACTIVE");

        String payerToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-34-000-008", "correct-horse");
        String payerAccount = openAccount(payerToken, "SAVINGS", "USD");
        fund(payerAccount, "500.0000");

        mockMvc.perform(payRequest(payerToken, "OBS1:M:MERCH-TEST-PSAR", payerAccount, "20.00", "USD", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CURRENCY_MISMATCH"));
        // Declaring the merchant's currency instead does not help: the source
        // account still does not hold it.
        mockMvc.perform(payRequest(payerToken, "OBS1:M:MERCH-TEST-PSAR", payerAccount, "20000.00", "KHR", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CURRENCY_MISMATCH"));
    }

    @Test
    void aQrPaymentLargerThanTheBalanceOrThePerTransferCapIsRejected() throws Exception {
        String payeeToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-33-000-008", "correct-horse");
        String payload = qrPayload(payeeToken, openAccount(payeeToken, "SAVINGS", "USD"));

        String payerToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-33-000-009", "correct-horse");
        String payerAccount = openAccount(payerToken, "CHECKING", "USD");
        fund(payerAccount, "10.0000");

        mockMvc.perform(payRequest(payerToken, payload, payerAccount, "10.01", "USD", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_FUNDS"));

        // Funded well past the amount — this one is the US-027 cap talking.
        fund(payerAccount, "50000.0000");
        mockMvc.perform(payRequest(payerToken, payload, payerAccount, "5000.01", "USD", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TRANSFER_LIMIT_EXCEEDED"));

        mockMvc.perform(
                        get("/accounts/" + payerAccount + "/balance")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + payerToken))
                .andExpect(jsonPath("$.balance").value(50000.00));
    }

    @Test
    void qrPaymentsCountTowardsTheSameDailyCapAsOtherTransfers() throws Exception {
        // Not a side channel around US-027: one cap, shared with US-025/US-026.
        String payeeToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-33-000-010", "correct-horse");
        String payload = qrPayload(payeeToken, openAccount(payeeToken, "SAVINGS", "USD"));

        String payerToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-33-000-011", "correct-horse");
        String payerAccount = openAccount(payerToken, "CHECKING", "USD");
        String ownAccount = openAccount(payerToken, "SAVINGS", "USD");
        fund(payerAccount, "50000.0000");

        // An own-accounts 4,000 and a QR 4,000 share one 10,000 daily cap, so the
        // third is over it whichever kind it is.
        mockMvc.perform(
                        post("/transfers")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + payerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"fromAccountId\": \"%s\", \"toAccountId\": \"%s\", \"amount\": 4000.00}"
                                        .formatted(payerAccount, ownAccount)))
                .andExpect(status().isCreated());
        mockMvc.perform(payRequest(payerToken, payload, payerAccount, "4000.00", "USD", null))
                .andExpect(status().isCreated());
        mockMvc.perform(payRequest(payerToken, payload, payerAccount, "4000.00", "USD", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("DAILY_LIMIT_EXCEEDED"));

        mockMvc.perform(
                        get("/accounts/" + payerAccount + "/balance")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + payerToken))
                .andExpect(jsonPath("$.balance").value(42000.00));
    }

    @Test
    void payingFromAnAccountYouDoNotOwnReturnsNotFound() throws Exception {
        String ownerToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-33-000-012", "correct-horse");
        String othersAccount = openAccount(ownerToken, "SAVINGS", "USD");
        fund(othersAccount, "500.0000");
        String payload = qrPayload(ownerToken, othersAccount);

        String intruderToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-33-000-013", "correct-horse");

        // The destination may be a stranger's; the source may not.
        mockMvc.perform(payRequest(intruderToken, payload, othersAccount, "5.00", "USD", null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void aPayWithoutARequiredFieldFailsValidation() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-33-000-014", "correct-horse");

        mockMvc.perform(
                        post("/qr/pay")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"amount\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.payload").exists())
                .andExpect(jsonPath("$.fieldErrors.fromAccountId").exists())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void qrEndpointsAreRejectedWithoutAToken() throws Exception {
        mockMvc.perform(get("/qr/me").param("accountId", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(
                        post("/qr/pay")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"payload\": \"OBS1:P:900000000001\", \"fromAccountId\": \"%s\"}"
                                        .formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder payRequest(
            String token, String payload, String fromAccountId, String amount, String currency, String description) {
        StringBuilder body = new StringBuilder("{\"payload\": \"%s\", \"fromAccountId\": \"%s\""
                .formatted(payload, fromAccountId));
        if (amount != null) {
            body.append(", \"amount\": ").append(amount);
        }
        if (currency != null) {
            body.append(", \"currency\": \"").append(currency).append('"');
        }
        if (description != null) {
            body.append(", \"description\": \"").append(description).append('"');
        }
        body.append('}');
        return post("/qr/pay")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString());
    }

    /** US-032 is how a payer gets a personal payload, so the tests get theirs the same way. */
    private String qrPayload(String token, String accountId) throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/qr/me")
                                .param("accountId", accountId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.payload");
    }

    private String accountNumberOf(String token, String accountId) throws Exception {
        MvcResult result = mockMvc.perform(get("/accounts").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        java.util.List<String> numbers = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$[?(@.id == '%s')].accountNumber".formatted(accountId));
        return numbers.get(0);
    }

    private String balanceOf(String token, String accountId) throws Exception {
        return mockMvc.perform(
                        get("/accounts/" + accountId + "/balance")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
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
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    /**
     * The three demo merchants live in {@code seed_demo_data.sql}, which never
     * runs in tests, so a test that needs a merchant inserts its own. There is no
     * merchant CRUD endpoint to go through — US-034 is seeded rows only.
     */
    private void createMerchant(String merchantCode, String displayName, String settlementAccountId, String status) {
        entityManager.flush();
        entityManager
                .createNativeQuery(
                        "INSERT INTO merchants (id, merchant_code, display_name, settlement_account_id, status)"
                                + " VALUES (:id, :code, :name, :accountId, :status)")
                .setParameter("id", UUID.randomUUID())
                .setParameter("code", merchantCode)
                .setParameter("name", displayName)
                .setParameter("accountId", UUID.fromString(settlementAccountId))
                .setParameter("status", status)
                .executeUpdate();
        entityManager.clear();
    }

    /**
     * There is no deposit endpoint, so opening balances go straight into the row.
     * flush before / clear after: the account is managed in this test's
     * transaction and would otherwise keep serving its cached balance.
     */
    private void fund(String accountId, String balance) {
        entityManager.flush();
        entityManager
                .createNativeQuery("UPDATE accounts SET balance = :balance WHERE id = :accountId")
                .setParameter("balance", new BigDecimal(balance))
                .setParameter("accountId", UUID.fromString(accountId))
                .executeUpdate();
        entityManager.clear();
    }

    private void assertLegCountForTransfer(String transferId, int expectedLegs) {
        entityManager.flush();
        Number legs = (Number) entityManager
                .createNativeQuery("SELECT count(*) FROM transactions WHERE transfer_id = :transferId")
                .setParameter("transferId", UUID.fromString(transferId))
                .getSingleResult();
        org.assertj.core.api.Assertions.assertThat(legs.intValue()).isEqualTo(expectedLegs);
    }

    /**
     * fund()'s counterpart for the NOT_SUPPORTED tests below: with no transaction active
     * there is nothing to flush and no JPA cache to clear, and JdbcTemplate auto-commits so
     * the balance is visible to the request thread.
     */
    private void fundCommitted(String accountId, String balance) {
        jdbcTemplate.update(
                "UPDATE accounts SET balance = ? WHERE id = ?", new BigDecimal(balance), UUID.fromString(accountId));
    }

    /** assertLegCountForTransfer's counterpart for the NOT_SUPPORTED test, same reason as fundCommitted. */
    private void assertLegCountForTransferCommitted(String transferId, int expectedLegs) {
        Integer legs = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM transactions WHERE transfer_id = ?", Integer.class, UUID.fromString(transferId));
        org.assertj.core.api.Assertions.assertThat(legs).isEqualTo(expectedLegs);
    }
}
