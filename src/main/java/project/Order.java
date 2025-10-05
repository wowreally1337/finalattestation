package project;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Order {
    private Long id;
    private Long productId;
    private Long customerId;
    private Long statusId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private LocalDateTime orderDate;

    // Дополнительные поля для отображения (не из БД)
    private String productName;
    private String customerName;
    private String statusName;

    // Конструкторы
    public Order() {}

    public Order(Long productId, Long customerId, Long statusId, Integer quantity, BigDecimal totalAmount) {
        this.productId = productId;
        this.customerId = customerId;
        this.statusId = statusId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getStatusId() { return statusId; }
    public void setStatusId(Long statusId) { this.statusId = statusId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }

    @Override
    public String toString() {
        if (productName != null && customerName != null) {
            return String.format("Order[ID: %d, Product: %s, Customer: %s, Quantity: %d, Amount: %.2f, Status: %s, Date: %s]",
                    id, productName, customerName, quantity, totalAmount, statusName, orderDate);
        } else {
            return String.format("Order[ID: %d, ProductID: %d, CustomerID: %d, Quantity: %d, Amount: %.2f, StatusID: %d]",
                    id, productId, customerId, quantity, totalAmount, statusId);
        }
    }
}