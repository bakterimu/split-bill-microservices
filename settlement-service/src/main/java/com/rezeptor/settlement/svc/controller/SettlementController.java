package com.rezeptor.settlement.svc.controller;

import com.rezeptor.settlement.svc.dto.request.SettlementCreateReq;
import com.rezeptor.settlement.svc.dto.response.Response;
import com.rezeptor.settlement.svc.dto.response.SettlementDto;
import com.rezeptor.settlement.svc.dto.response.SettlementHistoryDto;
import com.rezeptor.settlement.svc.dto.response.SettlementSuggestionDto;
import com.rezeptor.settlement.svc.service.SettlementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/settlement")
@RequiredArgsConstructor
public class SettlementController {
    private final SettlementService settlementService;

    @GetMapping("/suggestion")
    public ResponseEntity<Response<SettlementSuggestionDto>> getSuggestion(@RequestParam @Positive Long groupId) {
        return Response.ok(settlementService.getSuggestion(groupId)).toEntity();
    }

    @PostMapping
    public ResponseEntity<Response<SettlementDto>> createSettlement(@Valid @RequestBody SettlementCreateReq req) {
        return Response.created(settlementService.createSettlement(req)).toEntity();
    }

    @GetMapping
    public ResponseEntity<Response<List<SettlementHistoryDto>>> getHistory(@RequestParam @Positive Long groupId) {
        return Response.ok(settlementService.getHistory(groupId)).toEntity();
    }
}
