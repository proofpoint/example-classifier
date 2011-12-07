package com.proofpoint.example.classifier;

import com.google.common.io.ByteStreams;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class StreamFlattener
{
    private final byte[] ZIP_MAGIC = new byte[] { 0x50, 0x4B, 0x03, 0x04 };

    public void flatten(InputStream input, EntryProcessor processor)
            throws IOException
    {
        BufferedInputStream bufferedStream = new BufferedInputStream(input);

        if (isZip(bufferedStream)) {
            // process each entry
            ZipInputStream zipStream = new ZipInputStream(bufferedStream);
            for (ZipEntry entry = zipStream.getNextEntry(); entry != null; entry = zipStream.getNextEntry()) {
                flatten(zipStream, processor);
                zipStream.closeEntry();
            }
        }
        else {
            // process as single entry
            processor.process(bufferedStream);
        }
    }

    private boolean isZip(BufferedInputStream stream)
            throws IOException
    {
        stream.mark(4);
        byte[] zipMagic = new byte[4];
        int read = ByteStreams.read(stream, zipMagic, 0, zipMagic.length);
        stream.reset();

        return Arrays.equals(zipMagic, ZIP_MAGIC) && read == zipMagic.length;
    }
}
