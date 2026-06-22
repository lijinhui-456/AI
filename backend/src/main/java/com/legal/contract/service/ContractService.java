package com.legal.contract.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.legal.contract.common.BusinessException;
import com.legal.contract.dto.ContractQueryDto;
import com.legal.contract.entity.Contract;
import com.legal.contract.mapper.ContractMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractMapper contractMapper;

    @Value("${contract.upload.dir:./uploads/contracts}")
    private String uploadDir;

    /**
     * 上传合同文件
     */
    public Contract upload(MultipartFile file, Long userId, String title, String type) {
        log.info("上传合同文件: title={}, type={}, userId={}, fileName={}", title, type, userId, file.getOriginalFilename());

        try {
            // 确保上传目录存在
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String storedFilename = UUID.randomUUID().toString() + extension;

            // 保存文件到磁盘
            Path targetPath = Paths.get(uploadDir, storedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 提取文本内容
            String contentText = extractTextFromFile(file);

            // 创建合同记录
            Contract contract = new Contract();
            contract.setUserId(userId);
            contract.setTitle(title);
            contract.setType(type != null ? type : "other");
            contract.setFileName(originalFilename);
            contract.setFileUrl("/uploads/contracts/" + storedFilename);
            contract.setFileSize(file.getSize());
            contract.setContentText(contentText);
            contract.setStatus("UPLOADED");

            contractMapper.insert(contract);
            log.info("合同上传成功: contractId={}", contract.getId());

            return contract;

        } catch (Exception e) {
            log.error("合同上传失败", e);
            throw new BusinessException("合同上传失败: " + e.getMessage());
        }
    }

    /**
     * 从文件中提取文本内容
     */
    public String extractTextFromFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return "";
        }

        String lowerName = originalFilename.toLowerCase();

        try {
            if (lowerName.endsWith(".txt")) {
                // 直接读取文本文件
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
                return sb.toString();
            } else if (lowerName.endsWith(".docx")) {

                try (XWPFDocument document = new XWPFDocument(file.getInputStream());
                     XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    String text = extractor.getText();
                    log.info("Word文档文本提取成功, 长度={}", text.length());
                    return text != null ? text : "";
                }
            } else if (lowerName.endsWith(".doc")) {

                log.info("旧版 Word 文件 .doc 不支持直接解析，请转换为 .docx 或 .txt 格式");
                return "[提示：.doc 格式暂不支持解析，请上传 .docx、.txt 或 .pdf 格式文件]";
            } else {
                log.info("文件格式 {} 暂不支持直接解析", lowerName);
                return "[提示：不支持的文件格式，请上传 .docx、.txt 或 .pdf 格式文件]";
            }
        } catch (Exception e) {
            log.warn("提取文件文本内容失败: {}", e.getMessage());
            return "[提示：文件内容解析失败，请上传纯文本 .txt 格式文件]";
        }
    }


    public String extractTextFromFileUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return "";
        }
        // 从URL中提取文件名
        String filename = fileUrl;
        if (filename.contains("/")) {
            filename = filename.substring(filename.lastIndexOf("/") + 1);
        }
        // 拼接成绝对路径
        File file = new File(uploadDir, filename);
        if (!file.exists()) {
            log.warn("文件不存在: {}", file.getAbsolutePath());
            return "[提示：合同文件不存在，无法分析]";
        }

        String lowerName = filename.toLowerCase();
        try {
            if (lowerName.endsWith(".txt")) {
                return Files.readString(file.toPath(), StandardCharsets.UTF_8);
            } else if (lowerName.endsWith(".docx")) {
                try (XWPFDocument document = new XWPFDocument(Files.newInputStream(file.toPath()));
                     XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    String text = extractor.getText();
                    log.info("从磁盘解析Word文档成功, 长度={}", text.length());
                    return text != null ? text : "";
                }
            } else if (lowerName.endsWith(".doc")) {
                return "[提示：.doc 格式暂不支持解析，请转换为 .docx 或 .txt 格式]";
            } else {
                return "[提示：不支持的文件格式，请上传 .docx 或 .txt 格式文件]";
            }
        } catch (Exception e) {
            log.warn("从磁盘解析文件失败: {}", e.getMessage());
            return "[提示：文件内容解析失败]";
        }
    }

    /**
     * 分页查询合同
     */
    public IPage<Contract> queryPage(ContractQueryDto dto, Long userId) {
        Page<Contract> page = new Page<>(dto.getPage(), dto.getSize());

        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        // 只查询当前用户的合同
        wrapper.eq(Contract::getUserId, userId);

        // 按状态筛选
        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            wrapper.eq(Contract::getStatus, dto.getStatus());
        }

        // 按类型筛选
        if (dto.getType() != null && !dto.getType().isEmpty()) {
            wrapper.eq(Contract::getType, dto.getType());
        }

        // 按关键词搜索标题
        if (dto.getKeyword() != null && !dto.getKeyword().isEmpty()) {
            wrapper.like(Contract::getTitle, dto.getKeyword());
        }

        // 按创建时间倒序排列
        wrapper.orderByDesc(Contract::getCreatedTime);

        return contractMapper.selectPage(page, wrapper);
    }

    /**
     * 根据ID获取合同详情
     */
    public Contract getById(Long id) {
        Contract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw BusinessException.badRequest("合同不存在");
        }
        return contract;
    }

    /**
     * 软删除合同
     */
    public void delete(Long id, Long userId) {
        Contract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw BusinessException.badRequest("合同不存在");
        }
        if (!contract.getUserId().equals(userId)) {
            throw BusinessException.badRequest("无权删除此合同");
        }
        contractMapper.deleteById(id);
        log.info("合同已删除: contractId={}, userId={}", id, userId);
    }
}