package com.proofpoint.example.classifier;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestClassifierResource
{
    private ClassifierResource classifier;
    private UsersOnHold usersOnHold;

    @BeforeMethod
    protected void setUp()
            throws Exception
    {
        usersOnHold = new UsersOnHold();
        classifier = new ClassifierResource(usersOnHold);
    }

    @Test
    public void testCreditCard()
            throws IOException
    {
        String document = "5555-5555-5555-4444";
        Map<String, Number> scores = classifier.post(null, toInputStream(document));

        assertEquals(scores, ImmutableMap.of("CreditCard", 100));
    }

    @Test
    public void testSSN()
            throws IOException
    {
        String document = "111-22-3333";
        Map<String, Number> scores = classifier.post(null, toInputStream(document));

        assertEquals(scores, ImmutableMap.of("SSN", 100));
    }

    @Test
    public void testCreditCardAndSSN()
            throws IOException
    {
        String document = "5555-5555-5555-4444 111-22-3333";
        Map<String, Number> actual = classifier.post(null, toInputStream(document));

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
        Map<String, Number> actual = classifier.post(null, Resources.newInputStreamSupplier(Resources.getResource("binaryWithCreditCard.bin")).getInput());

        Map<String, Number> expected = ImmutableMap.<String, Number>builder()
                .put("CreditCard", 100)
                .build();

        assertEquals(actual, expected);
    }

    @Test
    public void testSupportsWordDoc()
            throws IOException
    {
        Map<String, Number> actual = classifier.post(null, Resources.newInputStreamSupplier(Resources.getResource("PrivateData.docx")).getInput());

        Map<String, Number> expected = ImmutableMap.<String, Number>builder()
                .put("CreditCard", 100)
                .put("SSN", 100)
                .build();

        assertEquals(actual, expected);
    }


    @Test
    public void testEmptyWordDoc()
            throws IOException
    {
        Map<String, Number> actual = classifier.post(null, Resources.newInputStreamSupplier(Resources.getResource("EmptyDocument.docx")).getInput());

        assertEquals(actual, ImmutableMap.of());
    }

    @Test
    public void testUserOnHold()
            throws IOException
    {
        usersOnHold.replace(ImmutableSet.of("joe"));

        Map<String, Number> actual = classifier.post("joe", toInputStream(""));
        assertEquals(actual, ImmutableMap.of("Hold", 100));
    }

    @Test
    public void testUserNotOnHold()
            throws IOException
    {
        Map<String, Number> actual = classifier.post("joe", toInputStream(""));
        assertEquals(actual, ImmutableMap.of());
    }

    @Test
    public void testSeesChangeInUsersOnHold()
            throws IOException
    {
        assertEquals(classifier.post("joe", toInputStream("")), ImmutableMap.of());

        usersOnHold.replace(ImmutableSet.of("joe"));
        assertEquals(classifier.post("joe", toInputStream("")), ImmutableMap.of("Hold", 100));

        usersOnHold.replace(ImmutableSet.<String>of());
        assertEquals(classifier.post("joe", toInputStream("")), ImmutableMap.of());
    }

    @Test
    public void testMultipleUsersOnHold()
            throws IOException
    {
        usersOnHold.replace(ImmutableSet.of("joe", "sam"));
        assertEquals(classifier.post("joe", toInputStream("")), ImmutableMap.of("Hold", 100));
        assertEquals(classifier.post("sam", toInputStream("")), ImmutableMap.of("Hold", 100));
    }

    private ByteArrayInputStream toInputStream(String document)
    {
        return new ByteArrayInputStream(document.getBytes(Charsets.UTF_8));
    }
}
