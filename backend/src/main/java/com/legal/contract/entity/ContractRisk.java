package com.legal.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("contract_risk")
public class ContractRisk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long contractId;

    private String clause;

    private String riskType;

    private String riskLevel;

    private String description;

    private String suggestion;

    private String position;

    private LocalDateTime createdTime;
}