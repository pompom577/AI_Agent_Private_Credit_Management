package com.platform.gateway.services;

import com.platform.gateway.exceptions.EncryptedArchiveException;
import com.platform.gateway.exceptions.InvalidArchiveFormatException;
import com.platform.gateway.exceptions.UnsupportedArchiveEntryException;
import org.apache.commons.compress.archivers.zip.UnsupportedZipFeatureException;
import org.apache.commons.compress.archivers.zip.UnsupportedZipFeatureException.Feature;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

/**
 * Deep-stream ZIP inspection for Story 1.1.
 *
 * 1.1a: validateMagicBytes confirms PK local-file-header signature.
 * 1.1b: inspectEntries rejects denylisted file types -> 415; assertNotEncrypted
 *       detects password-protected archives -> 422.
 */
@Service
public class ZipInspectionService {

    /** Standard ZIP local-file-header magic: {@code PK\x03\x04}. */
    private static final byte[] ZIP_LFH = {0x50, 0x4B, 0x03, 0x04};
    /** Empty-archive end-of-central-directory magic: {@code PK\x05\x06}. */
    private static final byte[] ZIP_EOCD_EMPTY = {0x50, 0x4B, 0x05, 0x06};
    /** Spanned/split archive marker: {@code PK\x07\x08}. */
    private static final byte[] ZIP_SPANNED = {0x50, 0x4B, 0x07, 0x08};

    /**
     * File extensions disallowed inside deal-ingestion archives.
     * Coordinated with Product — keep in sync with TC-GW-03 fixtures.
     */
    static final Set<String> DENYLISTED_EXTENSIONS = Set.of(
            "exe", "bat", "sh", "cmd", "com", "msi", "scr", "dll", "ps1", "vbs", "jar");

    /**
     * Reads the first 4 bytes of the uploaded file and rejects anything that is
     * not a valid ZIP signature. Covers TC-GW-02 (renamed .exe -> 415).
     *
     * @throws InvalidArchiveFormatException when the file does not start with a PK ZIP header.
     */
    public void validateMagicBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidArchiveFormatException("empty upload");
        }
        byte[] head = new byte[4];
        try (InputStream in = file.getInputStream()) {
            int read = in.readNBytes(head, 0, 4);
            if (read < 4) {
                throw new InvalidArchiveFormatException("file too short to be a zip");
            }
        } catch (IOException e) {
            throw new InvalidArchiveFormatException("could not read upload stream: " + e.getMessage());
        }
        if (!startsWith(head, ZIP_LFH)
                && !startsWith(head, ZIP_EOCD_EMPTY)
                && !startsWith(head, ZIP_SPANNED)) {
            throw new InvalidArchiveFormatException("not a valid zip archive (magic byte mismatch)");
        }
    }

    /**
     * Walks every entry in the archive and rejects any file whose extension is on the
     * {@link #DENYLISTED_EXTENSIONS} list. Covers TC-GW-03.
     *
     * @throws UnsupportedArchiveEntryException when a denylisted extension is found.
     * @throws InvalidArchiveFormatException    when the bytes are not a parseable ZIP.
     */
    public void inspectEntries(MultipartFile file) {
        try (InputStream in = file.getInputStream();
             ZipArchiveInputStream zis = new ZipArchiveInputStream(in)) {
            ZipArchiveEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String ext = extensionOf(entry.getName());
                if (ext != null && DENYLISTED_EXTENSIONS.contains(ext)) {
                    throw new UnsupportedArchiveEntryException(
                            "disallowed entry inside archive: " + entry.getName());
                }
            }
        } catch (UnsupportedZipFeatureException e) {
            if (e.getFeature() == Feature.ENCRYPTION) {
                throw new EncryptedArchiveException(
                        "encrypted entry detected during inspection: " + e.getMessage());
            }
            throw new InvalidArchiveFormatException("unsupported zip feature: " + e.getMessage());
        } catch (IOException e) {
            throw new InvalidArchiveFormatException("corrupt zip archive: " + e.getMessage());
        }
    }

    /**
     * Walks every entry header and flips to {@link EncryptedArchiveException} as soon as
     * the general-purpose-bit-flag advertises encryption. Covers TC-GW-04.
     *
     * @throws EncryptedArchiveException     when any entry has the encryption bit set.
     * @throws InvalidArchiveFormatException when the bytes are not a parseable ZIP.
     */
    public void assertNotEncrypted(MultipartFile file) {
        try (InputStream in = file.getInputStream();
             ZipArchiveInputStream zis = new ZipArchiveInputStream(in)) {
            ZipArchiveEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getGeneralPurposeBit().usesEncryption()) {
                    throw new EncryptedArchiveException(
                            "encrypted entry detected: " + entry.getName());
                }
            }
        } catch (UnsupportedZipFeatureException e) {
            if (e.getFeature() == Feature.ENCRYPTION) {
                throw new EncryptedArchiveException(
                        "encrypted entry detected: " + e.getMessage());
            }
            throw new InvalidArchiveFormatException("unsupported zip feature: " + e.getMessage());
        } catch (IOException e) {
            throw new InvalidArchiveFormatException("corrupt zip archive: " + e.getMessage());
        }
    }

    private static String extensionOf(String name) {
        if (name == null) return null;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        String basename = slash >= 0 ? name.substring(slash + 1) : name;
        int dot = basename.lastIndexOf('.');
        if (dot < 0 || dot == basename.length() - 1) return null;
        return basename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) return false;
        }
        return true;
    }
}
