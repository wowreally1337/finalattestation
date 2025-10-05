package project;

import java.sql.*;
import java.util.Properties;
import java.io.InputStream;

public class DatabaseManager {
    private static DatabaseManager instance;
    private String url;
    private String username;
    private String password;
    private Connection connection;

    private DatabaseManager() {
        loadProperties();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            Properties props = new Properties();
            if (input == null) {
                System.out.println("application.properties не найден, используются значения по умолчанию");
                this.url = "jdbc:postgresql://localhost:5432/order_db";
                this.username = "postgres";
                this.password = "password";
                return;
            }
            props.load(input);

            this.url = props.getProperty("db.url");
            this.username = props.getProperty("db.username");
            this.password = props.getProperty("db.password");

        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки настроек БД", e);
        }
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            // Явно регистрируем драйвер
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("PostgreSQL драйвер не найден", e);
            }
            connection = DriverManager.getConnection(url, username, password);
        }
        return connection;
    }

    public void testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("Тест подключения: УСПЕШНО");
        } catch (SQLException e) {
            System.err.println("Тест подключения: ОШИБКА - " + e.getMessage());
        }
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Подключение к БД закрыто");
            } catch (SQLException e) {
                System.err.println("Ошибка при закрытии подключения: " + e.getMessage());
            }
        }
    }

    // CRUD операции для продуктов
    public Long createProduct(Product product) throws SQLException {
        String sql = "INSERT INTO products (name, description, price, quantity, category) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, product.getName());
            stmt.setString(2, product.getDescription());
            stmt.setBigDecimal(3, product.getPrice());
            stmt.setInt(4, product.getQuantity());
            stmt.setString(5, product.getCategory());

            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                return keys.getLong(1);
            }
            throw new SQLException("Не удалось получить ID продукта");
        }
    }

    public Product getProductById(Long id) throws SQLException {
        String sql = "SELECT * FROM products WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Product product = new Product();
                product.setId(rs.getLong("id"));
                product.setName(rs.getString("name"));
                product.setDescription(rs.getString("description"));
                product.setPrice(rs.getBigDecimal("price"));
                product.setQuantity(rs.getInt("quantity"));
                product.setCategory(rs.getString("category"));
                product.setCreatedAt(rs.getTimestamp("created_at") != null ?
                        rs.getTimestamp("created_at").toLocalDateTime() : null);
                return product;
            }
            return null;
        }
    }

    // CRUD операции для клиентов
    public Long createCustomer(Customer customer) throws SQLException {
        String sql = "INSERT INTO customers (first_name, last_name, phone, email) VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, customer.getFirstName());
            stmt.setString(2, customer.getLastName());
            stmt.setString(3, customer.getPhone());
            stmt.setString(4, customer.getEmail());

            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                return keys.getLong(1);
            }
            throw new SQLException("Не удалось получить ID клиента");
        } catch (SQLException e) {
            // Пробрасываем исключение с более понятным сообщением
            if (e.getMessage().contains("duplicate key") || e.getMessage().contains("unique constraint")) {
                throw new SQLException("Клиент с email '" + customer.getEmail() + "' уже существует", e);
            }
            throw e;
        }
    }

    public Customer getCustomerById(Long id) throws SQLException {
        String sql = "SELECT * FROM customers WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Customer customer = new Customer();
                customer.setId(rs.getLong("id"));
                customer.setFirstName(rs.getString("first_name"));
                customer.setLastName(rs.getString("last_name"));
                customer.setPhone(rs.getString("phone"));
                customer.setEmail(rs.getString("email"));
                customer.setCreatedAt(rs.getTimestamp("created_at") != null ?
                        rs.getTimestamp("created_at").toLocalDateTime() : null);
                return customer;
            }
            return null;
        }
    }

    // CRUD операции для заказов
    public Long createOrder(Order order) throws SQLException {
        String sql = "INSERT INTO orders (product_id, customer_id, status_id, quantity, total_amount) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, order.getProductId());
            stmt.setLong(2, order.getCustomerId());
            stmt.setLong(3, order.getStatusId());
            stmt.setInt(4, order.getQuantity());
            stmt.setBigDecimal(5, order.getTotalAmount());

            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                return keys.getLong(1);
            }
            throw new SQLException("Не удалось получить ID заказа");
        }
    }

    public boolean deleteOrder(Long id) throws SQLException {
        String sql = "DELETE FROM orders WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    // Методы для отображения данных
    public void printAllProducts() throws SQLException {
        String sql = "SELECT * FROM products ORDER BY id";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n ВСЕ ТОВАРЫ:");
            while (rs.next()) {
                System.out.printf("ID: %d | %s | Цена: %.2f | Кол-во: %d | Категория: %s%n",
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getBigDecimal("price"),
                        rs.getInt("quantity"),
                        rs.getString("category"));
            }
        }
    }

    public void printAllCustomers() throws SQLException {
        String sql = "SELECT * FROM customers ORDER BY id";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n ВСЕ КЛИЕНТЫ:");
            while (rs.next()) {
                System.out.printf("ID: %d | %s %s | Email: %s | Телефон: %s%n",
                        rs.getLong("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("phone"));
            }
        }
    }

    public void printRecentOrders(int limit) throws SQLException {
        String sql = """
            SELECT o.id, p.name as product_name, 
                   c.first_name || ' ' || c.last_name as customer_name,
                   os.name as status_name,
                   o.quantity, o.total_amount, o.order_date
            FROM orders o
            JOIN products p ON o.product_id = p.id
            JOIN customers c ON o.customer_id = c.id
            JOIN order_status os ON o.status_id = os.id
            ORDER BY o.order_date DESC
            LIMIT ?
            """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            System.out.println("\n ПОСЛЕДНИЕ ЗАКАЗЫ:");
            while (rs.next()) {
                System.out.printf("Заказ #%d | Товар: %s | Клиент: %s | Кол-во: %d | Сумма: %.2f | Статус: %s | Дата: %s%n",
                        rs.getLong("id"),
                        rs.getString("product_name"),
                        rs.getString("customer_name"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("total_amount"),
                        rs.getString("status_name"),
                        rs.getTimestamp("order_date"));
            }
        }
    }

    public boolean updateProductPrice(Long productId, java.math.BigDecimal newPrice) throws SQLException {
        String sql = "UPDATE products SET price = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, newPrice);
            stmt.setLong(2, productId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateProductQuantity(Long productId, Integer newQuantity) throws SQLException {
        String sql = "UPDATE products SET quantity = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, newQuantity);
            stmt.setLong(2, productId);
            return stmt.executeUpdate() > 0;
        }
    }

    public void printPopularProducts() throws SQLException {
        String sql = """
            SELECT p.name, p.category, 
                   SUM(o.quantity) as total_sold,
                   SUM(o.total_amount) as total_revenue
            FROM products p
            JOIN orders o ON p.id = o.product_id
            GROUP BY p.id, p.name, p.category
            ORDER BY total_sold DESC
            LIMIT 5
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n ПОПУЛЯРНЫЕ ТОВАРЫ:");
            while (rs.next()) {
                System.out.printf("Товар: %s | Категория: %s | Продано: %d | Выручка: %.2f%n",
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getInt("total_sold"),
                        rs.getBigDecimal("total_revenue"));
            }
        }
    }
}