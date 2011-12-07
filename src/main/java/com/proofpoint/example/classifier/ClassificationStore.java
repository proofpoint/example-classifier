package com.proofpoint.example.classifier;

import java.util.Map;

public interface ClassificationStore
{
    Map<String, Number> getClassifications(String dataHash);
    Map<String, Number> setClassifications(String dataHash, Map<String, Number> classifications);
}
