package com.exambank.exam;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.exambank.common.ApiResponse;
import com.exambank.common.exception.DocumentProcessingException;
import com.exambank.exam.dto.ExamResponse;
import com.exambank.exam.dto.ExtractionResponse;
import com.exambank.exam.dto.QuestionResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;
    private final ExtractionService extractionService;

    public ExamController(ExamService examService, ExtractionService extractionService) {
        this.examService = examService;
        this.extractionService = extractionService;
    }

    @PostMapping(path = "/illustrative", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ExamResponse>> uploadIllustrative(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name) {

        if (file.isEmpty()) {
            throw new DocumentProcessingException("Uploaded file is empty");
        }
        String examName = StringUtils.hasText(name) ? name : file.getOriginalFilename();
        ExamPaper exam = examService.createIllustrative(examName, readBytes(file));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(ExamResponse.from(exam)));
    }

    @PostMapping("/{id}/extract")
    public ApiResponse<ExtractionResponse> extract(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "TESSERACT") ExtractionMethod method) {
        List<Question> questions = extractionService.extract(id, method);
        List<QuestionResponse> dtos = questions.stream().map(QuestionResponse::from).toList();
        return ApiResponse.ok(new ExtractionResponse(id, dtos.size(), dtos));
    }

    @GetMapping("/{id}/questions")
    public ApiResponse<List<QuestionResponse>> questions(@PathVariable UUID id) {
        List<QuestionResponse> dtos = examService.getQuestions(id).stream()
                .map(QuestionResponse::from).toList();
        return ApiResponse.ok(dtos);
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to read uploaded file", e);
        }
    }
}
