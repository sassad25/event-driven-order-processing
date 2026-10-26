package io.shehzeb.eop.api;

import io.shehzeb.eop.readmodel.OrderSummary;
import io.shehzeb.eop.readmodel.OrderSummaryRepository;
import io.shehzeb.eop.service.OrderCommandService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Order API. Commands (POST) are accepted and acknowledged immediately; the resulting events are
 * processed asynchronously by the Kafka consumer. The GET endpoint serves the projected read model.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderCommandService commandService;
    private final OrderSummaryRepository summaryRepository;

    public OrderController(OrderCommandService commandService, OrderSummaryRepository summaryRepository) {
        this.commandService = commandService;
        this.summaryRepository = summaryRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderDtos.OrderAccepted place(@Valid @RequestBody OrderDtos.PlaceOrderRequest request) {
        UUID id = commandService.placeOrder(request.customerId(), request.amount());
        return new OrderDtos.OrderAccepted(id.toString(), "PLACED");
    }

    @PostMapping("/{id}/payment")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderDtos.OrderAccepted pay(@PathVariable UUID id, @Valid @RequestBody OrderDtos.PayOrderRequest request) {
        commandService.payOrder(id, request.paymentReference());
        return new OrderDtos.OrderAccepted(id.toString(), "PAID");
    }

    @PostMapping("/{id}/cancellation")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderDtos.OrderAccepted cancel(@PathVariable UUID id, @Valid @RequestBody OrderDtos.CancelOrderRequest request) {
        commandService.cancelOrder(id, request.reason());
        return new OrderDtos.OrderAccepted(id.toString(), "CANCELLED");
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderSummary> get(@PathVariable UUID id) {
        return summaryRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
