package com.proofpoint.example.classifier;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestCreditCardValidator
{
    @Test
    public void testValid()
    {
        assertTrue(new ClassifierResource(new UsersOnHold()).isValidCreditCard("5105105105105100"));
        assertTrue(new ClassifierResource(new UsersOnHold()).isValidCreditCard("5555555555554444"));
    }

    @Test
    public void testInvalid()
    {
        assertFalse(new ClassifierResource(new UsersOnHold()).isValidCreditCard("4020202020204"));
        assertFalse(new ClassifierResource(new UsersOnHold()).isValidCreditCard("4050305040603020"));
    }

}
