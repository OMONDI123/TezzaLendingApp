package co.ke.tezza.loanapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.ke.tezza.loanapp.service.PaymentsService;

@RestController
@RequestMapping("/api/payments")
public class PaymentCallbackController {
    
    @Autowired
    private PaymentsService paymentsService;
    
    @PostMapping("/sTKCallback")
    public ResponseEntity<String> handleMpesaCallback(@RequestBody String callbackResponse) {
        try {
            paymentsService.handleMpesaCallback(callbackResponse);
            return ResponseEntity.ok("Callback processed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing callback");
        }
    }
    @PostMapping("/c2BCallback")
    public ResponseEntity<String> handleC2BCallback(@RequestBody String payload) {
        try {
            paymentsService.handleC2BCallback(payload);
            return ResponseEntity.ok("C2B callback processed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing C2B callback: " + e.getMessage());
        }
    }
    
    @PostMapping("/c2BCallbackValidation")
    public ResponseEntity<String> mpesaC2BCallbackValidation(@RequestBody String payload) {
        try {
            //paymentsService.handleC2BCallback(payload);
            return ResponseEntity.ok("C2B callback processed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing C2B callback validation: " + e.getMessage());
        }
    }
}

