package com.museum.service.impl.agent;

import com.museum.domain.po.MsCollection;
import com.museum.mapper.CollectionMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class VectorSyncService {

    private static final Logger log = LoggerFactory.getLogger(VectorSyncService.class);

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    @Resource
    private QwenEmbeddingModel embeddingModel;

    @Resource
    private CollectionMapper collectionMapper;

    @Value("${langchain4j.chroma.enabled:false}")
    private boolean chromaEnabled;

    @Value("${langchain4j.chroma.host:http://localhost}")
    private String chromaHost;

    @Value("${langchain4j.chroma.port:8000}")
    private int chromaPort;

    @Value("${langchain4j.chroma.collection-name:museum-collection}")
    private String collectionName;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    /**
     * 检查向量数据库是否已初始化
     */
    public boolean isVectorDataInitialized() {
        if (!chromaEnabled) {
            // 内存存储无法持久化检查，总是需要重新初始化
            return false;
        }

        try {
            // 1. 先获取集合信息，获得ID
            String baseUrl = buildBaseUrl();
            String collectionUrl = baseUrl + "api/v1/collections/" + collectionName;
            
            Request collectionRequest = new Request.Builder()
                    .url(collectionUrl)
                    .get()
                    .build();

            String collectionId = null;
            try (Response response = httpClient.newCall(collectionRequest).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    log.debug("Collection info: {}", responseBody);
                    
                    // 简单的JSON解析获取ID
                    int idStart = responseBody.indexOf("\"id\":\"") + 6;
                    int idEnd = responseBody.indexOf("\"", idStart);
                    if (idStart > 5 && idEnd > idStart) {
                        collectionId = responseBody.substring(idStart, idEnd);
                        log.debug("Found collection ID: {}", collectionId);
                    }
                } else {
                    log.info("集合 {} 不存在，需要初始化", collectionName);
                    return false;
                }
            }

            if (collectionId == null) {
                log.info("无法获取集合ID，将重新初始化");
                return false;
            }

            // 2. 使用集合ID查询数据数量
            String countUrl = baseUrl + "api/v1/collections/" + collectionId + "/count";
            Request countRequest = new Request.Builder()
                    .url(countUrl)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(countRequest).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    log.debug("Collection count response: {}", responseBody);
                    
                    try {
                        int count = Integer.parseInt(responseBody.trim());
                        log.info("向量数据库中已有 {} 条数据", count);
                        return count > 0;
                    } catch (NumberFormatException e) {
                        log.warn("无法解析数据数量: {}", responseBody);
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("检查向量数据初始化状态失败: {}", e.getMessage());
        }
        
        log.info("无法确定向量数据库状态，将重新初始化");
        return false;
    }

    /**
     * 初始化向量数据库（首次启动时使用，不删除现有数据）
     */
    public void initializeVectorData() {
        try {
            log.info("开始初始化向量数据库...");
            
            // 直接加载所有藏品，不删除现有数据
            List<MsCollection> collections = collectionMapper.selectList(null);
            
            if (collections.isEmpty()) {
                log.info("数据库中没有藏品信息，使用默认数据");
                initializeDefaultData();
                return;
            }

            // 批量添加藏品向量
            for (MsCollection collection : collections) {
                try {
                    String text = buildCollectionText(collection);
                    TextSegment segment = TextSegment.from(text);
                    Embedding embedding = embeddingModel.embed(segment).content();
                    
                    embeddingStore.add(embedding, segment);
                    log.debug("已添加藏品向量: {}", collection.getTitle());
                } catch (Exception e) {
                    log.warn("添加藏品向量失败 [{}]: {}", collection.getTitle(), e.getMessage());
                }
            }
            
            log.info("成功初始化 {} 个藏品到向量数据库", collections.size());
        } catch (Exception e) {
            log.error("初始化向量数据库失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 重新同步所有藏品到向量数据库（用于数据变更后的完全重建）
     */
    public void syncAllCollections() {
        try {
            log.info("开始重新同步所有藏品到向量数据库...");
            
            // 1. 清空现有数据
            clearVectorData();
            
            // 2. 重新初始化
            initializeVectorData();
            
        } catch (Exception e) {
            log.error("重新同步藏品到向量数据库失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 清空向量数据库中的所有数据
     */
    private void clearVectorData() {
        if (!chromaEnabled) {
            log.debug("使用内存存储，无需清空数据");
            return;
        }

        try {
            // 1. 先检查集合是否存在
            if (!checkCollectionExists()) {
                log.info("Chroma 集合 {} 不存在，跳过删除操作", collectionName);
                return;
            }

            // 2. 如果存在，则删除集合
            String baseUrl = buildBaseUrl();
            String url = baseUrl + "api/v1/collections/" + collectionName + 
                        "?tenant=default_tenant&database=default_database";
            
            Request request = new Request.Builder()
                    .url(url)
                    .delete()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.info("成功删除 Chroma 集合: {}", collectionName);
                } else {
                    log.warn("删除 Chroma 集合失败，状态码: {}, 响应: {}", 
                            response.code(), 
                            response.body() != null ? response.body().string() : "无响应体");
                }
            }
        } catch (Exception e) {
            log.warn("清空向量数据失败: {}", e.getMessage());
        }
    }

    /**
     * 检查Chroma集合是否存在
     */
    private boolean checkCollectionExists() {
        try {
            String baseUrl = buildBaseUrl();
            String collectionUrl = baseUrl + "api/v1/collections/" + collectionName;
            
            Request collectionRequest = new Request.Builder()
                    .url(collectionUrl)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(collectionRequest).execute()) {
                if (response.isSuccessful()) {
                    log.debug("Chroma 集合 {} 存在", collectionName);
                    return true;
                } else if (response.code() == 404) {
                    log.debug("Chroma 集合 {} 不存在", collectionName);
                    return false;
                } else {
                    log.warn("检查集合存在性失败，状态码: {}", response.code());
                    return false;
                }
            }
        } catch (Exception e) {
            log.debug("检查集合存在性时发生异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 初始化默认数据
     */
    private void initializeDefaultData() {
        try {
            String[] sampleTexts = {
                "藏品名称：香妃链",
                "来源：清-北京",
                    "品类：玉器",
                "描述：香妃链据传是清朝乾隆年间，由宫廷工匠精心打造，专为香妃伊帕尔罕所制。这串项链由珍贵的宝石和珍珠串成，每一颗宝石都经过精心挑选，以确保其色泽和光泽能够衬托出香妃的绝世容颜。项链的设计融合了满族和维吾尔族的元素，体现了当时多民族融合的文化特色。",
                "展厅：展厅四",
                    "简介：一条由宝石、珍珠构造的手链。承载着太多的记忆和故事。"
            };

            for (String text : sampleTexts) {
                TextSegment segment = TextSegment.from(text);
                Embedding embedding = embeddingModel.embed(segment).content();
                embeddingStore.add(embedding, segment);
            }
            
            log.info("成功初始化 {} 个默认向量数据", sampleTexts.length);
        } catch (Exception e) {
            log.error("初始化默认数据失败: {}", e.getMessage());
        }
    }

    private String buildCollectionText(MsCollection collection) {
        return String.format("""
            藏品名称：%s
            来源：%s
            品类：%s
            描述：%s
            展厅：%s
            简介：%s
            """, 
            collection.getTitle() != null ? collection.getTitle() : "未知",
            collection.getOrigin() != null ? collection.getOrigin() : "未知",
                collection.getCateId() != null ? collection.getCateId() : "未知",
            collection.getDesColl() != null ? collection.getDesColl() : "暂无描述",
            collection.getDisplayRoom() != null ? collection.getDisplayRoom() : "未指定",
            collection.getBase() != null ? collection.getBase() : "未知简介"
        );
    }

    private String buildBaseUrl() {
        String host = chromaHost;
        if (!host.startsWith("http://") && !host.startsWith("https://")) {
            host = "http://" + host;
        }
        String baseUrl = host + ":" + chromaPort;
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        return baseUrl;
    }
}
