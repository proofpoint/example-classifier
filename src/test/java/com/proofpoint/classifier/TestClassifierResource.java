package com.proofpoint.classifier;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestClassifierResource
{
    @Test
    public void testCreditCard()
            throws IOException
    {
        String document = "5555-5555-5555-4444";
        Map<String, Number> scores = new ClassifierResource().post(toInputStream(document));

        assertEquals(scores, ImmutableMap.of("CreditCard", 100));
    }

    @Test
    public void testSSN()
            throws IOException
    {
        String document = "111-22-3333";
        Map<String, Number> scores = new ClassifierResource().post(toInputStream(document));

        assertEquals(scores, ImmutableMap.of("SSN", 100));
    }

    @Test
    public void testCreditCardAndSSN()
            throws IOException
    {
        String document = "5555-5555-5555-4444 111-22-3333";
        Map<String, Number> actual = new ClassifierResource().post(toInputStream(document));

        Map<String, Number> expected = ImmutableMap.<String, Number>builder()
                .put("CreditCard", 100)
                .put("SSN", 100)
                .build();

        assertEquals(actual, expected);
    }

    @Test
    public void testExtractsTextFromBinary()
            throws IOException
    {
        Map<String, Number> actual = new ClassifierResource().post(Resources.newInputStreamSupplier(Resources.getResource("binaryWithCreditCard.bin")).getInput());

        Map<String, Number> expected = ImmutableMap.<String, Number>builder()
                .put("CreditCard", 100)
                .build();

        assertEquals(actual, expected);
    }

    @Test
    public void testSupportsWordDoc()
            throws IOException
    {
        Map<String, Number> actual = new ClassifierResource().post(Resources.newInputStreamSupplier(Resources.getResource("PrivateData.docx")).getInput());

        Map<String, Number> expected = ImmutableMap.<String, Number>builder()
                .put("CreditCard", 100)
                .put("SSN", 100)
                .build();

        assertEquals(actual, expected);
    }

    private ByteArrayInputStream toInputStream(String document)
    {
        return new ByteArrayInputStream(document.getBytes(Charsets.UTF_8));
    }
}
