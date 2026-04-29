package com.innowise.paymentservice.service.impl;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.innowise.paymentservice.client.AcquiringResult;
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

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentAcquiringClient paymentAcquiringClient;
    private final PaymentPersistenceService paymentPersistenceService;

    @Override
    public PaymentResponse createPayment(String currentUserId, CreatePaymentRequest createPaymentRequest) {
        Payment payment = paymentMapper.toEntity(createPaymentRequest, currentUserId);

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

    @Override
    public PaymentResponse getPaymentById(String paymentId, String currentUserId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if (!isAllowedToAccessPayment(payment, currentUserId) ) {
            throw new AccessDeniedException("You are not allowed to access this payment");
        }

        return paymentMapper.toResponse(payment);
    }

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

    @Override
    public BigDecimal getTotalPaymentAmountByUserIdAndDateRange(String currentUserId, Instant from, Instant to) {
        return paymentRepository.sumByUserIdDateRange(currentUserId, from, to);
    }

    @Override
    public BigDecimal getTotalPaymentAmountByDateRange(Instant from, Instant to) {
        return paymentRepository.sumAllPaymentsDateRange(from, to);
    }

    private boolean isAllowedToAccessPayment(Payment payment, String currentUserId) {
        return payment.getUserId().equals(currentUserId) || SecurityUtils.isAdmin();
    }
}
