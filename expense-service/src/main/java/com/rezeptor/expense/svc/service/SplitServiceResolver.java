package com.rezeptor.expense.svc.service;

import com.rezeptor.expense.svc.constant.SplitType;
import com.rezeptor.expense.svc.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SplitServiceResolver {
    private final Map<SplitType, SplitService> splitServices;

    public SplitServiceResolver(List<SplitService> splitServiceList) {
        this.splitServices = splitServiceList.stream()
                .collect(Collectors.toMap(
                        SplitService::type,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException("Duplicate split service for " + a.type());
                        },
                        () -> new EnumMap<>(SplitType.class)));
    }

    public SplitService resolve(SplitType type) {
        SplitService splitService = splitServices.get(type);
        if (splitService == null) {
            throw new BusinessException("Unsupported split type: " + type);
        }
        return splitService;
    }
}
