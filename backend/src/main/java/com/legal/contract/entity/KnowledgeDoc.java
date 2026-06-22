package com.legal.contract.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.legal.contract.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_doc")
public class KnowledgeDoc extends BaseEntity {

    private String title;

    private String category;

    private String content;

    private String tags;

    private String fileName;

    private String fileUrl;

    private String status;
}