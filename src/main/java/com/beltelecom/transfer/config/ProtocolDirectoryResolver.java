package com.beltelecom.transfer.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Каталог протоколов: {@code <корень data>/protocole}.
 */
@Component
@RequiredArgsConstructor
public class ProtocolDirectoryResolver {

    private final TransferProperties properties;

    public Path resolve() {
        String configured = properties.getProtocolDirectory();
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        Path inputDir = Path.of(properties.getInputDirectory()).toAbsolutePath().normalize();
        Path dataRoot = inputDir.getParent() != null ? inputDir.getParent() : inputDir;
        return dataRoot.resolve("protocole");
    }
}
