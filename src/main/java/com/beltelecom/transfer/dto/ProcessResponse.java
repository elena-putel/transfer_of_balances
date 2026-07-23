package com.beltelecom.transfer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Результат обработки файлов переноса балансов")
public class ProcessResponse {

    @Schema(description = "Количество обработанных файлов")
    private int filesProcessed;

    @Schema(description = "Количество успешно загруженных файлов")
    private int filesSucceeded;

    @Schema(description = "Количество файлов с ошибками")
    private int filesFailed;

    @Schema(description = "Общее количество записей")
    private int totalRecords;

    @Schema(description = "Сообщение о результате")
    private String message;
}
