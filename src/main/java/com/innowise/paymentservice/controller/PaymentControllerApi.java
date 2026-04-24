package com.innowise.paymentservice.controller;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import com.innowise.paymentservice.aspect.Idempotent;
import com.innowise.paymentservice.dto.request.AdvancedPaymentSearchFilter;
import com.innowise.paymentservice.dto.request.CreatePaymentRequest;
import com.innowise.paymentservice.dto.request.PaymentSearchFilter;
import com.innowise.paymentservice.dto.response.PaymentResponse;

import jakarta.validation.Valid;

/**
 * REST contract for payment operations.
 */
@RequestMapping("/payments")
public interface PaymentControllerApi {

    /**
     * Creates a new payment.
     *
     * @param request payment creation payload
     * @return created payment data
     */
    @PostMapping
    @Idempotent
    ResponseEntity<PaymentResponse> createPayment(@RequestBody @Valid CreatePaymentRequest request);

    /**
     * Returns payment by identifier.
     *
     * @param id payment identifier
     * @return payment data
     */
    @GetMapping("/{id}")
    ResponseEntity<PaymentResponse> getPaymentById(@PathVariable String id);

    /**
     * Returns payments by basic filter with pagination.
     *
     * @param filter filtering criteria
     * @param pageable pagination and sorting settings
     * @return page of payments
     */
    @GetMapping
    ResponseEntity<Page<PaymentResponse>> getPayments(
            @Valid @ModelAttribute PaymentSearchFilter filter,
            Pageable pageable);

    /**
     * Returns payments by advanced filter with pagination.
     *
     * @param filter advanced filtering criteria
     * @param pageable pagination and sorting settings
     * @return page of payments
     */
    @GetMapping("/advanced")
    ResponseEntity<Page<PaymentResponse>> getAdvancedPayments(
            @Valid @ModelAttribute AdvancedPaymentSearchFilter filter,
            Pageable pageable);

    /**
     * Returns total payment amount for current authenticated user in time range.
     *
     * @param from range start (inclusive)
     * @param to range end (inclusive)
     * @return total amount for current user
     */
    @GetMapping("/summary/me")
    ResponseEntity<BigDecimal> getTotalPaymentAmountByUser(
            @RequestParam Instant from,
            @RequestParam Instant to);

    /**
     * Returns total payment amount across all users in time range.
     * Available only for administrators.
     *
     * @param from range start (inclusive)
     * @param to range end (inclusive)
     * @return total amount across all users
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<BigDecimal> getTotalPaymentAmount(
            @RequestParam Instant from,
            @RequestParam Instant to);
}
