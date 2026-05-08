package com.innowise.paymentservice.service.impl;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.innowise.paymentservice.client.AcquiringResult;
import com.innowise.paymentservice.client.OrderClient;
import com.innowise.paymentservice.client.PaymentAcquiringClient;
import com.innowise.paymentservice.dto.request.AdvancedPaymentSearchFilter;
import com.innowise.paymentservice.dto.request.CreatePaymentRequest;
import com.innowise.paymentservice.dto.request.PaymentSearchFilter;
import com.innowise.paymentservice.dto.response.PaymentResponse;
import com.innowise.paymentservice.exception.conflict.PaymentAlreadyProcessedException;
import com.innowise.paymentservice.exception.notfound.PaymentNotFoundException;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.persistence.PaymentPersistenceService;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.repository.criteria.AdvancedPaymentSearchCriteria;
import com.innowise.paymentservice.repository.criteria.PaymentSearchCriteria;
import com.innowise.paymentservice.security.SecurityUtils;
import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.model.enums.PaymentStatus;
import com.innowise.paymentservice.service.PaymentService;

import lombok.RequiredArgsConstructor;

/**
 * Default implementation of payment business operations.
 *
 * <p>Handles payment creation, access checks, filtered search, and summary calculations.
 * Coordinates persistence and acquiring calls while preserving idempotent behavior for
 * already paid orders.</p>
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OrderClient orderClient;
    private final PaymentAcquiringClient paymentAcquiringClient;
    private final PaymentPersistenceService paymentPersistenceService;

    /**
     * Creates a payment for a user and finalizes it based on acquiring response.
     *
     * @param currentUserId current authenticated user identifier
     * @param createPaymentRequest incoming payment creation request
     * @return finalized payment response
     */
    @Override
    public PaymentResponse createPayment(String currentUserId, CreatePaymentRequest createPaymentRequest) {
        Payment payment = paymentMapper.toEntity(createPaymentRequest, currentUserId);
        payment.setPaymentAmount(
            orderClient.getOrderTotalPrice(createPaymentRequest.orderId(), currentUserId)
        );

        if (paymentRepository.existsByOrderIdAndStatus(payment.getOrderId(), PaymentStatus.SUCCESS)) {
            throw new PaymentAlreadyProcessedException("Order already paid: " + payment.getOrderId());
        }

        payment = paymentPersistenceService.savePendingPayment(payment);

        AcquiringResult acquiringResult = paymentAcquiringClient.getAcquiringResult();

        PaymentStatus finalStatus = (acquiringResult.responseCode() % 2 == 0) 
            ? PaymentStatus.SUCCESS
            : PaymentStatus.FAILED;

        try {
            Payment finalizedPayment = paymentPersistenceService.finalizePaymentAndCreateOutboxEvent(
                payment, finalStatus
            );
            return paymentMapper.toResponse(finalizedPayment);
        } catch (DuplicateKeyException e) {
            throw new PaymentAlreadyProcessedException("Order already paid: " + payment.getOrderId());
        }
    }

    /**
     * Returns payment by identifier with ownership/admin access validation.
     *
     * @param paymentId payment identifier
     * @param currentUserId current authenticated user identifier
     * @return payment response
     */
    @Override
    public PaymentResponse getPaymentById(String paymentId, String currentUserId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if (!isAllowedToAccessPayment(payment, currentUserId) ) {
            throw new AccessDeniedException("You are not allowed to access this payment");
        }

        return paymentMapper.toResponse(payment);
    }

    /**
     * Returns paged payment list using basic filter criteria.
     *
     * @param paymentSearchFilter basic search filter
     * @param pageable pagination configuration
     * @param currentUserId current authenticated user identifier
     * @return page of payments mapped to response DTO
     */
    @Override
    public Page<PaymentResponse> getPayments(
        PaymentSearchFilter paymentSearchFilter,
        Pageable pageable,
        String currentUserId
    ) {
        String userId = SecurityUtils.isAdmin() ? paymentSearchFilter.userId() : currentUserId;

        PaymentSearchCriteria criteria = new PaymentSearchCriteria(
            userId, 
            paymentSearchFilter.orderId(), 
            paymentSearchFilter.status()
        );

        return paymentRepository.findPaymentsByCriteria(criteria, pageable).map(paymentMapper::toResponse);
    }

    /**
     * Returns paged payment list using advanced filter criteria.
     *
     * @param advancedPaymentSearchFilter advanced search filter
     * @param pageable pagination configuration
     * @param currentUserId current authenticated user identifier
     * @return page of payments mapped to response DTO
     */
    @Override
    public Page<PaymentResponse> getAdvancedPayments(
        AdvancedPaymentSearchFilter advancedPaymentSearchFilter,
        Pageable pageable,
        String currentUserId
    ) {
        String userId = SecurityUtils.isAdmin() ? advancedPaymentSearchFilter.userId() : currentUserId;

        AdvancedPaymentSearchCriteria criteria = new AdvancedPaymentSearchCriteria(
            userId, 
            advancedPaymentSearchFilter.orderId(), 
            advancedPaymentSearchFilter.statuses(), 
            advancedPaymentSearchFilter.createdFrom(), 
            advancedPaymentSearchFilter.createdTo()
        );

        return paymentRepository.searchPaymentsByCriteria(criteria, pageable).map(paymentMapper::toResponse);
    }

    /**
     * Calculates total successful payment amount for one user in the provided interval.
     *
     * @param currentUserId current authenticated user identifier
     * @param from interval start (inclusive)
     * @param to interval end (inclusive)
     * @return aggregated amount
     */
    @Override
    public BigDecimal getTotalPaymentAmountByUserIdAndDateRange(String currentUserId, Instant from, Instant to) {
        return paymentRepository.sumByUserIdDateRange(currentUserId, from, to);
    }

    /**
     * Calculates total successful payment amount for all users in the provided interval.
     *
     * @param from interval start (inclusive)
     * @param to interval end (inclusive)
     * @return aggregated amount
     */
    @Override
    public BigDecimal getTotalPaymentAmountByDateRange(Instant from, Instant to) {
        return paymentRepository.sumAllPaymentsDateRange(from, to);
    }

    private boolean isAllowedToAccessPayment(Payment payment, String currentUserId) {
        return payment.getUserId().equals(currentUserId) || SecurityUtils.isAdmin();
    }
}
