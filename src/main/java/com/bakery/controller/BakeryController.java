package com.bakery.controller;

import com.bakery.dto.OrderItemRequest;
import com.bakery.dto.OrderRequest;
import com.bakery.dto.OrderStatusUpdate;
import com.bakery.model.BakeryOrder;
import com.bakery.model.MenuItem;
import com.bakery.model.OrderItem;
import com.bakery.model.OrderStatus;
import com.bakery.model.UpiSettings;
import com.bakery.repository.MenuItemRepository;
import com.bakery.repository.OrderRepository;
import com.bakery.repository.UpiSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow any origin for simple development
public class BakeryController {

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UpiSettingsRepository upiRepository;

    @GetMapping("/menu")
    public List<MenuItem> getMenu() {
        return menuItemRepository.findAll();
    }

    @PostMapping("/menu")
    public MenuItem addMenuItem(@NonNull @RequestBody MenuItem menuItem) {
        return menuItemRepository.save(menuItem);
    }

    @PutMapping("/menu/{id}")
    public ResponseEntity<MenuItem> updateMenuItem(@NonNull @PathVariable(name = "id") Long id, @RequestBody MenuItem updatedItem) {
        return menuItemRepository.findById(id).map(item -> {
            item.setName(updatedItem.getName());
            item.setDescription(updatedItem.getDescription());
            item.setPrice(updatedItem.getPrice());
            item.setImageUrl(updatedItem.getImageUrl());
            item.setCategory(updatedItem.getCategory());
            item.setAvailable(updatedItem.isAvailable());
            return ResponseEntity.ok(menuItemRepository.save(item));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/menu/{id}")
    public ResponseEntity<Void> deleteMenuItem(@NonNull @PathVariable(name = "id") Long id) {
        if (menuItemRepository.existsById(id)) {
            menuItemRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/orders")
    public BakeryOrder placeOrder(@RequestBody OrderRequest orderRequest) {
        BakeryOrder order = new BakeryOrder();
        order.setTableNumber(orderRequest.getTableNumber());
        order.setCustomerName(orderRequest.getCustomerName());
        order.setDepartment(orderRequest.getDepartment());
        order.setCustomerYear(orderRequest.getCustomerYear());
        order.setPaymentMethod(orderRequest.getPaymentMethod());
        order.setOrderTime(LocalDateTime.now());
        order.setStatus(OrderStatus.PAYMENT_PENDING);

        double total = 0;
        for (OrderItemRequest itemReq : orderRequest.getItems()) {
            Long queryId = itemReq.getMenuItemId();
            @SuppressWarnings("null")
            MenuItem menuItem = menuItemRepository.findById(queryId)
                    .orElseThrow(() -> new RuntimeException("Menu item not found"));

            OrderItem orderItem = new OrderItem();
            orderItem.setBakeryOrder(order);
            orderItem.setMenuItem(menuItem);
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setPrice(menuItem.getPrice());

            total += (menuItem.getPrice() * itemReq.getQuantity());
            order.getItems().add(orderItem);
        }

        order.setTotalPrice(total);
        return orderRepository.save(order);
    }

    @GetMapping("/orders")
    public List<BakeryOrder> getAllOrders() {
        return orderRepository.findAll();
    }

    @GetMapping("/orders/table/{tableNumber}")
    public List<BakeryOrder> getOrdersByTable(@PathVariable(name = "tableNumber") String tableNumber) {
        return orderRepository.findByTableNumberOrderByOrderTimeDesc(tableNumber);
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<BakeryOrder> updateOrderStatus(
            @NonNull @PathVariable(name = "id") Long id,
            @RequestBody OrderStatusUpdate statusUpdate) {

        return orderRepository.findById(id).map(order -> {
            order.setStatus(statusUpdate.getStatus());
            BakeryOrder saved = orderRepository.save(order);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/orders/{id}/confirm-payment")
    public ResponseEntity<BakeryOrder> confirmPayment(@NonNull @PathVariable(name = "id") Long id) {
        return orderRepository.findById(id).map(order -> {
            if (order.getStatus() == OrderStatus.PAYMENT_PENDING) {
                order.setStatus(OrderStatus.PENDING);
                BakeryOrder saved = orderRepository.save(order);
                return ResponseEntity.ok(saved);
            }
            return ResponseEntity.ok(order);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/settings/upi")
    public ResponseEntity<UpiSettings> getUpiSettings() {
        return upiRepository.findFirstByOrderByIdAsc()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/settings/upi")
    public ResponseEntity<UpiSettings> updateUpiSettings(@RequestBody UpiSettings updatedSettings) {
        return upiRepository.findFirstByOrderByIdAsc()
                .map(settings -> {
                    settings.setUpiId(updatedSettings.getUpiId());
                    settings.setRecipientName(updatedSettings.getRecipientName());
                    settings.setMerchantName(updatedSettings.getMerchantName());
                    return ResponseEntity.ok(upiRepository.save(settings));
                })
                .orElseGet(() -> ResponseEntity.ok(upiRepository.save(updatedSettings)));
    }
}
