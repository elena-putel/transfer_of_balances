package com.beltelecom.transfer.integration;

import com.beltelecom.transfer.entity.TransferLoadLog;
import com.beltelecom.transfer.repository.TransferBalanceRepository;
import com.beltelecom.transfer.repository.TransferLoadLogRepository;
import com.beltelecom.transfer.service.TransferProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class TransferProcessingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("transfer_test")
            .withUsername("test")
            .withPassword("test");

    @TempDir
    static Path tempBase;

    static Path inputDir;
    static Path processedDir;
    static Path errorDir;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) throws IOException {
        inputDir = Files.createDirectory(tempBase.resolve("in"));
        processedDir = Files.createDirectory(tempBase.resolve("processed"));
        errorDir = Files.createDirectory(tempBase.resolve("error"));

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("transfer.input-directory", () -> inputDir.toString());
        registry.add("transfer.processed-directory", () -> processedDir.toString());
        registry.add("transfer.error-directory", () -> errorDir.toString());
        registry.add("transfer.scheduled.enabled", () -> "false");
    }

    @Autowired
    private TransferProcessingService processingService;

    @Autowired
    private TransferBalanceRepository balanceRepository;

    @Autowired
    private TransferLoadLogRepository loadLogRepository;

    @BeforeEach
    void cleanUp() {
        balanceRepository.deleteAll();
        loadLogRepository.deleteAll();
    }

    @Test
    void shouldProcessValidFilePair() throws IOException {
        copyTestFile("epb20260701090706.045", inputDir);
        copyTestFile("epbr20260701090706.045", inputDir);

        var response = processingService.processIncomingFiles();

        assertThat(response.getFilesSucceeded()).isEqualTo(1);
        assertThat(response.getTotalRecords()).isEqualTo(8);
        assertThat(balanceRepository.count()).isEqualTo(8);
        assertThat(loadLogRepository.findTopByOrderByStartedAtDesc())
                .isPresent()
                .get()
                .extracting(TransferLoadLog::getStatus)
                .isEqualTo(TransferLoadLog.LoadStatus.SUCCESS);
        assertThat(Files.list(processedDir).count()).isEqualTo(2);
    }

    @Test
    void shouldFailOnChecksumMismatch() throws IOException {
        copyTestFile("epb_invalid_checksum.045", inputDir);
        copyTestFile("epbr_invalid_checksum.045", inputDir);

        var response = processingService.processIncomingFiles();

        assertThat(response.getFilesFailed()).isEqualTo(1);
        assertThat(balanceRepository.count()).isZero();
        assertThat(loadLogRepository.findTopByOrderByStartedAtDesc())
                .isPresent()
                .get()
                .extracting(TransferLoadLog::getStatus)
                .isEqualTo(TransferLoadLog.LoadStatus.CHECKSUM_ERROR);
        assertThat(Files.list(errorDir).count()).isEqualTo(2);
    }

    @Test
    void shouldPreventDuplicateRecords() throws IOException {
        copyTestFile("epb20260701090706.045", inputDir);
        copyTestFile("epbr20260701090706.045", inputDir);
        processingService.processIncomingFiles();

        copyTestFile("epb20260701090706.045", inputDir);
        copyTestFile("epbr20260701090706.045", inputDir);
        var secondRun = processingService.processIncomingFiles();

        assertThat(secondRun.getFilesFailed()).isEqualTo(1);
        assertThat(balanceRepository.count()).isEqualTo(8);
    }

    private void copyTestFile(String name, Path targetDir) throws IOException {
        Path source = Path.of("src/test/resources/testdata", name);
        Files.copy(source, targetDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
    }
}
