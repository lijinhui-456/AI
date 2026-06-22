package com.legal.contract.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.contract.entity.ContractRisk;
import com.legal.contract.mapper.ContractRiskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractRiskService {

    private final ContractRiskMapper contractRiskMapper;

    /**
     * 根据合同ID获取风险列表
     */
    public List<ContractRisk> getRisksByContractId(Long contractId) {
        LambdaQueryWrapper<ContractRisk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContractRisk::getContractId, contractId);
        wrapper.orderByAsc(ContractRisk::getCreatedTime);
        return contractRiskMapper.selectList(wrapper);
    }

    /**
     * 批量保存风险
     */
    public void saveRisks(Long contractId, List<ContractRisk> risks) {
        if (risks == null || risks.isEmpty()) {
            return;
        }

        for (ContractRisk risk : risks) {
            risk.setContractId(contractId);
            contractRiskMapper.insert(risk);
        }
        log.info("合同风险保存完成: contractId={}, count={}", contractId, risks.size());
    }

    /**
     * 根据合同ID删除风险
     */
    public void deleteByContractId(Long contractId) {
        LambdaQueryWrapper<ContractRisk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContractRisk::getContractId, contractId);
        contractRiskMapper.delete(wrapper);
        log.info("合同风险已删除: contractId={}", contractId);
    }
}