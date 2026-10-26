package io.shehzeb.eop.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Command payloads for the order API. */
public final class OrderDtos {

    private OrderDtos() {
    }

    public record PlaceOrderRequest(
            @NotBlank(message = "customerId is required") String customerId,
            @NotNull @DecimalMin(value = "0.01", message = "amount must be positive") BigDecimal amount) {
    }

    public record PayOrderRequest(
            @NotBlank(message = "paymentReference is required") String paymentReference) {
    }

    public record CancelOrderRequest(
            @NotBlank(message = "reason is required") String reason) {
    }

    public record OrderAccepted(String orderId, String status) {
    }
}
