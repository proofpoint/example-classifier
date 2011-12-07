package com.proofpoint.classifier;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;

public class TestStreamFlattener
{
    @Test
    public void testHandlesSimpleStream()
            throws IOException
    {

        final AtomicInteger entryCount = new AtomicInteger(0);

        final byte[] expected = { 1, 2, 3, 4 };
        InputStream stream = ByteStreams.newInputStreamSupplier(expected).getInput();
        new StreamFlattener().flatten(stream, new EntryProcessor()
        {
            @Override
            public void process(InputStream entryStream)
                    throws IOException
            {
                byte[] actual = new byte[expected.length];
                ByteStreams.readFully(entryStream, actual);

                assertEquals(actual, expected);
                entryCount.incrementAndGet();
            }
        });

        assertEquals(entryCount.get(), 1);
    }

    @Test
    public void testHandlesFlatZip()
            throws IOException
    {
        List<String> expectedEntries = ImmutableList.of("1\n", "2\n", "3\n");
        final List<String> actualEntries = Lists.newArrayList();

        InputStream stream = Resources.newInputStreamSupplier(Resources.getResource("test.zip")).getInput();
        new StreamFlattener().flatten(stream, new EntryProcessor()
        {
            @Override
            public void process(InputStream entryStream)
                    throws IOException
            {
                actualEntries.add(CharStreams.toString(new InputStreamReader(entryStream, Charsets.UTF_8)));
            }
        });

        assertEquals(actualEntries, expectedEntries);
    }

    @Test
    public void testHandlesNestedZip()
            throws IOException
    {
        List<String> expectedEntries = ImmutableList.of("1\n", "2\n", "3\n", "1\n", "2\n", "3\n");
        final List<String> actualEntries = Lists.newArrayList();

        InputStream stream = Resources.newInputStreamSupplier(Resources.getResource("nested.zip")).getInput();
        new StreamFlattener().flatten(stream, new EntryProcessor()
        {
            @Override
            public void process(InputStream entryStream)
                    throws IOException
            {
                actualEntries.add(CharStreams.toString(new InputStreamReader(entryStream, Charsets.UTF_8)));
            }
        });

        assertEquals(actualEntries, expectedEntries);
    }

}
