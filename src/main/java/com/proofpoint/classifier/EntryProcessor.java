package com.proofpoint.classifier;

import java.io.IOException;
import java.io.InputStream;

public interface EntryProcessor
{
    void process(InputStream entryStream)
            throws IOException;
}
