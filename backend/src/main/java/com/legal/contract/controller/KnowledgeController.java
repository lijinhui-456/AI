package com.legal.contract.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.legal.contract.common.Result;
import com.legal.contract.dto.PageDto;
import com.legal.contract.entity.KnowledgeDoc;
import com.legal.contract.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @PostMapping
    public Result<KnowledgeDoc> create(@RequestBody KnowledgeDoc doc) {
        KnowledgeDoc created = knowledgeService.create(doc);
        return Result.success(created);
    }

    @PutMapping
    public Result<KnowledgeDoc> update(@RequestBody KnowledgeDoc doc) {
        KnowledgeDoc updated = knowledgeService.update(doc);
        return Result.success(updated);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return Result.success();
    }

    @GetMapping("/page")
    public Result<Result.Pager<KnowledgeDoc>> page(PageDto dto) {
        IPage<KnowledgeDoc> pageResult = knowledgeService.queryPage(dto);
        Result.Pager<KnowledgeDoc> pager = Result.Pager.<KnowledgeDoc>builder()
                .page((int) pageResult.getCurrent())
                .size((int) pageResult.getSize())
                .total(pageResult.getTotal())
                .records(pageResult.getRecords())
                .build();
        return Result.success(pager);
    }

    @GetMapping("/search")
    public Result<Result.Pager<KnowledgeDoc>> search(@RequestParam String keyword,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        IPage<KnowledgeDoc> pageResult = knowledgeService.search(keyword, page, size);
        Result.Pager<KnowledgeDoc> pager = Result.Pager.<KnowledgeDoc>builder()
                .page((int) pageResult.getCurrent())
                .size((int) pageResult.getSize())
                .total(pageResult.getTotal())
                .records(pageResult.getRecords())
                .build();
        return Result.success(pager);
    }

    @GetMapping("/{id}")
    public Result<KnowledgeDoc> getById(@PathVariable Long id) {
        KnowledgeDoc doc = knowledgeService.getById(id);
        return Result.success(doc);
    }
}