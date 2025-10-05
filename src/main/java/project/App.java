package project;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

public class App {
    private final DatabaseManager dbManager;
    private final DatabaseMigrator migrator;

    public App() {
        this.dbManager = DatabaseManager.getInstance();
        this.migrator = new DatabaseMigrator(dbManager);
    }

    public static void main(String[] args) {
        System.out.println("Запуск системы управления заказами...");
        System.out.println("==========================================");

        App app = new App();
        app.run();
    }

    public void run() {
        try {
            // Тестируем подключение к БД
            dbManager.testConnection();

            // Запускаем миграции БД (создание таблиц и данных)
            migrator.runMigrations();

            // Проверяем что ВСЕ 4 таблицы создались
            if (!migrator.checkDatabaseReady()) {
                System.err.println("База данных не готова. Не все таблицы созданы.");
                System.err.println("Завершение работы.");
                return;
            }

            // Дополнительная проверка данных в таблицах
            migrator.checkData();

            // Демонстрация всех CRUD операций в транзакции
            demonstrateAllOperations();

        } catch (Exception e) {
            System.err.println("Критическая ошибка: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Закрываем подключение к БД
            dbManager.closeConnection();
        }
    }

    private void demonstrateAllOperations() {
        try {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("          ДЕМОНСТРАЦИЯ CRUD ОПЕРАЦИЙ");
            System.out.println("=".repeat(60));

            // 1. Показываем существующие данные
            showExistingData();

            // 2. CREATE - Создание новых данных
            demonstrateCreateOperations();

            // 3. READ - Чтение и отображение данных
            demonstrateReadOperations();

            // 4. UPDATE - Обновление данных
            demonstrateUpdateOperations();

            // 5. DELETE - Удаление данных
            demonstrateDeleteOperations();

            // 6. Дополнительные аналитические запросы
            demonstrateAdditionalQueries();

            System.out.println("\n ВСЕ ОПЕРАЦИИ УСПЕШНО ЗАВЕРШЕНЫ!");

        } catch (SQLException e) {
            System.err.println("\n ОШИБКА: " + e.getMessage());
        }
    }

    private void showExistingData() throws SQLException {
        System.out.println("\n1. СУЩЕСТВУЮЩИЕ ДАННЫЕ");
        System.out.println("-".repeat(40));

        dbManager.printAllProducts();
        dbManager.printAllCustomers();
        dbManager.printRecentOrders(5);
    }

    private void demonstrateCreateOperations() throws SQLException {
        System.out.println("\n2. СОЗДАНИЕ НОВЫХ ДАННЫХ");
        System.out.println("-".repeat(40));

        // Создаем новый товар с уникальным именем
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uniqueSuffix = timestamp.substring(8); // Берем последние цифры

        Product newProduct = new Product(
                "Планшет Apple iPad Air " + uniqueSuffix,
                "Планшет с дисплеем Retina 10.9\" и чипом M1",
                new BigDecimal("55000.00"),
                8,
                "Электроника"
        );
        Long productId = dbManager.createProduct(newProduct);
        System.out.println("Создан новый товар с ID: " + productId);

        // Создаем нового клиента с УНИКАЛЬНЫМ email
        Customer newCustomer = new Customer(
                "Давид",
                "Давыдов",
                "+7916" + uniqueSuffix,
                "david.davidov." + uniqueSuffix + "@mail.ru"
        );

        Long customerId;
        try {
            customerId = dbManager.createCustomer(newCustomer);
            System.out.println("Создан новый клиент с ID: " + customerId);
        } catch (SQLException e) {
            if (e.getMessage().contains("duplicate key") || e.getMessage().contains("unique constraint")) {
                // Если email уже существует, пробуем другой
                System.out.println("Email уже существует, пробуем другой...");
                newCustomer.setEmail("david.davidov." + System.nanoTime() + "@mail.ru");
                customerId = dbManager.createCustomer(newCustomer);
                System.out.println("Создан новый клиент с ID: " + customerId);
            } else {
                throw e;
            }
        }

        // Создаем новый заказ (status_id = 1 - "Новый")
        Order newOrder = new Order(
                productId,
                customerId,
                1L, // Новый заказ
                1,
                new BigDecimal("55000.00")
        );
        Long orderId = dbManager.createOrder(newOrder);
        System.out.println("Создан новый заказ с ID: " + orderId);
    }

    private void demonstrateReadOperations() throws SQLException {
        System.out.println("\n3. ЧТЕНИЕ ДАННЫХ");
        System.out.println("-".repeat(40));

        // Читаем созданный товар
        Product product = dbManager.getProductById(11L); // 11-й товар (наш новый)
        if (product != null) {
            System.out.println("Прочитан товар: " + product);
        } else {
            System.out.println("Товар с ID 11 не найден, читаем ID 1");
            product = dbManager.getProductById(1L);
            if (product != null) {
                System.out.println("Прочитан товар: " + product);
            }
        }

        // Читаем созданного клиента
        Customer customer = dbManager.getCustomerById(11L); // 11-й клиент (наш новый)
        if (customer != null) {
            System.out.println("Прочитан клиент: " + customer);
        } else {
            System.out.println("Клиент с ID 11 не найден, читаем ID 1");
            customer = dbManager.getCustomerById(1L);
            if (customer != null) {
                System.out.println("Прочитан клиент: " + customer);
            }
        }

        // Показываем последние заказы (включая наш новый)
        System.out.println("\n Последние заказы (включая созданный):");
        dbManager.printRecentOrders(5);
    }

    private void demonstrateUpdateOperations() throws SQLException {
        System.out.println("\n4. ОБНОВЛЕНИЕ ДАННЫХ");
        System.out.println("-".repeat(40));

        // Обновляем цену товара (пробуем новый товар, если нет - первый)
        Long productIdToUpdate = 11L;
        Product productToUpdate = dbManager.getProductById(productIdToUpdate);
        if (productToUpdate == null) {
            productIdToUpdate = 1L;
            System.out.println("Товар с ID 11 не найден, обновляем товар с ID 1");
        }

        boolean priceUpdated = dbManager.updateProductPrice(productIdToUpdate, new BigDecimal("52000.00"));
        if (priceUpdated) {
            System.out.println("Цена товара обновлена (скидка 3000 руб)");
        } else {
            System.out.println("Не удалось обновить цену товара");
        }

        // Обновляем количество товара
        boolean quantityUpdated = dbManager.updateProductQuantity(productIdToUpdate, 7);
        if (quantityUpdated) {
            System.out.println(" Количество товара обновлено");
        } else {
            System.out.println(" Не удалось обновить количество товара");
        }

        // Показываем обновленный товар
        Product updatedProduct = dbManager.getProductById(productIdToUpdate);
        if (updatedProduct != null) {
            System.out.println(" Обновленный товар: " + updatedProduct);
        }
    }

    private void demonstrateDeleteOperations() throws SQLException {
        System.out.println("\n5.  УДАЛЕНИЕ ДАННЫХ");
        System.out.println("-".repeat(40));

        // Удаляем тестовый заказ (последний созданный)
        Long orderIdToDelete = 11L;
        boolean orderDeleted = dbManager.deleteOrder(orderIdToDelete);
        if (orderDeleted) {
            System.out.println("Тестовый заказ #" + orderIdToDelete + " удален");
        } else {
            // Если заказа с ID 11 нет, пробуем удалить последний существующий
            System.out.println("Заказ с ID " + orderIdToDelete + " не найден, удаляем заказ #10");
            orderDeleted = dbManager.deleteOrder(10L);
            if (orderDeleted) {
                System.out.println("Тестовый заказ #10 удален");
            } else {
                System.out.println("Не удалось удалить тестовый заказ");
            }
        }

        // Показываем заказы после удаления
        System.out.println("\n Заказы после удаления:");
        dbManager.printRecentOrders(5);
    }

    private void demonstrateAdditionalQueries() throws SQLException {
        System.out.println("\n6. ДОПОЛНИТЕЛЬНЫЕ АНАЛИТИЧЕСКИЕ ЗАПРОСЫ");
        System.out.println("-".repeat(50));

        // Популярные товары
        dbManager.printPopularProducts();

        // Статистика по заказам
        printOrderStatistics();

        // Статистика по клиентам
        printCustomerStatistics();
    }

    private void printOrderStatistics() throws SQLException {
        String sql = """
            SELECT 
                COUNT(*) as total_orders,
                SUM(total_amount) as total_revenue,
                AVG(total_amount) as avg_order_value,
                MIN(order_date) as first_order,
                MAX(order_date) as last_order
            FROM orders
            """;

        try (var conn = dbManager.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                System.out.println("\n СТАТИСТИКА ПО ЗАКАЗАМ:");
                System.out.printf("   Всего заказов: %d%n", rs.getInt("total_orders"));
                System.out.printf("   Общая выручка: %.2f руб%n", rs.getBigDecimal("total_revenue"));
                System.out.printf("   Средний чек: %.2f руб%n", rs.getBigDecimal("avg_order_value"));
                System.out.printf("   Первый заказ: %s%n", rs.getTimestamp("first_order"));
                System.out.printf("   Последний заказ: %s%n", rs.getTimestamp("last_order"));
            }
        }
    }

    private void printCustomerStatistics() throws SQLException {
        String sql = """
            SELECT 
                COUNT(*) as total_customers,
                COUNT(DISTINCT email) as unique_emails,
                COUNT(phone) as customers_with_phone
            FROM customers
            """;

        try (var conn = dbManager.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                System.out.println("\n СТАТИСТИКА ПО КЛИЕНТАМ:");
                System.out.printf("   Всего клиентов: %d%n", rs.getInt("total_customers"));
                System.out.printf("   Уникальных email: %d%n", rs.getInt("unique_emails"));
                System.out.printf("   Клиентов с телефоном: %d%n", rs.getInt("customers_with_phone"));
            }
        }
    }

    // Дополнительный метод для быстрой проверки работы приложения
    public void quickTest() {
        try {
            System.out.println("\n БЫСТРЫЙ ТЕСТ РАБОТОСПОСОБНОСТИ");
            System.out.println("-".repeat(40));

            // Простая проверка чтения данных
            Product firstProduct = dbManager.getProductById(1L);
            if (firstProduct != null) {
                System.out.println("Первый товар: " + firstProduct.getName());
            }

            Customer firstCustomer = dbManager.getCustomerById(1L);
            if (firstCustomer != null) {
                System.out.println("Первый клиент: " + firstCustomer.getFullName());
            }

            // Проверка количества записей
            try (var conn = dbManager.getConnection();
                 var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM orders")) {
                if (rs.next()) {
                    System.out.println("Количество заказов: " + rs.getInt("cnt"));
                }
            }

            System.out.println("Все основные операции работают корректно!");

        } catch (SQLException e) {
            System.err.println("Ошибка при быстром тесте: " + e.getMessage());
        }
    }
}