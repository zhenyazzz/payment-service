package com.innowise.paymentservice.service;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.innowise.paymentservice.dto.request.AdvancedPaymentSearchFilter;
import com.innowise.paymentservice.dto.request.CreatePaymentRequest;
import com.innowise.paymentservice.dto.request.PaymentSearchFilter;
import com.innowise.paymentservice.dto.response.PaymentResponse;

/**
 * Business contract for payment lifecycle operations and payment analytics.
 */
public interface PaymentService {
    /**
     * Creates a payment for current authenticated user.
     *
     * @param currentUserId current user identifier from security context
     * @param createPaymentRequest payment creation payload
     * @return created payment DTO
     */
    PaymentResponse createPayment(
        String currentUserId,
        CreatePaymentRequest createPaymentRequest
    );
    
    /**
     * Returns payment by id with access checks for current user.
     *
     * @param paymentId payment identifier
     * @param currentUserId current user identifier from security context
     * @return payment DTO
     */
    PaymentResponse getPaymentById(
        String paymentId, 
        String currentUserId
    );

    /**
     * Returns paged payments by basic filter for current user scope.
     *
     * @param paymentSearchFilter basic filter criteria
     * @param pageable pagination and sorting settings
     * @param currentUserId current user identifier from security context
     * @return page of payment DTOs
     */
    Page<PaymentResponse> getPayments(
        PaymentSearchFilter paymentSearchFilter,
        Pageable pageable,
        String currentUserId
    );

    /**
     * Returns paged payments by advanced filter for current user scope.
     *
     * @param advancedPaymentSearchFilter advanced filter criteria
     * @param pageable pagination and sorting settings
     * @param currentUserId current user identifier from security context
     * @return page of payment DTOs
     */
    Page<PaymentResponse> getAdvancedPayments(
        AdvancedPaymentSearchFilter advancedPaymentSearchFilter,
        Pageable pageable,
        String currentUserId
    );

    /**
     * Calculates total payment amount for given user in time range.
     *
     * @param currentUserId user identifier
     * @param from range start (inclusive)
     * @param to range end (inclusive)
     * @return summed amount
     */
    BigDecimal getTotalPaymentAmountByUserIdAndDateRange(
        String currentUserId,
        Instant from,
        Instant to
    );

    /**
     * Calculates total payment amount across all users in time range.
     *
     * @param from range start (inclusive)
     * @param to range end (inclusive)
     * @return summed amount across all users
     */
    BigDecimal getTotalPaymentAmountByDateRange(
        Instant from,
        Instant to
    );

}
