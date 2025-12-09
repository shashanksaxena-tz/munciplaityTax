package com.munitax.ledger.controller;

import com.munitax.ledger.dto.PaymentReceipt;
import com.munitax.ledger.dto.PaymentRequest;
import com.munitax.ledger.dto.PaymentResponse;
import com.munitax.ledger.dto.TestPaymentMethodsResponse;
import com.munitax.ledger.model.PaymentTransaction;
import com.munitax.ledger.service.MockPaymentProviderService;
import com.munitax.ledger.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Payment processing with mock payment provider")
public class PaymentController {
    
    private final PaymentService paymentService;
    private final MockPaymentProviderService mockPaymentProviderService;
    
    @PostMapping({"/process", ""})
    @Operation(
        summary = "Process a payment",
        description = """
            Process a payment using the mock payment provider.
            Supports Credit Card, ACH, and Check payment methods.
            Creates double-entry journal entries on both filer and municipality books.
            
            **Test Cards:**
            - 4242-4242-4242-4242: Visa (always approved)
            - 4111-1111-1111-1111: Visa (always approved)
            - 4000-0000-0000-0002: Visa (always declined)
            - 5555-5555-5555-4444: Mastercard (approved)
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment processed successfully",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid payment request"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody @Parameter(description = "Payment request details") PaymentRequest request) {
        log.info("Processing payment request for filer {}", request.getFilerId());
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/filer/{filerId}")
    @Operation(
        summary = "Get all payments for a filer",
        description = "Retrieve all payment transactions for a specific filer"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of payments retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentTransaction.class))
        ),
        @ApiResponse(responseCode = "404", description = "Filer not found")
    })
    public ResponseEntity<List<PaymentTransaction>> getFilerPayments(
            @PathVariable @Parameter(description = "Filer UUID") UUID filerId) {
        List<PaymentTransaction> payments = paymentService.getFilerPayments(filerId);
        return ResponseEntity.ok(payments);
    }
    
    @GetMapping("/{paymentId}")
    @Operation(
        summary = "Get payment by ID",
        description = "Retrieve a specific payment transaction by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentTransaction.class))
        ),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<PaymentTransaction> getPayment(
            @PathVariable @Parameter(description = "Payment UUID") UUID paymentId) {
        PaymentTransaction payment = paymentService.getPaymentByPaymentId(paymentId);
        return ResponseEntity.ok(payment);
    }
    
    @GetMapping("/{paymentId}/receipt")
    @Operation(
        summary = "Generate payment receipt",
        description = "Generate a detailed receipt for a completed payment transaction"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Receipt generated successfully",
            content = @Content(schema = @Schema(implementation = PaymentReceipt.class))
        ),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<PaymentReceipt> getPaymentReceipt(
            @PathVariable @Parameter(description = "Payment UUID") UUID paymentId) {
        log.info("Generating receipt for payment {}", paymentId);
        PaymentReceipt receipt = paymentService.generatePaymentReceipt(paymentId);
        return ResponseEntity.ok(receipt);
    }
    
    @PostMapping("/{id}/confirm")
    @Operation(
        summary = "Confirm a payment",
        description = """
            Confirm a payment transaction. In the mock implementation, payments are 
            immediately processed, so this endpoint returns the current payment status.
            In a real payment gateway, this would confirm a pending authorization.
            Accepts either paymentId or transactionId.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment confirmation status retrieved",
            content = @Content(schema = @Schema(implementation = PaymentTransaction.class))
        ),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<PaymentTransaction> confirmPayment(
            @PathVariable @Parameter(description = "Payment or Transaction UUID") UUID id) {
        log.info("Confirming payment {}", id);
        // Try to find by paymentId first, then by transactionId
        PaymentTransaction payment;
        try {
            payment = paymentService.getPaymentByPaymentId(id);
        } catch (IllegalArgumentException e) {
            payment = paymentService.getPaymentByTransactionId(id);
        }
        return ResponseEntity.ok(payment);
    }
    
    @GetMapping("/test-mode-indicator")
    @Operation(
        summary = "Get test mode indicator",
        description = "Returns a message indicating the system is in test mode"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Test mode indicator message"
    )
    public ResponseEntity<String> getTestModeIndicator() {
        return ResponseEntity.ok("TEST MODE: No real charges will be processed");
    }
    
    /**
     * Returns available test payment methods for the mock payment gateway.
     * In TEST mode: Returns populated lists of test credit cards and ACH accounts.
     * In PRODUCTION mode: Returns empty lists (frontend should hide test helpers panel).
     */
    @GetMapping("/test-methods")
    @Operation(
        summary = "Get available test payment methods",
        description = """
            Returns test credit cards and ACH accounts that can be used in TEST mode.
            
            **Test Cards Behavior:**
            - Cards ending in specific patterns produce deterministic results
            - Some cards always approve, some always decline, some always error
            - Useful for testing different payment scenarios
            
            **Production Behavior:**
            - Returns empty arrays (no test methods available)
            - Frontend should hide test helpers panel
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Test payment methods retrieved successfully",
            content = @Content(schema = @Schema(implementation = TestPaymentMethodsResponse.class))
        ),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TestPaymentMethodsResponse> getTestPaymentMethods() {
        log.info("Fetching test payment methods");
        TestPaymentMethodsResponse response = mockPaymentProviderService.getTestPaymentMethods();
        return ResponseEntity.ok(response);
    }
}
