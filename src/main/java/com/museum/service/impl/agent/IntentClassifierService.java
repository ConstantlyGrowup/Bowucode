package com.museum.service.impl.agent;

import com.museum.domain.enums.IntentType;

public interface IntentClassifierService {
    IntentType classify(String userInput);
}


