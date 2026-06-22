package com.legal.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ContractQueryDto extends PageDto {

    private String status;

    private String type;

    private String keyword;

    private String sortField;

    private String sortOrder;
}