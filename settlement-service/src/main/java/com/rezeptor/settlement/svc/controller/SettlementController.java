package com.rezeptor.settlement.svc.controller;

import com.rezeptor.settlement.svc.dto.response.Response;
import com.rezeptor.settlement.svc.dto.response.SettlementSuggestionDto;
import com.rezeptor.settlement.svc.service.SettlementService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settlement")
@RequiredArgsConstructor
public class SettlementController {
    private final SettlementService settlementService;

    @GetMapping("/suggestion")
    public ResponseEntity<Response<SettlementSuggestionDto>> getSuggestion(@RequestParam @Positive Long groupId) {
        return Response.ok(settlementService.getSuggestion(groupId)).toEntity();
    }
}
