package com.processeng.distill.api;

import com.processeng.distill.api.dto.DistillationRequest;
import com.processeng.distill.api.dto.DistillationResponse;
import com.processeng.distill.service.DistillationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 精馏简捷核算 HTTP 接口。纯 JSON API，不提供任何页面/图形界面。
 */
@RestController
@RequestMapping("/api/v1/distillation")
public class DistillationController {

    private final DistillationService service;

    public DistillationController(DistillationService service) {
        this.service = service;
    }

    @PostMapping("/shortcut")
    @ResponseStatus(HttpStatus.OK)
    public DistillationResponse shortcut(@Valid @RequestBody DistillationRequest request) {
        return service.calculate(request);
    }
}
