package com.legal.contract.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.legal.contract.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contract")
public class Contract extends BaseEntity {

    private Long userId;

    private String title;

    private String type;

    private String fileName;

    private String fileUrl;

    private Long fileSize;

    private String contentText;

    private String status;

    private String riskLevel;

    private Integer riskScore;
}