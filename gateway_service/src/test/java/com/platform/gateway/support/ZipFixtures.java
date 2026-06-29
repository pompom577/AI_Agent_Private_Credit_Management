package com.platform.gateway.support;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Test helpers that produce real ZIP byte arrays for MockMvc fixtures.
 * Used by {@code UploadControllerTest} to exercise TC-GW-01, 03, 04, 05.
 */
public final class ZipFixtures {

    private ZipFixtures() {}

    /** Standard ZIP with a single text entry — passes all 1.1 validations. */
    public static byte[] validZipWithTextEntry() throws IOException {
        return zipOf(Map.of("README.txt", "hello deal".getBytes(StandardCharsets.UTF_8)));
    }

    /** ZIP containing a denylisted {@code .exe} entry — triggers TC-GW-03 (415). */
    public static byte[] zipWithExeEntry() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("docs/notes.txt", "safe".getBytes(StandardCharsets.UTF_8));
        entries.put("tools/malware.exe", new byte[]{0x4D, 0x5A, 0x00, 0x00});
        return zipOf(entries);
    }

    /**
     * Hand-crafted minimal ZIP with one zero-byte entry "data.txt" whose
     * general-purpose-bit-flag advertises encryption (bit 0 set).
     * Commons Compress's {@code ZipArchiveOutputStream} refuses to write encrypted
     * content, so this fixture is built byte-for-byte against the PKWARE APPNOTE spec.
     * Triggers TC-GW-04 (422 encrypted archive).
     */
    public static byte[] encryptedFlagZip() {
        byte[] name = "data.txt".getBytes(StandardCharsets.US_ASCII);
        short nameLen = (short) name.length;

        java.nio.ByteBuffer lfh = java.nio.ByteBuffer.allocate(30 + nameLen)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN);
        lfh.putInt(0x04034B50);    // local file header signature
        lfh.putShort((short) 10);  // version needed (1.0)
        lfh.putShort((short) 0x0001); // general purpose bit flag: bit 0 = encrypted
        lfh.putShort((short) 0);   // compression method: stored
        lfh.putShort((short) 0);   // last mod time
        lfh.putShort((short) 0x21);// last mod date (1980-01-01)
        lfh.putInt(0);             // CRC-32
        lfh.putInt(0);             // compressed size
        lfh.putInt(0);             // uncompressed size
        lfh.putShort(nameLen);     // file name length
        lfh.putShort((short) 0);   // extra field length
        lfh.put(name);

        int lfhSize = lfh.position();

        java.nio.ByteBuffer cdh = java.nio.ByteBuffer.allocate(46 + nameLen)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN);
        cdh.putInt(0x02014B50);    // central directory file header signature
        cdh.putShort((short) 0x14);// version made by
        cdh.putShort((short) 10);  // version needed
        cdh.putShort((short) 0x0001); // gp flag: encrypted
        cdh.putShort((short) 0);   // compression
        cdh.putShort((short) 0);   // mod time
        cdh.putShort((short) 0x21);// mod date
        cdh.putInt(0);             // CRC-32
        cdh.putInt(0);             // compressed size
        cdh.putInt(0);             // uncompressed size
        cdh.putShort(nameLen);     // file name length
        cdh.putShort((short) 0);   // extra field length
        cdh.putShort((short) 0);   // file comment length
        cdh.putShort((short) 0);   // disk number start
        cdh.putShort((short) 0);   // internal file attrs
        cdh.putInt(0);             // external file attrs
        cdh.putInt(0);             // relative offset of local header
        cdh.put(name);

        int cdhSize = cdh.position();

        java.nio.ByteBuffer eocd = java.nio.ByteBuffer.allocate(22)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN);
        eocd.putInt(0x06054B50);   // end of central directory signature
        eocd.putShort((short) 0);  // disk number
        eocd.putShort((short) 0);  // disk with start of CD
        eocd.putShort((short) 1);  // entries on this disk
        eocd.putShort((short) 1);  // total entries
        eocd.putInt(cdhSize);      // size of central directory
        eocd.putInt(lfhSize);      // offset of CD start
        eocd.putShort((short) 0);  // comment length

        ByteArrayOutputStream out = new ByteArrayOutputStream(lfhSize + cdhSize + 22);
        out.write(lfh.array(), 0, lfhSize);
        out.write(cdh.array(), 0, cdhSize);
        out.write(eocd.array(), 0, eocd.position());
        return out.toByteArray();
    }

    private static byte[] zipOf(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(baos)) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                ZipArchiveEntry entry = new ZipArchiveEntry(e.getKey());
                zos.putArchiveEntry(entry);
                zos.write(e.getValue());
                zos.closeArchiveEntry();
            }
        }
        return baos.toByteArray();
    }
}
