package com.rezeptor.settlement.svc.service;

import com.rezeptor.settlement.svc.data.BalanceData;
import com.rezeptor.settlement.svc.data.TransferData;
import com.rezeptor.settlement.svc.service.impl.DebtSimplifierServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class DebtSimplifierServiceTest {

    private final DebtSimplifierService debtSimplifierService = new DebtSimplifierServiceImpl();

    @Test
    void simplifiesScenarioBeforeAnySettlement() {
        List<TransferData> result = debtSimplifierService.simplify(List.of(
                balance(1L, "1100000.00"),
                balance(2L, "-750000.00"),
                balance(3L, "500000.00"),
                balance(4L, "-850000.00")));

        assertThat(result)
                .extracting(TransferData::getFromUserId, TransferData::getToUserId, TransferData::getAmount)
                .containsExactly(
                        tuple(4L, 1L, new BigDecimal("850000.00")),
                        tuple(2L, 1L, new BigDecimal("250000.00")),
                        tuple(2L, 3L, new BigDecimal("500000.00")));
    }

    @Test
    void simplifiesScenarioAfterDodiPaysBudi() {
        List<TransferData> result = debtSimplifierService.simplify(List.of(
                balance(1L, "250000.00"),
                balance(2L, "-750000.00"),
                balance(3L, "500000.00"),
                balance(4L, "0.00")));

        assertThat(result)
                .extracting(TransferData::getFromUserId, TransferData::getToUserId, TransferData::getAmount)
                .containsExactly(
                        tuple(2L, 3L, new BigDecimal("500000.00")),
                        tuple(2L, 1L, new BigDecimal("250000.00")));
    }

    @Test
    void oneCreditorReceivesFromEveryDebtor() {
        List<TransferData> result = debtSimplifierService.simplify(List.of(
                balance(1L, "300000.00"),
                balance(2L, "-100000.00"),
                balance(3L, "-100000.00"),
                balance(4L, "-100000.00")));

        assertThat(result)
                .extracting(TransferData::getFromUserId, TransferData::getToUserId)
                .containsExactly(tuple(2L, 1L), tuple(3L, 1L), tuple(4L, 1L));
    }

    @Test
    void returnsNoTransferWhenEveryoneIsSettled() {
        assertThat(debtSimplifierService.simplify(List.of())).isEmpty();
        assertThat(debtSimplifierService.simplify(List.of(balance(1L, "0"), balance(2L, "0")))).isEmpty();
    }

    @Test
    void rejectsBalancesThatDoNotSumToZero() {
        assertThatThrownBy(() -> debtSimplifierService.simplify(List.of(
                balance(1L, "100000.00"),
                balance(2L, "-50000.00"))))
                .isInstanceOf(IllegalStateException.class);
    }

    private static BalanceData balance(Long userId, String amount) {
        return new BalanceData(userId, new BigDecimal(amount));
    }
}
