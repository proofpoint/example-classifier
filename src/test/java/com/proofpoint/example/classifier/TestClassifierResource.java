package com.proofpoint.example.classifier;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class TestClassifierResource
{
    private ClassifierResource classifier;
    private UsersOnHold usersOnHold;
    private InMemoryClassificationStore classificationStore;

    @BeforeMethod
    protected void setUp()
            throws Exception
    {
        usersOnHold = new UsersOnHold();
        classificationStore = new InMemoryClassificationStore();
        classifier = new ClassifierResource(usersOnHold, classificationStore);
    }

    @Test
    public void testCreditCard()
            throws IOException
    {
        String document = "5555-5555-5555-4444";
        Map<String, Number> scores = classifier.classify(null, null, toInputStream(document));

        assertClassification(scores, ImmutableMap.of("CreditCard", 100));
    }

    @Test
    public void testSSN()
            throws IOException
    {
        String document = "111-22-3333";
        Map<String, Number> scores = classifier.classify(null, null, toInputStream(document));

        assertClassification(scores, ImmutableMap.of("SSN", 100));
    }

    @Test
    public void testCreditCardAndSSN()
            throws IOException
    {
        String document = "5555-5555-5555-4444 111-22-3333";
        Map<String, Number> actual = classifier.classify(null, null, toInputStream(document));

        Map<String, Number> expected = ImmutableMap.<String, Number>builder()
                .put("CreditCard", 100)
                .put("SSN", 100)
                .build();

        assertClassification(actual, expected);
    }

    @Test
    public void testExtractsTextFromBinary()
            throws IOException
    {
        Map<String, Number> actual = classifier.classify(null, null, Resources.newInputStreamSupplier(Resources.getResource("binaryWithCreditCard.bin")).getInput());

        Map<String, Number> expected = ImmutableMap.<String, Number>builder()
                .put("CreditCard", 100)
                .build();

        assertClassification(actual, expected);
    }

    @Test
    public void testSupportsWordDoc()
            throws IOException
    {
        Map<String, Number> actual = classifier.classify(null, null, Resources.newInputStreamSupplier(Resources.getResource("PrivateData.docx")).getInput());

        Map<String, Number> expected = ImmutableMap.<String, Number>builder()
                .put("CreditCard", 100)
                .put("SSN", 100)
                .build();

        assertClassification(actual, expected);
    }

    @Test
    public void testEmptyWordDoc()
            throws IOException
    {
        Map<String, Number> actual = classifier.classify(null, null, Resources.newInputStreamSupplier(Resources.getResource("EmptyDocument.docx")).getInput());
        assertClassification(actual, ImmutableMap.<String, Number>of());
    }

    @Test
    public void testUserOnHold()
            throws IOException
    {
        usersOnHold.replace(ImmutableSet.of("joe"));

        Map<String, Number> actual = classifier.classify("joe", null, toInputStream(""));
        assertClassification(actual, ImmutableMap.of("Hold", 100));
    }

    @Test
    public void testUserNotOnHold()
            throws IOException
    {
        Map<String, Number> actual = classifier.classify("joe", null, toInputStream(""));
        assertClassification(actual, ImmutableMap.<String, Number>of());
    }

    @Test
    public void testSeesChangeInUsersOnHold()
            throws IOException
    {
        assertClassification(classifier.classify("joe", null, toInputStream("")), ImmutableMap.<String, Number>of());

        usersOnHold.replace(ImmutableSet.of("joe"));
        assertClassification(classifier.classify("joe", null, toInputStream("")), ImmutableMap.of("Hold", 100));

        usersOnHold.replace(ImmutableSet.<String>of());
        assertClassification(classifier.classify("joe", null, toInputStream("")), ImmutableMap.<String, Number>of());
    }

    @Test
    public void testMultipleUsersOnHold()
            throws IOException
    {
        usersOnHold.replace(ImmutableSet.of("joe", "sam"));
        assertClassification(classifier.classify("joe", null, toInputStream("")), ImmutableMap.of("Hold", 100));
        assertClassification(classifier.classify("sam", null, toInputStream("")), ImmutableMap.of("Hold", 100));
    }

    @Test
    public void testStoredClassification()
            throws IOException
    {
        String document = "5555-5555-5555-4444";
        String hash = "my-hash";
        Map<String, Number> scores = classifier.classify(null, hash, toInputStream(document));
        assertNotNull(scores.containsKey("Random"), "Expected actual to contain a random entry: " + scores);

        // reclassify the document several times to assure results have been stored
        for (int i = 0; i < 100; i++) {
            assertEquals(classifier.classify(null, hash, toInputStream(document)), scores);
        }

        // verify the scores are in the classification score
        assertEquals(classificationStore.getClassifications(hash), scores);
    }

    @Test
    public void testStoredClassificationWithHold()
            throws IOException
    {
        usersOnHold.replace(ImmutableSet.of("joe", "sam"));

        String document = "5555-5555-5555-4444";
        String hash = "my-hash";

        // score initial document with a user on hold
        Map<String, Number> scores = newHashMap(classifier.classify("joe", hash, toInputStream(document)));
        assertNotNull(scores.containsKey("Random"), "Expected actual to contain a random entry: " + scores);
        assertNotNull(scores.containsKey("Hold"), "Expected actual to contain a random entry: " + scores);
        // remove the hold classification to get back to "pure" scores
        scores.remove("Hold");

        // reclassify the document several times with different users to assure results have been stored, and are not polluted with user specific classifications
        for (int i = 0; i < 100; i++) {
            assertEquals(classifier.classify(null, hash, toInputStream(document)), scores);
            assertEquals(classifier.classify("joe", hash, toInputStream(document)), ImmutableMap.builder().putAll(scores).put("Hold", 100).build());
            assertEquals(classifier.classify("sam", hash, toInputStream(document)), ImmutableMap.builder().putAll(scores).put("Hold", 100).build());
            assertEquals(classifier.classify("bob", hash, toInputStream(document)), scores);
        }

        // verify the scores are in the classification score
        assertEquals(classificationStore.getClassifications(hash), scores);
    }

    private void assertClassification(Map<String, ? extends Number> actual, Map<String, ? extends Number> expected)
    {
        Map<String, Number> actualWithoutRandom = newHashMap(actual);
        Number random = actualWithoutRandom.remove("Random");
        assertNotNull(random, "Expected actual to contain a random entry: " + actual);
        assertEquals(actualWithoutRandom, expected);
    }

    private ByteArrayInputStream toInputStream(String document)
    {
        return new ByteArrayInputStream(document.getBytes(Charsets.UTF_8));
    }
}
