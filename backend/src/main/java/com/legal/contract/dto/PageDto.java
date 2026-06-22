package com.legal.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageDto {

    private Integer page = 1;

    private Integer size = 10;

    private String keyword;
}