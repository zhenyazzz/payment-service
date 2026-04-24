package com.innowise.paymentservice.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import com.innowise.paymentservice.client.AcquiringResult;
import com.innowise.paymentservice.client.PaymentAcquiringClient;
import com.innowise.paymentservice.dto.request.AdvancedPaymentSearchFilter;
import com.innowise.paymentservice.dto.request.CreatePaymentRequest;
import com.innowise.paymentservice.dto.request.PaymentSearchFilter;
import com.innowise.paymentservice.dto.response.PaymentResponse;
import com.innowise.paymentservice.exception.notfound.PaymentNotFoundException;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.model.enums.PaymentStatus;
import com.innowise.paymentservice.persistence.PaymentPersistenceService;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.repository.criteria.AdvancedPaymentSearchCriteria;
import com.innowise.paymentservice.repository.criteria.PaymentSearchCriteria;
import com.innowise.paymentservice.security.SecurityUtils;
import com.innowise.paymentservice.service.impl.PaymentServiceImpl;
import com.innowise.paymentservice.utils.PaymentTestDataFactory;

import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl (unit tests)")
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PaymentAcquiringClient paymentAcquiringClient;

    @Mock
    private PaymentPersistenceService paymentPersistenceService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Nested
    @DisplayName("createPayment")
    class CreatePayment {

        @Test
        @DisplayName("creates payment and returns mapped response when acquiring succeeds")
        void whenAcquiringSucceeds_createsSuccessfulPayment() {
            CreatePaymentRequest request = PaymentTestDataFactory.buildCreatePaymentRequest();
            Payment mappedPayment = PaymentTestDataFactory.buildPayment();
            Payment savedPayment = PaymentTestDataFactory.buildPayment(PaymentStatus.PENDING);
            Payment finalizedPayment = PaymentTestDataFactory.buildPayment(PaymentStatus.SUCCESS);
            PaymentResponse response = PaymentTestDataFactory.buildPaymentResponse(finalizedPayment);

            when(paymentMapper.toEntity(request, PaymentTestDataFactory.USER_ID)).thenReturn(mappedPayment);
            when(paymentPersistenceService.savePendingPayment(mappedPayment)).thenReturn(savedPayment);
            when(paymentAcquiringClient.getAcquiringResult()).thenReturn(new AcquiringResult(2));
            when(paymentPersistenceService.finalizePaymentAndCreateOutboxEvent(savedPayment, PaymentStatus.SUCCESS))
                .thenReturn(finalizedPayment);
            when(paymentMapper.toResponse(finalizedPayment)).thenReturn(response);

            PaymentResponse result = paymentService.createPayment(PaymentTestDataFactory.USER_ID, request);

            assertThat(result).isEqualTo(response);
            verify(paymentMapper).toEntity(request, PaymentTestDataFactory.USER_ID);
            verify(paymentPersistenceService).savePendingPayment(mappedPayment);
            verify(paymentAcquiringClient).getAcquiringResult();
            verify(paymentPersistenceService)
                .finalizePaymentAndCreateOutboxEvent(savedPayment, PaymentStatus.SUCCESS);
            verify(paymentMapper).toResponse(finalizedPayment);
        }

        @Test
        @DisplayName("creates failed payment when acquiring returns odd response code")
        void whenAcquiringReturnsOddCode_createsFailedPayment() {
            CreatePaymentRequest request = PaymentTestDataFactory.buildCreatePaymentRequest();
            Payment mappedPayment = PaymentTestDataFactory.buildPayment();
            Payment savedPayment = PaymentTestDataFactory.buildPayment(PaymentStatus.PENDING);
            Payment finalizedPayment = PaymentTestDataFactory.buildPayment(PaymentStatus.FAILED);
            PaymentResponse response = PaymentTestDataFactory.buildPaymentResponse(finalizedPayment);

            when(paymentMapper.toEntity(request, PaymentTestDataFactory.USER_ID)).thenReturn(mappedPayment);
            when(paymentPersistenceService.savePendingPayment(mappedPayment)).thenReturn(savedPayment);
            when(paymentAcquiringClient.getAcquiringResult()).thenReturn(new AcquiringResult(3));
            when(paymentPersistenceService.finalizePaymentAndCreateOutboxEvent(savedPayment, PaymentStatus.FAILED))
                .thenReturn(finalizedPayment);
            when(paymentMapper.toResponse(finalizedPayment)).thenReturn(response);

            PaymentResponse result = paymentService.createPayment(PaymentTestDataFactory.USER_ID, request);

            assertThat(result).isEqualTo(response);
            verify(paymentPersistenceService).finalizePaymentAndCreateOutboxEvent(savedPayment, PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("creates failed payment when acquiring client throws")
        void whenAcquiringThrows_createsFailedPayment() {
            CreatePaymentRequest request = PaymentTestDataFactory.buildCreatePaymentRequest();
            Payment mappedPayment = PaymentTestDataFactory.buildPayment();
            Payment savedPayment = PaymentTestDataFactory.buildPayment(PaymentStatus.PENDING);
            Payment finalizedPayment = PaymentTestDataFactory.buildPayment(PaymentStatus.FAILED);
            PaymentResponse response = PaymentTestDataFactory.buildPaymentResponse(finalizedPayment);

            when(paymentMapper.toEntity(request, PaymentTestDataFactory.USER_ID)).thenReturn(mappedPayment);
            when(paymentPersistenceService.savePendingPayment(mappedPayment)).thenReturn(savedPayment);
            when(paymentAcquiringClient.getAcquiringResult()).thenThrow(new RuntimeException("gateway down"));
            when(paymentPersistenceService.finalizePaymentAndCreateOutboxEvent(savedPayment, PaymentStatus.FAILED))
                .thenReturn(finalizedPayment);
            when(paymentMapper.toResponse(finalizedPayment)).thenReturn(response);

            PaymentResponse result = paymentService.createPayment(PaymentTestDataFactory.USER_ID, request);

            assertThat(result).isEqualTo(response);
            verify(paymentPersistenceService).finalizePaymentAndCreateOutboxEvent(savedPayment, PaymentStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("getPaymentById")
    class GetPaymentById {

        @Test
        @DisplayName("returns payment response when owner requests payment")
        void whenOwnerRequestsPayment_returnsResponse() {
            Payment payment = PaymentTestDataFactory.buildPayment();
            PaymentResponse response = PaymentTestDataFactory.buildPaymentResponse(payment);

            when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
            when(paymentMapper.toResponse(payment)).thenReturn(response);

            PaymentResponse result = paymentService.getPaymentById(payment.getId(), payment.getUserId());

            assertThat(result).isEqualTo(response);
            verify(paymentMapper).toResponse(payment);
        }

        @Test
        @DisplayName("throws not found when payment does not exist")
        void whenPaymentMissing_throwsNotFound() {
            when(paymentRepository.findById(PaymentTestDataFactory.PAYMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPaymentById(
                PaymentTestDataFactory.PAYMENT_ID,
                PaymentTestDataFactory.USER_ID
            ))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessage("Payment not found");
        }

        @Test
        @DisplayName("throws access denied when user is not owner and not admin")
        void whenUserIsNotOwnerAndNotAdmin_throwsAccessDenied() {
            Payment payment = PaymentTestDataFactory.buildPayment();

            when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

            try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
                securityUtils.when(SecurityUtils::isAdmin).thenReturn(false);

                String paymentId = payment.getId();
                assertThatThrownBy(() -> paymentService.getPaymentById(paymentId, PaymentTestDataFactory.OTHER_USER_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("You are not allowed to access this payment");
            }
        }

        @Test
        @DisplayName("returns payment response when admin requests payment")
        void whenAdminRequestsPayment_returnsResponse() {
            Payment payment = PaymentTestDataFactory.buildPayment();
            PaymentResponse response = PaymentTestDataFactory.buildPaymentResponse(payment);

            when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
            when(paymentMapper.toResponse(payment)).thenReturn(response);

            try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
                securityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

                PaymentResponse result = paymentService.getPaymentById(
                    payment.getId(),
                    PaymentTestDataFactory.OTHER_USER_ID
                );

                assertThat(result).isEqualTo(response);
            }
        }
    }

    @Nested
    @DisplayName("getPayments")
    class GetPayments {

        @Test
        @DisplayName("uses current user for non-admin search")
        void whenUserIsNotAdmin_usesCurrentUserId() {
            Payment payment = PaymentTestDataFactory.buildPayment();
            PaymentResponse response = PaymentTestDataFactory.buildPaymentResponse(payment);
            PaymentSearchFilter filter = PaymentTestDataFactory.buildPaymentSearchFilter();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Payment> page = new PageImpl<>(List.of(payment), pageable, 1);
            PaymentSearchCriteria expectedCriteria = new PaymentSearchCriteria(
                PaymentTestDataFactory.OTHER_USER_ID,
                filter.orderId(),
                filter.status()
            );

            when(paymentRepository.findPaymentsByCriteria(expectedCriteria, pageable)).thenReturn(page);
            when(paymentMapper.toResponse(payment)).thenReturn(response);

            try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
                securityUtils.when(SecurityUtils::isAdmin).thenReturn(false);

                Page<PaymentResponse> result = paymentService.getPayments(
                    filter,
                    pageable,
                    PaymentTestDataFactory.OTHER_USER_ID
                );

                assertThat(result.getContent()).containsExactly(response);
            }

            verify(paymentRepository).findPaymentsByCriteria(expectedCriteria, pageable);
            verify(paymentMapper).toResponse(payment);
        }

        @Test
        @DisplayName("uses filter user for admin search")
        void whenUserIsAdmin_usesFilterUserId() {
            Payment payment = PaymentTestDataFactory.buildPayment();
            PaymentResponse response = PaymentTestDataFactory.buildPaymentResponse(payment);
            PaymentSearchFilter filter = PaymentTestDataFactory.buildPaymentSearchFilter();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Payment> page = new PageImpl<>(List.of(payment), pageable, 1);
            PaymentSearchCriteria expectedCriteria = new PaymentSearchCriteria(
                filter.userId(),
                filter.orderId(),
                filter.status()
            );

            when(paymentRepository.findPaymentsByCriteria(expectedCriteria, pageable)).thenReturn(page);
            when(paymentMapper.toResponse(payment)).thenReturn(response);

            try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
                securityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

                Page<PaymentResponse> result = paymentService.getPayments(
                    filter,
                    pageable,
                    PaymentTestDataFactory.OTHER_USER_ID
                );

                assertThat(result.getContent()).containsExactly(response);
            }

            verify(paymentRepository).findPaymentsByCriteria(expectedCriteria, pageable);
        }
    }

    @Nested
    @DisplayName("getAdvancedPayments")
    class GetAdvancedPayments {

        @Test
        @DisplayName("uses current user for non-admin advanced search")
        void whenUserIsNotAdmin_usesCurrentUserId() {
            Payment payment = PaymentTestDataFactory.buildPayment();
            PaymentResponse response = PaymentTestDataFactory.buildPaymentResponse(payment);
            AdvancedPaymentSearchFilter filter = PaymentTestDataFactory.buildAdvancedPaymentSearchFilter();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Payment> page = new PageImpl<>(List.of(payment), pageable, 1);
            AdvancedPaymentSearchCriteria expectedCriteria = new AdvancedPaymentSearchCriteria(
                PaymentTestDataFactory.OTHER_USER_ID,
                filter.orderId(),
                filter.statuses(),
                filter.createdFrom(),
                filter.createdTo()
            );

            when(paymentRepository.searchPaymentsByCriteria(expectedCriteria, pageable)).thenReturn(page);
            when(paymentMapper.toResponse(payment)).thenReturn(response);

            try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
                securityUtils.when(SecurityUtils::isAdmin).thenReturn(false);

                Page<PaymentResponse> result = paymentService.getAdvancedPayments(
                    filter,
                    pageable,
                    PaymentTestDataFactory.OTHER_USER_ID
                );

                assertThat(result.getContent()).containsExactly(response);
            }

            verify(paymentRepository).searchPaymentsByCriteria(expectedCriteria, pageable);
            verify(paymentMapper).toResponse(payment);
        }

        @Test
        @DisplayName("uses filter user for admin advanced search")
        void whenUserIsAdmin_usesFilterUserId() {
            Payment payment = PaymentTestDataFactory.buildPayment();
            PaymentResponse response = PaymentTestDataFactory.buildPaymentResponse(payment);
            AdvancedPaymentSearchFilter filter = PaymentTestDataFactory.buildAdvancedPaymentSearchFilter();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Payment> page = new PageImpl<>(List.of(payment), pageable, 1);
            AdvancedPaymentSearchCriteria expectedCriteria = new AdvancedPaymentSearchCriteria(
                filter.userId(),
                filter.orderId(),
                filter.statuses(),
                filter.createdFrom(),
                filter.createdTo()
            );

            when(paymentRepository.searchPaymentsByCriteria(expectedCriteria, pageable)).thenReturn(page);
            when(paymentMapper.toResponse(payment)).thenReturn(response);

            try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
                securityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

                Page<PaymentResponse> result = paymentService.getAdvancedPayments(
                    filter,
                    pageable,
                    PaymentTestDataFactory.OTHER_USER_ID
                );

                assertThat(result.getContent()).containsExactly(response);
            }

            verify(paymentRepository).searchPaymentsByCriteria(expectedCriteria, pageable);
        }
    }

    @Nested
    @DisplayName("getTotalPaymentAmountByUserIdAndDateRange")
    class GetTotalPaymentAmountByUserIdAndDateRange {

        @Test
        @DisplayName("returns total amount for user in date range")
        void whenCalled_returnsUserTotal() {
            Instant from = Instant.parse("2025-06-01T00:00:00Z");
            Instant to = Instant.parse("2025-06-02T00:00:00Z");
            BigDecimal expected = new BigDecimal("123.45");

            when(paymentRepository.sumByUserIdDateRange(PaymentTestDataFactory.USER_ID, from, to)).thenReturn(expected);

            BigDecimal result = paymentService.getTotalPaymentAmountByUserIdAndDateRange(
                PaymentTestDataFactory.USER_ID,
                from,
                to
            );

            assertThat(result).isEqualByComparingTo(expected);
            verify(paymentRepository).sumByUserIdDateRange(PaymentTestDataFactory.USER_ID, from, to);
        }
    }

    @Nested
    @DisplayName("getTotalPaymentAmountByDateRange")
    class GetTotalPaymentAmountByDateRange {

        @Test
        @DisplayName("returns total amount across all users in date range")
        void whenCalled_returnsTotal() {
            Instant from = Instant.parse("2025-06-01T00:00:00Z");
            Instant to = Instant.parse("2025-06-02T00:00:00Z");
            BigDecimal expected = new BigDecimal("999.99");

            when(paymentRepository.sumAllPaymentsDateRange(from, to)).thenReturn(expected);

            BigDecimal result = paymentService.getTotalPaymentAmountByDateRange(from, to);

            assertThat(result).isEqualByComparingTo(expected);
            verify(paymentRepository).sumAllPaymentsDateRange(from, to);
        }
    }
}
