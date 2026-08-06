package com.obs.backend.feature.statement.controller;

import com.obs.backend.feature.statement.service.StatementService;
import com.obs.backend.security.CurrentUserProvider;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
public class StatementController {

    private final StatementService statementService;
    private final CurrentUserProvider currentUserProvider;

    public StatementController(StatementService statementService, CurrentUserProvider currentUserProvider) {
        this.statementService = statementService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/{accountId}/statement")
    public ResponseEntity<byte[]> getStatement(
            @PathVariable UUID accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        byte[] pdf = statementService.generateStatement(
                currentUserProvider.currentUserId(), accountId, fromDate, toDate);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("statement-" + accountId + ".pdf")
                                .build()
                                .toString())
                .body(pdf);
    }
}
