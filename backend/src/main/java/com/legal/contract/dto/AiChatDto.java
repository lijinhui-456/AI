package com.legal.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatDto {

    private String sessionId;

    private String content;

    private Long contractId;
}