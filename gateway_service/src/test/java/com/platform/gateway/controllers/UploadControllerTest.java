package com.platform.gateway.controllers;

import com.platform.gateway.clients.FastApiClient;
import com.platform.gateway.exceptions.GlobalExceptionHandler;
import com.platform.gateway.services.DealOrchestrationService;
import com.platform.gateway.services.JwtService;
import com.platform.gateway.services.StorageService;
import com.platform.gateway.services.ZipInspectionService;
import com.platform.gateway.support.ZipFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice tests for POST /uploads covering Sub-Stories 1.1a and 1.1b.
 *
 * Mapped acceptance criteria:
 *   TC-GW-01 — Happy path: valid ZIP -> 201 + {bucket_url, deal_id}.
 *   TC-GW-02 — Magic-byte mismatch (renamed .exe) -> 415 + {reason:"invalid zip format"}.
 *   TC-GW-03 — Unsupported entry inside zip (.exe)    -> 415 + {reason:"unsupported entries"}.
 *   TC-GW-04 — Encrypted/password-protected archive   -> 422 + {reason:"encrypted archive"}.
 *   TC-GW-05 — Persistence: StorageService.put invoked exactly once on success.
 */
@WebMvcTest(UploadController.class)
@Import({ZipInspectionService.class, JwtService.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class UploadControllerTest {

    /** MZ — Windows PE executable header (a .exe renamed to .zip). */
    private static final byte[] EXE_SIGNATURE = {0x4D, 0x5A, (byte) 0x90, 0x00};

    @Autowired
    MockMvc mockMvc;

    @MockBean
    StorageService storageService;

    // JwtService is the real bean (imported above) — Mockito on Java 25 cannot
    // subclass-mock its final SecretKey field, and the real one is cheap to build.

    @MockBean
    FastApiClient fastApiClient;

    @MockBean
    DealOrchestrationService dealOrchestrationService;

    @Test
    void tcGw01_happyPath_validZip_returns201WithBucketUrlAndDealId() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "deal.zip", "application/zip", ZipFixtures.validZipWithTextEntry());

        when(storageService.put(anyString(), any())).thenAnswer(invocation ->
                "s3://test-bucket/" + invocation.getArgument(0) + "/deal.zip");

        mockMvc.perform(multipart("/uploads").file(file).header("X-User-Id", "user-123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bucket_url", notNullValue()))
                .andExpect(jsonPath("$.deal_id", notNullValue()));

        // The parent deals row must be persisted on success — without it the FastAPI
        // worker's document_records inserts fail the deals(deal_id) foreign key.
        verify(dealOrchestrationService, times(1))
                .recordIngestedDeal(anyString(), anyString(), anyString());
    }

    @Test
    void tcGw02_magicByteMismatch_renamedExe_returns415() throws Exception {
        byte[] payload = new byte[]{EXE_SIGNATURE[0], EXE_SIGNATURE[1], EXE_SIGNATURE[2], EXE_SIGNATURE[3],
                'f', 'a', 'k', 'e'};
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.zip", "application/zip", payload);

        mockMvc.perform(multipart("/uploads").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.reason").value("invalid zip format"));

        verify(storageService, never()).put(anyString(), any());
        verify(dealOrchestrationService, never()).recordIngestedDeal(anyString(), anyString(), anyString());
    }

    @Test
    void tcGw03_archiveContainsExecutableEntry_returns415() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "deal.zip", "application/zip", ZipFixtures.zipWithExeEntry());

        mockMvc.perform(multipart("/uploads").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.reason").value("unsupported entries"));

        verify(storageService, never()).put(anyString(), any());
        verify(dealOrchestrationService, never()).recordIngestedDeal(anyString(), anyString(), anyString());
    }

    @Test
    void tcGw04_encryptedArchive_returns422() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "secret.zip", "application/zip", ZipFixtures.encryptedFlagZip());

        mockMvc.perform(multipart("/uploads").file(file))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.reason").value("encrypted archive"));

        verify(storageService, never()).put(anyString(), any());
        verify(dealOrchestrationService, never()).recordIngestedDeal(anyString(), anyString(), anyString());
    }

    @Test
    void tcGw05_persistenceInvokedExactlyOnceOnHappyPath() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "deal.zip", "application/zip", ZipFixtures.validZipWithTextEntry());

        when(storageService.put(anyString(), any())).thenReturn("s3://test-bucket/x/deal.zip");

        mockMvc.perform(multipart("/uploads").file(file))
                .andExpect(status().isCreated());

        verify(storageService, times(1)).put(anyString(), any());
    }
}
