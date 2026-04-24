package com.innowise.paymentservice.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.innowise.paymentservice.dto.request.AdvancedPaymentSearchFilter;
import com.innowise.paymentservice.dto.request.CreatePaymentRequest;
import com.innowise.paymentservice.dto.request.PaymentSearchFilter;
import com.innowise.paymentservice.dto.response.PaymentResponse;
import com.innowise.paymentservice.security.SecurityUtils;
import com.innowise.paymentservice.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.security.access.prepost.PreAuthorize;
import com.innowise.paymentservice.aspect.Idempotent;


@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController implements PaymentControllerApi {

    private final PaymentService paymentService;

    @PostMapping
    @Idempotent
    public ResponseEntity<PaymentResponse> createPayment(
        @RequestBody @Valid CreatePaymentRequest request) {

        PaymentResponse response = paymentService.createPayment(
            SecurityUtils.getCurrentUserId(),request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}" )
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.getPaymentById(
            id, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping
    public ResponseEntity<Page<PaymentResponse>> getPayments(
        @Valid @ModelAttribute PaymentSearchFilter filter,
        Pageable pageable) {
        return ResponseEntity.ok(
            paymentService.getPayments(filter, pageable, SecurityUtils.getCurrentUserId())
        );
    }

    @GetMapping("/advanced")
    public ResponseEntity<Page<PaymentResponse>> getAdvancedPayments(
        @Valid @ModelAttribute AdvancedPaymentSearchFilter filter,
        Pageable pageable) {
        return ResponseEntity.ok(
            paymentService.getAdvancedPayments(filter, pageable, SecurityUtils.getCurrentUserId())
        );
    }

    @GetMapping("/summary/me")
    public ResponseEntity<BigDecimal> getTotalPaymentAmountByUser(
        @RequestParam Instant from,
        @RequestParam Instant to) {
        return ResponseEntity.ok(paymentService.getTotalPaymentAmountByUserIdAndDateRange(SecurityUtils.getCurrentUserId(), from, to));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BigDecimal> getTotalPaymentAmount(
        @RequestParam Instant from,
        @RequestParam Instant to) {
        return ResponseEntity.ok(paymentService.getTotalPaymentAmountByDateRange(from, to));
    }
}
