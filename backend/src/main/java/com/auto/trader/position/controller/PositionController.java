package com.auto.trader.position.controller;

import com.auto.trader.position.dto.PositionDto;
import com.auto.trader.position.service.PositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
public class PositionController {
    private final PositionService positionService;

    @GetMapping
    public List<PositionDto> getAll() {
        return positionService.findAll();
    }

    @PostMapping
    public void saveAll(@RequestBody List<PositionDto> positions) {
        positionService.saveAll(positions);
    }
}
