package com.beltelecom.transfer.service;

import com.beltelecom.transfer.domain.Region;
import com.beltelecom.transfer.entity.TransferPath;
import com.beltelecom.transfer.exception.TransferProcessingException;
import com.beltelecom.transfer.repository.TransferPathRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Рабочие каталоги региона: {@code {path}/in}, {@code {path}/out}, {@code {path}/prt}.
 * Базовый {@code path} берётся из {@code transfer_path}.
 */
@Service
@RequiredArgsConstructor
public class TransferWorkspaceService {

    /** Пока обрабатываем только Минскую область; далее регион будет параметром. */
    public static final Region CURRENT_REGION = Region.MINSK_REGION;

    public static final String NO_PATH_IN_DB = "Нет пути в БД";
    public static final String DIRECTORY_MISSING = "Каталог отсутствует";

    private final TransferPathRepository transferPathRepository;

    public Optional<Workspace> resolveCurrentWorkspace() {
        return transferPathRepository.findFirstByIdRegionOrderByIdAsc(CURRENT_REGION.getId())
                .map(TransferPath::getPath)
                .filter(p -> p != null && !p.isBlank())
                .map(p -> Path.of(p.trim()).toAbsolutePath().normalize())
                .map(Workspace::new);
    }

    /**
     * Путь есть в БД и базовый каталог существует на диске.
     */
    public Optional<Workspace> resolveExistingCurrentWorkspace() {
        return resolveCurrentWorkspace().filter(ws -> Files.isDirectory(ws.basePath()));
    }

    public Workspace requireCurrentWorkspace() {
        return resolveCurrentWorkspace()
                .orElseThrow(() -> new TransferProcessingException("NO_PATH", NO_PATH_IN_DB));
    }

    public record Workspace(Path basePath) {

        public boolean exists() {
            return Files.isDirectory(basePath);
        }

        public Path in() {
            return basePath.resolve("in");
        }

        public Path out() {
            return basePath.resolve("out");
        }

        public Path prt() {
            return basePath.resolve("prt");
        }

        /**
         * Создаёт {@code out} и {@code prt}, если их ещё нет.
         * Вызывать только когда базовый каталог уже существует.
         */
        public void ensureOutAndPrt() {
            if (!exists()) {
                throw new TransferProcessingException("NO_DIRECTORY", DIRECTORY_MISSING);
            }
            createIfMissing(out());
            createIfMissing(prt());
        }

        private static void createIfMissing(Path dir) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new TransferProcessingException("IO_ERROR", "Не удалось создать каталог: " + dir, e);
            }
        }
    }
}
