package com.beltelecom.transfer.config;

import com.beltelecom.transfer.exception.TransferProcessingException;
import com.beltelecom.transfer.service.TransferWorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Каталог протоколов: {@code {transfer_path.path}/prt}.
 * Базовый каталог должен уже существовать — здесь его не создаём.
 */
@Component
@RequiredArgsConstructor
public class ProtocolDirectoryResolver {

    private final TransferWorkspaceService workspaceService;

    public Path resolve() {
        TransferWorkspaceService.Workspace workspace = workspaceService.requireCurrentWorkspace();
        if (!workspace.exists()) {
            throw new TransferProcessingException("NO_DIRECTORY", TransferWorkspaceService.DIRECTORY_MISSING);
        }
        return workspace.prt();
    }
}
