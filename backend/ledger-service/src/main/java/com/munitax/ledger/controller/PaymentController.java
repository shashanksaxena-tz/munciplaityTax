package com.munitax.ledger.controller;

import com.munitax.ledger.dto.PaymentReceipt;
import com.munitax.ledger.dto.PaymentRequest;
import com.munitax.ledger.dto.PaymentResponse;
import com.munitax.ledger.model.PaymentTransaction;
import com.munitax.ledger.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        log.info("Processing payment request for filer {}", request.getFilerId());
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/filer/{filerId}")
    public ResponseEntity<List<PaymentTransaction>> getFilerPayments(@PathVariable UUID filerId) {
        List<PaymentTransaction> payments = paymentService.getFilerPayments(filerId);
        return ResponseEntity.ok(payments);
    }
    
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentTransaction> getPayment(@PathVariable UUID paymentId) {
        PaymentTransaction payment = paymentService.getPaymentByPaymentId(paymentId);
        return ResponseEntity.ok(payment);
    }
    
    @GetMapping("/{paymentId}/receipt")
    public ResponseEntity<PaymentReceipt> getPaymentReceipt(@PathVariable UUID paymentId) {
        log.info("Generating receipt for payment {}", paymentId);
        PaymentReceipt receipt = paymentService.generatePaymentReceipt(paymentId);
        return ResponseEntity.ok(receipt);
    }
    
    @GetMapping("/test-mode-indicator")
    public ResponseEntity<String> getTestModeIndicator() {
        return ResponseEntity.ok("TEST MODE: No real charges will be processed");
    }
}
