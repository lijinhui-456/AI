package com.legal.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractGenerateDto {

    @NotBlank(message = "合同类型不能为空")
    private String contractType;

    @NotNull(message = "合同方信息不能为空")
    private List<PartyInfo> parties;

    @NotBlank(message = "标的物不能为空")
    private String subjectMatter;

    private String duration;

    private String amount;

    private String specialClauses;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartyInfo {

        private String name;

        private String role;
    }
}