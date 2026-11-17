package com.rezeptor.expense.svc.service;

import com.rezeptor.expense.svc.constant.SplitType;
import com.rezeptor.expense.svc.data.ShareResultData;
import com.rezeptor.expense.svc.dto.request.ParticipantReq;
import com.rezeptor.expense.svc.exception.BusinessException;
import com.rezeptor.expense.svc.service.impl.EqualSplitServiceImpl;
import com.rezeptor.expense.svc.service.impl.ExactSplitServiceImpl;
import com.rezeptor.expense.svc.service.impl.PercentageSplitServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class SplitServiceTest {

    private final EqualSplitServiceImpl equal = new EqualSplitServiceImpl();
    private final PercentageSplitServiceImpl percentage = new PercentageSplitServiceImpl();
    private final ExactSplitServiceImpl exact = new ExactSplitServiceImpl();

    @Test
    void equalSplitDividesEvenly() {
        List<ShareResultData> result = equal.split(amount("3000000"), participants(1L, 2L, 3L, 4L));

        assertThat(result).extracting(ShareResultData::userId, ShareResultData::shareAmount).containsExactly(
                tuple(1L, amount("750000.00")),
                tuple(2L, amount("750000.00")),
                tuple(3L, amount("750000.00")),
                tuple(4L, amount("750000.00")));
    }

    @Test
    void equalSplitGivesRemainingCentToLowestUserId() {
        List<ShareResultData> result = equal.split(amount("100000"), participants(3L, 1L, 2L));

        assertThat(result).extracting(ShareResultData::userId, ShareResultData::shareAmount).containsExactly(
                tuple(1L, amount("33333.34")),
                tuple(2L, amount("33333.33")),
                tuple(3L, amount("33333.33")));
        assertThat(total(result)).isEqualByComparingTo("100000");
    }

    @Test
    void percentageSplitFollowsPercentages() {
        List<ShareResultData> result = percentage.split(amount("2000000"), List.of(
                participant(1L, "40"), participant(2L, "20"), participant(3L, "20"), participant(4L, "20")));

        assertThat(result).extracting(ShareResultData::userId, ShareResultData::shareAmount).containsExactly(
                tuple(1L, amount("800000.00")),
                tuple(2L, amount("400000.00")),
                tuple(3L, amount("400000.00")),
                tuple(4L, amount("400000.00")));
    }

    @Test
    void percentageSplitGivesRoundingDifferenceToLargestPercentage() {
        List<ShareResultData> result = percentage.split(amount("10"), List.of(
                participant(1L, "33.33"), participant(2L, "33.33"), participant(3L, "33.34")));

        assertThat(result).extracting(ShareResultData::userId, ShareResultData::shareAmount).containsExactly(
                tuple(1L, amount("3.33")),
                tuple(2L, amount("3.33")),
                tuple(3L, amount("3.34")));
        assertThat(total(result)).isEqualByComparingTo("10");
    }

    @Test
    void percentageSplitRejectsTotalOtherThanHundred() {
        assertThatThrownBy(() -> percentage.split(amount("100000"), List.of(
                participant(1L, "50"), participant(2L, "30"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("100");
    }

    @Test
    void exactSplitUsesGivenAmounts() {
        List<ShareResultData> result = exact.split(amount("450000"), List.of(
                participant(1L, "150000"), participant(3L, "150000"), participant(4L, "150000")));

        assertThat(result).extracting(ShareResultData::userId, ShareResultData::shareAmount).containsExactly(
                tuple(1L, amount("150000.00")),
                tuple(3L, amount("150000.00")),
                tuple(4L, amount("150000.00")));
    }

    @Test
    void exactSplitRejectsTotalDifferentFromAmount() {
        assertThatThrownBy(() -> exact.split(amount("450000"), List.of(
                participant(1L, "200000"), participant(2L, "200000"))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void exactSplitRejectsMissingValue() {
        assertThatThrownBy(() -> exact.split(amount("100000"), participants(1L, 2L)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void resolverReturnsServiceForEverySplitType() {
        SplitServiceResolver resolver = new SplitServiceResolver(List.of(equal, percentage, exact));

        for (SplitType type : SplitType.values()) {
            assertThat(resolver.resolve(type).type()).isEqualTo(type);
        }
    }

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }

    private static ParticipantReq participant(Long userId, String value) {
        return new ParticipantReq(userId, new BigDecimal(value));
    }

    private static List<ParticipantReq> participants(Long... userIds) {
        return java.util.Arrays.stream(userIds).map(id -> new ParticipantReq(id, null)).toList();
    }

    private static BigDecimal total(List<ShareResultData> shares) {
        return shares.stream().map(ShareResultData::shareAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
