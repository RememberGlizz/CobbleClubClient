/*
 * Decompiled with CFR 0.152.
 */
package com.cobbleclub.server.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public final class SellProtocol {
    public static final int MAX_COMPRESSED_BYTES = 262144;
    private static final int MAX_JSON_BYTES = 0x200000;

    private SellProtocol() {
    }

    public static byte[] compress(String json) {
        byte[] raw = (json == null ? "{}" : json).getBytes(StandardCharsets.UTF_8);
        if (raw.length > 0x200000) {
            throw new IllegalArgumentException("Sell catalog exceeds decoded size limit");
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(raw.length, 65536));
            try (DeflaterOutputStream deflater = new DeflaterOutputStream(output);){
                deflater.write(raw);
            }
            byte[] compressed = output.toByteArray();
            if (compressed.length > 262144) {
                throw new IllegalArgumentException("Sell catalog exceeds packet size limit");
            }
            return compressed;
        }
        catch (IOException error) {
            throw new IllegalArgumentException("Could not encode sell catalog", error);
        }
    }

    /*
     * Enabled aggressive exception aggregation
     */
    public static String decompress(byte[] compressed) {
        if (compressed == null || compressed.length == 0 || compressed.length > 262144) {
            throw new IllegalArgumentException("Invalid sell catalog packet size");
        }
        try (InflaterInputStream inflater = new InflaterInputStream(new ByteArrayInputStream(compressed));){
            String string;
            try (ByteArrayOutputStream output = new ByteArrayOutputStream();){
                int read;
                byte[] buffer = new byte[8192];
                int total = 0;
                while ((read = inflater.read(buffer)) >= 0) {
                    if ((total += read) > 0x200000) {
                        throw new IllegalArgumentException("Sell catalog exceeds decoded size limit");
                    }
                    output.write(buffer, 0, read);
                }
                string = output.toString(StandardCharsets.UTF_8);
            }
            return string;
        }
        catch (IOException error) {
            throw new IllegalArgumentException("Could not decode sell catalog", error);
        }
    }
}

