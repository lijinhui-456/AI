package com.legal.contract.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.legal.contract.common.BusinessException;
import com.legal.contract.dto.PageDto;
import com.legal.contract.entity.KnowledgeDoc;
import com.legal.contract.mapper.KnowledgeDocMapper;
import com.legal.contract.service.agent.MilvusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeDocMapper knowledgeDocMapper;
    private final MilvusService milvusService;

    /**
     * 创建知识文档
     */
    public KnowledgeDoc create(KnowledgeDoc doc) {
        log.info("创建知识文档: title={}", doc.getTitle());

        if (doc.getStatus() == null) {
            doc.setStatus("ENABLED");
        }


        knowledgeDocMapper.insert(doc);


        boolean milvusOk = false;
        try {
            milvusOk = milvusService.insertDocument(doc.getId(), doc.getTitle(), doc.getContent(), doc.getTags());
        } catch (Exception e) {
            log.warn("知识文档索引到Milvus失败: id={}, error={}", doc.getId(), e.getMessage());
        }

        if (milvusOk) {
            log.info("知识文档创建成功: id={}, 已同步到Milvus向量索引", doc.getId());
        } else {
            log.info("知识文档创建成功: id={}, Milvus不可用，原文已保存在MySQL content字段", doc.getId());
        }
        return doc;
    }

    /**
     * 更新知识文档
     */
    public KnowledgeDoc update(KnowledgeDoc doc) {
        log.info("更新知识文档: id={}", doc.getId());

        KnowledgeDoc existing = knowledgeDocMapper.selectById(doc.getId());
        if (existing == null) {
            throw BusinessException.badRequest("知识文档不存在");
        }

        knowledgeDocMapper.updateById(doc);

        // 更新Milvus中的数据（先删除再插入）
        try {
            milvusService.deleteDocument(doc.getId());
            milvusService.insertDocument(doc.getId(), doc.getTitle(), doc.getContent(), doc.getTags());
        } catch (Exception e) {
            log.warn("知识文档更新Milvus索引失败: id={}, error={}", doc.getId(), e.getMessage());
        }

        log.info("知识文档更新成功: id={}", doc.getId());
        return doc;
    }

    /**
     * 删除知识文档（软删除）
     */
    public void delete(Long id) {
        log.info("删除知识文档: id={}", id);

        KnowledgeDoc existing = knowledgeDocMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.badRequest("知识文档不存在");
        }


        knowledgeDocMapper.deleteById(id);

        // 从Milvus删除
        try {
            milvusService.deleteDocument(id);
        } catch (Exception e) {
            log.warn("从Milvus删除知识文档索引失败: id={}, error={}", id, e.getMessage());
        }

        log.info("知识文档删除成功: id={}", id);
    }

    /**
     * 分页查询知识文档
     */
    public IPage<KnowledgeDoc> queryPage(PageDto dto) {
        Page<KnowledgeDoc> page = new Page<>(dto.getPage(), dto.getSize());

        LambdaQueryWrapper<KnowledgeDoc> wrapper = new LambdaQueryWrapper<>();
        if (dto.getKeyword() != null && !dto.getKeyword().isEmpty()) {
            wrapper.like(KnowledgeDoc::getTitle, dto.getKeyword())
                    .or()
                    .like(KnowledgeDoc::getContent, dto.getKeyword());
        }
        wrapper.orderByDesc(KnowledgeDoc::getCreatedTime);

        return knowledgeDocMapper.selectPage(page, wrapper);
    }

    /**
     * 搜索知识文档（按标题和内容）
     */
    public IPage<KnowledgeDoc> search(String keyword, int page, int size) {
        Page<KnowledgeDoc> pageObj = new Page<>(page, size);

        LambdaQueryWrapper<KnowledgeDoc> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(KnowledgeDoc::getTitle, keyword)
                .or()
                .like(KnowledgeDoc::getContent, keyword)
                .or()
                .like(KnowledgeDoc::getTags, keyword);
        wrapper.orderByDesc(KnowledgeDoc::getCreatedTime);

        return knowledgeDocMapper.selectPage(pageObj, wrapper);
    }

    /**
     * 根据ID获取知识文档
     */
    public KnowledgeDoc getById(Long id) {
        KnowledgeDoc doc = knowledgeDocMapper.selectById(id);
        if (doc == null) {
            throw BusinessException.badRequest("知识文档不存在");
        }
        return doc;
    }
}