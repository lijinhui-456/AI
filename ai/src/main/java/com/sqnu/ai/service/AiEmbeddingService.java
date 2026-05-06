package com.sqnu.ai.service;

import java.io.IOException;
import java.util.List;

public interface AiEmbeddingService {
    public void add() throws IOException;
    public List<String> search(String query);
}
