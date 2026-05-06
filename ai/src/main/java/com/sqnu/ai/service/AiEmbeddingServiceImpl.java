package com.sqnu.ai.service;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiEmbeddingServiceImpl implements AiEmbeddingService {
    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private EmbeddingStore embeddingStore;
    @Override
    public void add()  {
         try {
            ClassPathResource resource = new ClassPathResource("text.txt");
            String text = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            Document document = Document.from(text);
            DocumentBySentenceSplitter splitter = new DocumentBySentenceSplitter(500, 100);
            List<TextSegment> segments = splitter.split(document);
            for (TextSegment segment : segments) {
                Embedding embedding = embeddingModel.embed(segment).content();
                embeddingStore.add(embedding, segment);
            }

            System.out.println(" 文档入库完成！");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> search(String query) {
        Response<Embedding> embed = embeddingModel.embed(query);
        EmbeddingSearchRequest request=EmbeddingSearchRequest.builder()
                .queryEmbedding(embed.content())
                .maxResults(3)
                .build();
        ArrayList<String> results = new ArrayList<>();
        EmbeddingSearchResult search = embeddingStore.search(request);
        List<EmbeddingMatch<TextSegment>> result=search.matches();
        for (EmbeddingMatch<TextSegment> match : result) {
            if(match.score()>0.5){
                String text=match.embedded().text();
                results.add(text);
                System.out.println("搜到相关文本"+text);
            }
        }
        return results;
    }
}
