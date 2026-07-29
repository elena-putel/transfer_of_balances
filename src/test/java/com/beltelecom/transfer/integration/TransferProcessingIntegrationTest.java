package com.beltelecom.transfer.integration;

import com.beltelecom.transfer.domain.Region;
import com.beltelecom.transfer.entity.TransferLoadLog;
import com.beltelecom.transfer.entity.TransferPath;
import com.beltelecom.transfer.repository.CTransferRepository;
import com.beltelecom.transfer.repository.TransferLoadLogRepository;
import com.beltelecom.transfer.repository.TransferPathRepository;
import com.beltelecom.transfer.service.TransferProcessingService;
import com.beltelecom.transfer.service.TransferWorkspaceService;
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
    static Path outDir;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) throws IOException {
        inputDir = Files.createDirectories(tempBase.resolve("in"));
        outDir = tempBase.resolve("out");

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("informix.datasource.enabled", () -> "false");
        registry.add("transfer.scheduled.enabled", () -> "false");
    }

    @Autowired
    private TransferProcessingService processingService;

    @Autowired
    private CTransferRepository cTransferRepository;

    @Autowired
    private TransferLoadLogRepository loadLogRepository;

    @Autowired
    private TransferPathRepository transferPathRepository;

    @BeforeEach
    void cleanUp() {
        cTransferRepository.deleteAll();
        loadLogRepository.deleteAll();
        transferPathRepository.deleteAll();
        transferPathRepository.save(TransferPath.builder()
                .idRegion(Region.MINSK_REGION.getId())
                .path(tempBase.toAbsolutePath().toString())
                .build());
    }

    @Test
    void shouldFailWhenPathMissingInDb() {
        transferPathRepository.deleteAll();

        var response = processingService.processIncomingFiles();

        assertThat(response.getMessage()).isEqualTo(TransferWorkspaceService.NO_PATH_IN_DB);
        assertThat(response.getFilesFailed()).isEqualTo(1);
        assertThat(loadLogRepository.findTopByOrderByStartedAtDesc())
                .isPresent()
                .get()
                .satisfies(log -> {
                    assertThat(log.getErrorMessage()).isEqualTo(TransferWorkspaceService.NO_PATH_IN_DB);
                    assertThat(log.getFlFile()).isNull();
                    assertThat(log.getReportFile()).isNull();
                });
        assertThat(Files.exists(outDir)).isFalse();
        assertThat(Files.exists(tempBase.resolve("prt"))).isFalse();
    }

    @Test
    void shouldFailWhenBaseDirectoryMissing() throws IOException {
        Path missingBase = tempBase.resolve("missing-base");
        transferPathRepository.deleteAll();
        transferPathRepository.save(TransferPath.builder()
                .idRegion(Region.MINSK_REGION.getId())
                .path(missingBase.toAbsolutePath().toString())
                .build());

        var response = processingService.processIncomingFiles();

        assertThat(response.getMessage()).startsWith(TransferWorkspaceService.DIRECTORY_MISSING);
        assertThat(Files.exists(missingBase)).isFalse();
        assertThat(Files.exists(missingBase.resolve("out"))).isFalse();
        assertThat(Files.exists(missingBase.resolve("prt"))).isFalse();
        assertThat(loadLogRepository.findTopByOrderByStartedAtDesc())
                .isPresent()
                .get()
                .satisfies(log -> {
                    assertThat(log.getErrorMessage()).startsWith(TransferWorkspaceService.DIRECTORY_MISSING);
                    assertThat(log.getFlFile()).isNull();
                });
    }

    @Test
    void shouldProcessValidFilePair() throws IOException {
        copyTestFile("epb20260701090706.045", inputDir);
        copyTestFile("epbr20260701090706.045", inputDir);

        var response = processingService.processIncomingFiles();

        assertThat(response.getFilesSucceeded()).isEqualTo(1);
        assertThat(response.getTotalRecords()).isEqualTo(8);
        assertThat(cTransferRepository.count()).isEqualTo(8);
        assertThat(loadLogRepository.findTopByOrderByStartedAtDesc())
                .isPresent()
                .get()
                .extracting(TransferLoadLog::getStatus)
                .isEqualTo(TransferLoadLog.LoadStatus.SUCCESS);
        assertThat(Files.list(outDir).count()).isEqualTo(2);
        assertThat(Files.isDirectory(tempBase.resolve("prt"))).isTrue();
    }

    @Test
    void shouldFailOnChecksumMismatch() throws IOException {
        copyTestFile("epb_invalid_checksum.045", inputDir);
        copyTestFile("epbr_invalid_checksum.045", inputDir);

        var response = processingService.processIncomingFiles();

        assertThat(response.getFilesFailed()).isEqualTo(1);
        assertThat(cTransferRepository.count()).isZero();
        assertThat(loadLogRepository.findTopByOrderByStartedAtDesc())
                .isPresent()
                .get()
                .extracting(TransferLoadLog::getStatus)
                .isEqualTo(TransferLoadLog.LoadStatus.CHECKSUM_ERROR);
        assertThat(Files.list(outDir).count()).isEqualTo(2);
    }

    @Test
    void shouldRejectAlreadyLoadedFileName() throws IOException {
        copyTestFile("epb20260701090706.045", inputDir);
        copyTestFile("epbr20260701090706.045", inputDir);
        processingService.processIncomingFiles();

        copyTestFile("epb20260701090706.045", inputDir);
        copyTestFile("epbr20260701090706.045", inputDir);
        var secondRun = processingService.processIncomingFiles();

        assertThat(secondRun.getFilesFailed()).isEqualTo(1);
        assertThat(cTransferRepository.count()).isEqualTo(8);
        assertThat(loadLogRepository.findTopByOrderByStartedAtDesc())
                .isPresent()
                .get()
                .satisfies(log -> {
                    assertThat(log.getStatus()).isEqualTo(TransferLoadLog.LoadStatus.VALIDATION_ERROR);
                    assertThat(log.getErrorMessage()).isEqualTo("Файл с таким именем загружен");
                });
        assertThat(Files.list(outDir).count()).isEqualTo(2);
    }

    private void copyTestFile(String name, Path targetDir) throws IOException {
        Path source = Path.of("src/test/resources/testdata", name);
        Files.copy(source, targetDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
    }
}
