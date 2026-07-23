package com.beltelecom.transfer.controller;

import com.beltelecom.transfer.dto.ProcessResponse;
import com.beltelecom.transfer.dto.TransferStatusResponse;
import com.beltelecom.transfer.entity.TransferLoadLog;
import com.beltelecom.transfer.service.TransferProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransferProcessingService processingService;

    @Test
    void shouldTriggerProcessing() throws Exception {
        when(processingService.processIncomingFiles()).thenReturn(ProcessResponse.builder()
                .filesProcessed(1)
                .filesSucceeded(1)
                .totalRecords(8)
                .message("OK")
                .build());

        mockMvc.perform(post("/api/v1/transfer/process").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filesProcessed").value(1))
                .andExpect(jsonPath("$.totalRecords").value(8));
    }

    @Test
    void shouldReturnStatus() throws Exception {
        when(processingService.getLastLoadStatus()).thenReturn(TransferStatusResponse.builder()
                .fileName("epb20260701090706.045")
                .status(TransferLoadLog.LoadStatus.SUCCESS)
                .recordsProcessed(8)
                .build());

        mockMvc.perform(get("/api/v1/transfer/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("epb20260701090706.045"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }
}
