package com.legal.contract.service.agent;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Milvus向量数据库服务
 * 提供法律知识文档的向量存储和语义检索能力
 */
@Slf4j
@Component
public class MilvusService {

    private static final String FIELD_ID = "id";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_TAGS = "tags";
    private static final String FIELD_EMBEDDING = "embedding";

    private static final int DEFAULT_DIMENSION = 1536;
    private static final int MAX_SEARCH_RESULTS = 10;

    private final MilvusServiceClient milvusClient;
    private final EmbeddingModel embeddingModel;

    @Value("${milvus.collection-name:legal_knowledge}")
    private String collectionName;

    @Value("${milvus.dimension:1536}")
    private int dimension;

    public MilvusService(MilvusServiceClient milvusClient, EmbeddingModel embeddingModel) {
        this.milvusClient = milvusClient;
        this.embeddingModel = embeddingModel;
    }

    @PostConstruct
    public void initCollection() {
        log.info("[Milvus] 初始化集合: {}", collectionName);
        try {
            HasCollectionParam hasParam = HasCollectionParam.newBuilder()
                    .withCollectionName(collectionName).build();
            R<Boolean> hasResponse = milvusClient.hasCollection(hasParam);
            if (hasResponse.getData() != null && hasResponse.getData()) {
                log.info("[Milvus] 集合已存在: {}", collectionName);
                return;
            }

            FieldType idField = FieldType.newBuilder()
                    .withName(FIELD_ID).withDataType(DataType.Int64)
                    .withPrimaryKey(true).withAutoID(false).build();
            FieldType titleField = FieldType.newBuilder()
                    .withName(FIELD_TITLE).withDataType(DataType.VarChar).withMaxLength(500).build();
            FieldType contentField = FieldType.newBuilder()
                    .withName(FIELD_CONTENT).withDataType(DataType.VarChar).withMaxLength(65535).build();
            FieldType tagsField = FieldType.newBuilder()
                    .withName(FIELD_TAGS).withDataType(DataType.VarChar).withMaxLength(500).build();
            FieldType embeddingField = FieldType.newBuilder()
                    .withName(FIELD_EMBEDDING).withDataType(DataType.FloatVector)
                    .withDimension(dimension > 0 ? dimension : DEFAULT_DIMENSION).build();

            CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withDescription("法律知识文档向量库")
                    .withShardsNum(2)
                    .addFieldType(idField).addFieldType(titleField)
                    .addFieldType(contentField).addFieldType(tagsField)
                    .addFieldType(embeddingField).build();

            R<?> createResp = milvusClient.createCollection(createParam);
            if (createResp.getException() != null) {
                log.error("[Milvus] 创建集合失败", createResp.getException());
                return;
            }
            log.info("[Milvus] 集合创建成功");

            CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFieldName(FIELD_EMBEDDING)
                    .withIndexType(IndexType.IVF_FLAT)
                    .withMetricType(MetricType.IP)
                    .withExtraParam("{\"nlist\":128}").build();
            milvusClient.createIndex(indexParam);

            milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                    .withCollectionName(collectionName).build());
            log.info("[Milvus] 集合加载完成");

        } catch (Exception e) {
            log.error("[Milvus] 初始化集合异常", e);
        }
    }

    public boolean insertDocument(Long docId, String title, String content, String tags) {
        if (milvusClient == null) {
            log.warn("[Milvus] 客户端不可用，跳过向量索引");
            return false;
        }
        log.info("[Milvus] 插入文档 docId={}", docId);
        try {
            Embedding embedding = embeddingModel.embed(content).content();
            List<String> fieldNames = Arrays.asList(FIELD_ID, FIELD_TITLE, FIELD_CONTENT, FIELD_TAGS, FIELD_EMBEDDING);
            List<List<?>> values = new ArrayList<>();
            values.add(Collections.singletonList(docId));
            values.add(Collections.singletonList(title != null ? title : ""));
            values.add(Collections.singletonList(content));
            values.add(Collections.singletonList(tags != null ? tags : ""));
            values.add(Collections.singletonList(embedding.vectorAsList()));

            List<InsertParam.Field> fields = new ArrayList<>();
            for (int i = 0; i < fieldNames.size(); i++) {
                fields.add(new InsertParam.Field(fieldNames.get(i), values.get(i)));
            }

            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(collectionName).withFields(fields).build();
            R<MutationResult> resp = milvusClient.insert(insertParam);
            if (resp.getException() != null) {
                log.error("[Milvus] 插入失败: {}", resp.getException().getMessage());
                return false;
            }
            log.info("[Milvus] 插入成功 count={}", resp.getData().getInsertCnt());
            return true;
        } catch (Exception e) {
            log.error("[Milvus] 插入异常", e);
            return false;
        }
    }
    /**
     * Build InsertParam.Field list compatible with various Milvus SDK versions.
     * In 2.3.4: new InsertParam.Field(name, values) takes 2 args.
     */
    private List<InsertParam.Field> buildInsertFields(List<String> names, List<List<?>> values) {
        List<InsertParam.Field> fields = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            fields.add(new InsertParam.Field(names.get(i), values.get(i)));
        }
        return fields;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchSimilar(String query, int topK) {
        log.info("[Milvus] 搜索 query={} topK={}", query, topK);
        if (topK <= 0 || topK > MAX_SEARCH_RESULTS) topK = MAX_SEARCH_RESULTS;
        try {
            Embedding queryEmb = embeddingModel.embed(query).content();
            List<List<Float>> vectors = Collections.singletonList(queryEmb.vectorAsList());

            SearchParam param = SearchParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withMetricType(MetricType.IP)
                    .withTopK(topK)
                    .withVectors(vectors)
                    .withVectorFieldName(FIELD_EMBEDDING)
                    .withOutFields(Arrays.asList(FIELD_ID, FIELD_TITLE, FIELD_CONTENT, FIELD_TAGS))
                    .withConsistencyLevel(ConsistencyLevelEnum.EVENTUALLY)
                    .build();

            R<SearchResults> resp = milvusClient.search(param);
            if (resp.getException() != null) {
                log.error("[Milvus] 搜索失败", resp.getException());
                return Collections.emptyList();
            }

            SearchResultsWrapper wrapper = new SearchResultsWrapper(resp.getData().getResults());
            List<Map<String, Object>> results = new ArrayList<>();

            // Milvus 2.3.x SDK: get row records from result group 0
            List<SearchResultsWrapper.IDScore> idScores = wrapper.getIDScore(0);
            if (idScores != null) {
                for (SearchResultsWrapper.IDScore idScore : idScores) {
                    Map<String, Object> doc = new HashMap<>();
                    // IDScore has getStrField() for String fields and getFloatField() for score
                    doc.put("title", getFieldSafely(idScore, FIELD_TITLE));
                    doc.put("content", getFieldSafely(idScore, FIELD_CONTENT));
                    doc.put("tags", getFieldSafely(idScore, FIELD_TAGS));
                    doc.put("score", idScore.getScore());
                    results.add(doc);
                }
            }

            log.info("[Milvus] 搜索完成 返回{}条", results.size());
            return results;
        } catch (Exception e) {
            log.error("[Milvus] 搜索异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * Safely get string field from IDScore, trying multiple method names
     * for compatibility across Milvus SDK versions.
     */
    private String getFieldSafely(SearchResultsWrapper.IDScore idScore, String fieldName) {
        if (idScore == null) return "";
        try {
            // Try getStrField (Milvus 2.3.x)
            Object val = idScore.get(fieldName);
            if (val != null) return val.toString();
        } catch (Exception ignored) {
        }
        try {
            // Fallback: try reflection-based getField
            Object val = idScore.get(fieldName);
            if (val != null) return val.toString();
        } catch (Exception ignored) {
        }
        return "";
    }

    public void deleteDocument(Long docId) {
        log.info("[Milvus] 删除文档 docId={}", docId);
        try {
            DeleteParam deleteParam = DeleteParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withExpr("id in [" + docId + "]")
                    .build();
            R<MutationResult> resp = milvusClient.delete(deleteParam);
            if (resp.getException() != null) {
                log.error("[Milvus] 删除失败", resp.getException());
            } else {
                log.info("[Milvus] 删除成功 count={}", resp.getData().getDeleteCnt());
            }
        } catch (Exception e) {
            log.error("[Milvus] 删除异常", e);
        }
    }
}