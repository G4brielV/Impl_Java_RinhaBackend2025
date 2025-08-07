package com.rinha2025.__Imp_rinha2025.controller;

import com.rinha2025.__Imp_rinha2025.model.dto.PaymentSummaryResponseDTO;
import com.rinha2025.__Imp_rinha2025.service.PaymentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/payments-summary")
public class PaymentSummaryController {

    private final PaymentService paymentService;

    public PaymentSummaryController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public PaymentSummaryResponseDTO getPaymentSummary(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime  to)
    {
        return this.paymentService.getPaymentSummary(
                from != null ? from.toLocalDateTime() : null,
                to != null ? to.toLocalDateTime() : null
        );
    }
}
