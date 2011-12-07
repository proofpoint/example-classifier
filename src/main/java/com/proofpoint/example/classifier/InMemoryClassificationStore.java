package com.proofpoint.example.classifier;

import com.google.common.collect.MapMaker;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class InMemoryClassificationStore implements ClassificationStore
{
    private final ConcurrentMap<String, Map<String, Number>> store = new MapMaker().makeMap();

    @Override
    public Map<String, Number> getClassifications(String dataHash)
    {
        return store.get(dataHash);
    }

    @Override
    public Map<String, Number> setClassifications(String dataHash, Map<String, Number> classifications)
    {
        return store.put(dataHash, classifications);
    }
}
