package com.legal.contract.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.legal.contract.common.Result;
import com.legal.contract.dto.ContractQueryDto;
import com.legal.contract.entity.Contract;
import com.legal.contract.entity.ContractRisk;
import com.legal.contract.service.ContractRiskService;
import com.legal.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/contract")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;
    private final ContractRiskService contractRiskService;

    /**
     * 上传合同文件
     */
    @PostMapping("/upload")
    public Result<Contract> upload(@RequestParam("file") MultipartFile file,
                                   @RequestParam("title") String title,
                                   @RequestParam(defaultValue = "other") String type,
                                   @RequestAttribute Long userId) {
        Contract contract = contractService.upload(file, userId, title, type);
        return Result.success(contract);
    }

    /**
     * 分页查询合同列表
     */
    @GetMapping("/page")
    public Result<Result.Pager<Contract>> page(ContractQueryDto dto,
                                               @RequestAttribute Long userId) {
        IPage<Contract> pageResult = contractService.queryPage(dto, userId);
        Result.Pager<Contract> pager = Result.Pager.<Contract>builder()
                .page((int) pageResult.getCurrent())
                .size((int) pageResult.getSize())
                .total(pageResult.getTotal())
                .records(pageResult.getRecords())
                .build();
        return Result.success(pager);
    }

    /**
     * 获取合同详情
     */
    @GetMapping("/{id}")
    public Result<Contract> getById(@PathVariable Long id) {
        Contract contract = contractService.getById(id);
        return Result.success(contract);
    }

    /**
     * 删除合同（软删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestAttribute Long userId) {
        contractService.delete(id, userId);
        return Result.success();
    }

    /**
     * 获取合同风险列表
     */
    @GetMapping("/{id}/risks")
    public Result<List<ContractRisk>> getContractRisks(@PathVariable Long id) {
        List<ContractRisk> risks = contractRiskService.getRisksByContractId(id);
        return Result.success(risks);
    }
}