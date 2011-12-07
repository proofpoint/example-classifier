package com.proofpoint.example.classifier;

import com.google.common.collect.ImmutableSet;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class UsersOnHold
{
    private final AtomicReference<Set<String>> userIdsReference = new AtomicReference<Set<String>>(ImmutableSet.<String>of());

    public void replace(Set<String> userIds)
    {
        userIdsReference.set(ImmutableSet.copyOf(userIds));
    }

    public boolean contains(String userId)
    {
        return userIdsReference.get().contains(userId);
    }
}
