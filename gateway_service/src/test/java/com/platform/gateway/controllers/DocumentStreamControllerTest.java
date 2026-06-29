package com.platform.gateway.controllers;

import com.platform.gateway.exceptions.DocumentStreamTimeoutException;
import com.platform.gateway.exceptions.GlobalExceptionHandler;
import com.platform.gateway.exceptions.InvalidPageRangeException;
import com.platform.gateway.services.StorageStreamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice tests for GET /documents/{sourceDocId}/page/{pageNumber} covering Sub-Story 2.2a.
 *
 * Mapped acceptance criteria:
 *   TC-BE-01 — Happy path: valid doc + page → 200 + application/pdf byte stream.
 *   TC-BE-03 — Invalid page number → 422 Unprocessable Entity.
 *   TC-BE-04 — Storage timeout → 504 Gateway Timeout.
 *
 * TC-BE-02 (memory footprint under concurrent load) requires an integration/performance
 * harness and cannot be asserted with MockMvc — it verifies no OutOfMemoryError occurs
 * when 10 concurrent requests hit the streaming endpoint against a real S3-compatible store.
 */
@WebMvcTest(DocumentStreamController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class DocumentStreamControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    StorageStreamService storageStreamService;

    @Test
    void tcBe01_validDocAndPage_returns200WithPdfStream() throws Exception {
        byte[] pdfBytes = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF magic bytes
        ResponseInputStream<GetObjectResponse> fakeStream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                AbortableInputStream.create(new ByteArrayInputStream(pdfBytes)));

        when(storageStreamService.openStream(42L, 14)).thenReturn(fakeStream);

        mockMvc.perform(get("/documents/42/page/14"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    void tcBe01_unknownDocument_returns404() throws Exception {
        when(storageStreamService.openStream(999L, 1))
                .thenThrow(new NoSuchElementException("Document not found: 999"));

        mockMvc.perform(get("/documents/999/page/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.reason").value("Document not found: 999"));
    }

    @Test
    void tcBe03_pageNumberOutOfRange_returns422() throws Exception {
        // Document has 20 pages; page 99 must be rejected before stalling on S3.
        when(storageStreamService.openStream(1L, 99))
                .thenThrow(new InvalidPageRangeException(99, 1L));

        mockMvc.perform(get("/documents/1/page/99"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.reason").value("page_number 99 is out of range for document 1"));
    }

    @Test
    void tcBe04_storageBucketTimeout_returns504() throws Exception {
        // Simulates a slow storage provider that exceeds the 1.8-second SLA.
        when(storageStreamService.openStream(5L, 3))
                .thenThrow(new DocumentStreamTimeoutException(5L));

        mockMvc.perform(get("/documents/5/page/3"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.reason").value("Storage fetch timed out for document 5"));
    }
}
