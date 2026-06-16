package co.ke.tezza.loanapp.controller;
//package co.ke.tezza.loanapp.controller;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import com.paypal.api.payments.Payment;
//import com.paypal.base.rest.PayPalRESTException;
//
//import co.ke.tezza.loanapp.paypal.config.PaypalPaymentIntent;
//import co.ke.tezza.loanapp.paypal.config.PaypalPaymentMethod;
//import co.ke.tezza.loanapp.service.PayPalPaymentService;
//
//import com.google.gson.JsonObject;
//
//import javax.servlet.http.HttpServletRequest;
//
//@RestController
//@RequestMapping("/api/paypal")
//public class PaypalPaymentController {
//
//    @Autowired
//    private PayPalPaymentService payPalService;
//
//    // ================================
//    //   1️⃣ OLD PAYPAL REDIRECT API
//    // ================================
//
//    @PostMapping("/create")
//    public String createPayment(
//            @RequestParam("total") Double total,
//            @RequestParam("description") String description,
//            HttpServletRequest req) {
//
//        String cancelUrl = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort()
//                + "/api/paypal/cancel";
//
//        String successUrl = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort()
//                + "/api/paypal/success";
//
//        try {
//            Payment payment = payPalService.createPayment(
//                    total,
//                    "USD",
//                    PaypalPaymentMethod.paypal,
//                   PaypalPaymentIntent.sale,
//                    description,
//                    cancelUrl,
//                    successUrl
//            );
//
//            // Redirect customer to approval link
//            return payment.getLinks().stream()
//                    .filter(link -> link.getRel().equals("approval_url"))
//                    .findFirst()
//                    .orElseThrow(() -> new RuntimeException("No approval URL"))
//                    .getHref();
//
//        } catch (PayPalRESTException e) {
//            return "Error: " + e.getMessage();
//        }
//    }
//
//    @GetMapping("/success")
//    public Object successPay(
//            @RequestParam("paymentId") String paymentId,
//            @RequestParam("PayerID") String payerId) {
//
//        try {
//            Payment payment = payPalService.executePayment(paymentId, payerId);
//            return payment;
//        } catch (Exception e) {
//            return "Error: " + e.getMessage();
//        }
//    }
//
//    @GetMapping("/cancel")
//    public String cancelPay() {
//        return "Payment Cancelled!";
//    }
//
//
//
//    // ==========================================
//    //   2️⃣ PAYPAL V2 — CARD PROCESSING API
//    // ==========================================
//
//    /** Create Order with PayPal V2 (Card Payments) */
//    @PostMapping("/v2/order")
//    public Object createOrder(
//            @RequestParam("amount") String amount) {
//
//        try {
//            String orderId = payPalService.createOrderV2(amount);
//
//            JsonObject response = new JsonObject();
//            response.addProperty("orderId", orderId);
//
//            return response;
//
//        } catch (Exception e) {
//            JsonObject error = new JsonObject();
//            error.addProperty("error", e.getMessage());
//            return error;
//        }
//    }
//
//    /** Capture Order after the card is processed */
//    @PostMapping("/v2/order/capture/{orderId}")
//    public Object captureOrder(@PathVariable String orderId) {
//
//        try {
//            return payPalService.captureOrderV2(orderId);
//        } catch (Exception e) {
//            JsonObject error = new JsonObject();
//            error.addProperty("error", e.getMessage());
//            return error;
//        }
//    }
//}
