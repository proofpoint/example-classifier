package com.proofpoint.example.classifier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestCreditCardValidator
{

    private ClassifierResource classifierResource;

    @BeforeMethod
    protected void setUp()
            throws Exception
    {
        classifierResource = new ClassifierResource(new UsersOnHold(), new InMemoryClassificationStore());
    }

    @Test
    public void testValid()
    {
        assertTrue(classifierResource.isValidCreditCard("5105105105105100"));
        assertTrue(classifierResource.isValidCreditCard("5555555555554444"));
    }

    @Test
    public void testInvalid()
    {
        assertFalse(classifierResource.isValidCreditCard("4020202020204"));
        assertFalse(classifierResource.isValidCreditCard("4050305040603020"));
    }

}
