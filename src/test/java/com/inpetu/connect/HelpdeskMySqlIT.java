package com.inpetu.connect;

import com.inpetu.connect.dto.HelpdeskCreateRequest;
import com.inpetu.connect.service.HelpdeskCryptography;
import com.inpetu.connect.service.HelpdeskException;
import com.inpetu.connect.service.HelpdeskService;
import com.inpetu.connect.repository.HelpdeskTicketRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("helpdesk-mysql")
@ExtendWith(HelpdeskMySqlAvailabilityCondition.class)
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:3307/inpetu_helpdesk_test?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.jpa.hibernate.ddl-auto=update",
    "spring.sql.init.mode=never",
    "app.jwt.secret=helpdesk-mysql-test-jwt-secret-at-least-32-chars",
    "OTP_SECRET=helpdesk-mysql-test-otp-secret-at-least-32-chars",
    "APP_RATE_LIMIT_SECRET=helpdesk-mysql-test-rate-secret-at-least-32-chars",
    "app.mail.enabled=false",
    "app.helpdesk.rate.ip-per-hour=20",
    "app.helpdesk.rate.email-per-hour=5"
})
@ActiveProfiles("test")
class HelpdeskMySqlIT {
    private static final String DATABASE_URL =
        "jdbc:mysql://localhost:3307/inpetu_helpdesk_test?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
    static final String DATABASE_USER = System.getenv("HELPDESK_TEST_DB_USER");
    static final String DATABASE_PASSWORD = System.getenv("HELPDESK_TEST_DB_PASSWORD");

    @Autowired private HelpdeskService helpdesk;
    @Autowired private HelpdeskCryptography cryptography;
    @Autowired private HelpdeskTicketRepository tickets;

    @DynamicPropertySource
    static void databaseCredentials(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.username", () -> DATABASE_USER == null ? "" : DATABASE_USER);
        registry.add("spring.datasource.password", () -> DATABASE_PASSWORD == null ? "" : DATABASE_PASSWORD);
    }

    @Test
    void serializesConcurrentIpAndEmailLimitsInMySql() throws Exception {
        String ip = testIp();
        List<Attempt> ipAttempts = runConcurrently(25, index ->
            submit(ip, "teste-ip-" + UUID.randomUUID() + "@example.invalid", index));
        long acceptedByIp = ipAttempts.stream().filter(Attempt::accepted).count();
        assertEquals(20, acceptedByIp);
        assertEquals(5, ipAttempts.stream().filter(attempt -> "RATE_LIMIT_IP_EXCEEDED".equals(attempt.errorCode())).count());
        assertEquals(20, tickets.countByIpHmacAndCreatedAtAfter(
            cryptography.hmacHex("inpetu:helpdesk:rate-limit:ip:v1", ip), java.time.LocalDateTime.now().minusMinutes(60)));

        String sharedEmail = "teste-email-" + UUID.randomUUID() + "@example.invalid";
        List<Attempt> emailAttempts = runConcurrently(10, index ->
            submit(testIp(), sharedEmail, index));
        assertEquals(5, emailAttempts.stream().filter(Attempt::accepted).count());
        assertEquals(5, emailAttempts.stream()
            .filter(attempt -> "RATE_LIMIT_EMAIL_EXCEEDED".equals(attempt.errorCode())).count());
        assertEquals(5, tickets.countByEmailHmacAndCreatedAtAfter(
            cryptography.hmacHex("inpetu:helpdesk:rate-limit:email:v1", sharedEmail),
            java.time.LocalDateTime.now().minusMinutes(60)));
    }

    private Attempt submit(String ip, String email, int index) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        HelpdeskCreateRequest payload = new HelpdeskCreateRequest("TESTE solicitante",
            email, "TESTE assunto " + index, "TESTE motivo " + index, null, null);
        try {
            helpdesk.createTicket(payload, UUID.randomUUID().toString(), request);
            return new Attempt(true, null);
        } catch (HelpdeskException rejected) {
            return new Attempt(false, rejected.getCode());
        }
    }

    private List<Attempt> runConcurrently(int count, java.util.function.IntFunction<Attempt> action)
        throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Attempt>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < count; index++) {
                int taskIndex = index;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return action.apply(taskIndex);
                }));
            }
            ready.await();
            start.countDown();
            List<Attempt> results = new ArrayList<>();
            for (Future<Attempt> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private String testIp() {
        return "198.51." + (1 + java.util.concurrent.ThreadLocalRandom.current().nextInt(254))
            + "." + (1 + java.util.concurrent.ThreadLocalRandom.current().nextInt(254));
    }

    private record Attempt(boolean accepted, String errorCode) {
    }
}
