package project;

import java.sql.*;

public class DatabaseMigrator {
    private final DatabaseManager dbManager;

    public DatabaseMigrator(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void runMigrations() {
        System.out.println("Запуск миграций базы данных...");

        try {
            // Создаем таблицы по одной с проверками
            createTableOrderStatus();
            createTableProducts();
            createTableCustomers();
            createTableOrders();
            createIndexes();

            // Вставляем данные с проверкой на существование
            insertTestDataSafe();

            System.out.println("Все миграции успешно завершены!");

        } catch (SQLException e) {
            System.err.println("Ошибка миграций: " + e.getMessage());
        }
    }

    private void createTableOrderStatus() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS order_status (
                id SERIAL PRIMARY KEY,
                name VARCHAR(50) NOT NULL UNIQUE
            )
            """;
        executeSQL(sql, "order_status");
    }

    private void createTableProducts() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS products (
                id SERIAL PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                description TEXT,
                price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
                quantity INTEGER NOT NULL CHECK (quantity >= 0),
                category VARCHAR(100),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        executeSQL(sql, "products");
    }

    private void createTableCustomers() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS customers (
                id SERIAL PRIMARY KEY,
                first_name VARCHAR(50) NOT NULL,
                last_name VARCHAR(50) NOT NULL,
                phone VARCHAR(20),
                email VARCHAR(100) UNIQUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        executeSQL(sql, "customers");
    }

    private void createTableOrders() throws SQLException {
        // Сначала создаем без foreign keys
        String sql = """
            CREATE TABLE IF NOT EXISTS orders (
                id SERIAL PRIMARY KEY,
                product_id INTEGER NOT NULL,
                customer_id INTEGER NOT NULL,
                status_id INTEGER NOT NULL,
                quantity INTEGER NOT NULL CHECK (quantity > 0),
                total_amount DECIMAL(10,2) NOT NULL CHECK (total_amount >= 0),
                order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        executeSQL(sql, "orders");

        // Потом добавляем foreign keys
        addForeignKey("orders", "product_id", "products", "id", "fk_order_product");
        addForeignKey("orders", "customer_id", "customers", "id", "fk_order_customer");
        addForeignKey("orders", "status_id", "order_status", "id", "fk_order_status");
    }

    private void createIndexes() throws SQLException {
        String[] indexes = {
                "CREATE INDEX IF NOT EXISTS idx_orders_product_id ON orders(product_id)",
                "CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders(customer_id)",
                "CREATE INDEX IF NOT EXISTS idx_orders_status_id ON orders(status_id)",
                "CREATE INDEX IF NOT EXISTS idx_orders_date ON orders(order_date)",
                "CREATE INDEX IF NOT EXISTS idx_products_category ON products(category)",
                "CREATE INDEX IF NOT EXISTS idx_customers_email ON customers(email)"
        };

        for (String index : indexes) {
            executeSQL(index, "index");
        }
        System.out.println("Все индексы созданы/проверены");
    }

    private void insertTestDataSafe() throws SQLException {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // Проверяем, есть ли уже данные в таблицах
            if (!hasData(conn, "order_status")) {
                insertOrderStatusData(stmt);
            } else {
                System.out.println("Данные order_status уже существуют");
            }

            if (!hasData(conn, "products")) {
                insertProductsData(stmt);
            } else {
                System.out.println("Данные products уже существуют");
            }

            if (!hasData(conn, "customers")) {
                insertCustomersData(stmt);
            } else {
                System.out.println("Данные customers уже существуют");
            }

            if (!hasData(conn, "orders")) {
                insertOrdersData(stmt);
            } else {
                System.out.println("Данные orders уже существуют");
            }
        }
    }

    private boolean hasData(Connection conn, String tableName) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM " + tableName)) {
            return rs.next() && rs.getInt("cnt") > 0;
        }
    }

    private void insertOrderStatusData(Statement stmt) throws SQLException {
        String sql = """
            INSERT INTO order_status (name) VALUES 
                ('Новый'), ('Подтвержден'), ('В обработке'), ('Отправлен'), ('Доставлен'), ('Отменен')
            ON CONFLICT (name) DO NOTHING
            """;
        stmt.execute(sql);
        System.out.println("Данные order_status добавлены");
    }

    private void insertProductsData(Statement stmt) throws SQLException {
        // Очищаем таблицу перед вставкой (если нужно)
        try {
            stmt.execute("DELETE FROM products");
            System.out.println("Таблица products очищена");
        } catch (SQLException e) {
            System.out.println("Не удалось очистить products: " + e.getMessage());
        }

        String sql = """
            INSERT INTO products (name, description, price, quantity, category) VALUES
                ('Ноутбук Lenovo IdeaPad', '15-дюймовый ноутбук с процессором Intel i5', 45000.00, 15, 'Электроника'),
                ('Смартфон Samsung Galaxy', 'Смартфон с AMOLED дисплеем 6.1"', 35000.00, 25, 'Электроника'),
                ('Наушники Sony WH-1000XM4', 'Беспроводные наушники с шумоподавлением', 25000.00, 30, 'Электроника'),
                ('Книга "Java для начинающих"', 'Учебник по программированию на Java', 1500.00, 50, 'Книги'),
                ('Кофемашина DeLonghi', 'Автоматическая кофемашина для дома', 30000.00, 10, 'Бытовая техника'),
                ('Футболка хлопковая', 'Хлопковая футболка унисекс', 1200.00, 100, 'Одежда'),
                ('Кроссовки Nike Air Max', 'Спортивные кроссовки для бега', 8000.00, 40, 'Обувь'),
                ('Чайник электрический', 'Стеклянный электрочайник 1.7л', 2500.00, 35, 'Бытовая техника'),
                ('Мышь беспроводная Logitech', 'Беспроводная компьютерная мышь', 1500.00, 60, 'Электроника'),
                ('Монитор 24" Dell', 'Монитор с IPS матрицей 1920x1080', 20000.00, 20, 'Электроника')
            """;
        stmt.execute(sql);
        System.out.println("Данные products добавлены (10 записей)");
    }

    private void insertCustomersData(Statement stmt) throws SQLException {
        // Очищаем таблицу перед вставкой (если нужно)
        try {
            stmt.execute("DELETE FROM customers");
            System.out.println("Таблица customers очищена");
        } catch (SQLException e) {
            System.out.println("Не удалось очистить customers: " + e.getMessage());
        }

        String sql = """
            INSERT INTO customers (first_name, last_name, phone, email) VALUES
                ('Иван', 'Петров', '+79161234567', 'ivan.petrov@mail.ru'),
                ('Мария', 'Сидорова', '+79167654321', 'maria.sidorova@mail.ru'),
                ('Алексей', 'Козлов', '+79169998877', 'alex.kozlov@mail.ru'),
                ('Елена', 'Новикова', '+79165554433', 'elena.novikova@mail.ru'),
                ('Дмитрий', 'Волков', '+79162223344', 'dmitry.volkov@mail.ru'),
                ('Ольга', 'Морозова', '+79163332211', 'olga.morozova@mail.ru'),
                ('Сергей', 'Павлов', '+79164445566', 'sergey.pavlov@mail.ru'),
                ('Анна', 'Лебедева', '+79167778899', 'anna.lebedeva@mail.ru'),
                ('Михаил', 'Семенов', '+79168889900', 'mikhail.semenov@mail.ru'),
                ('Наталья', 'Орлова', '+79161112233', 'natalia.orlova@mail.ru')
            """;
        stmt.execute(sql);
        System.out.println("Данные customers добавлены (10 записей)");
    }

    private void insertOrdersData(Statement stmt) throws SQLException {
        // Очищаем таблицу перед вставкой (если нужно)
        try {
            stmt.execute("DELETE FROM orders");
            System.out.println("Таблица orders очищена");
        } catch (SQLException e) {
            System.out.println("Не удалось очистить orders: " + e.getMessage());
        }

        String sql = """
            INSERT INTO orders (product_id, customer_id, status_id, quantity, total_amount) VALUES
                (1, 1, 1, 1, 45000.00),
                (2, 2, 2, 1, 35000.00),
                (3, 3, 3, 2, 50000.00),
                (4, 4, 4, 3, 4500.00),
                (5, 5, 5, 1, 30000.00),
                (6, 6, 1, 5, 6000.00),
                (7, 7, 2, 1, 8000.00),
                (8, 8, 3, 2, 5000.00),
                (9, 9, 4, 1, 1500.00),
                (10, 10, 5, 1, 20000.00)
            """;
        stmt.execute(sql);
        System.out.println("Данные orders добавлены (10 записей)");
    }

    private void executeSQL(String sql, String operation) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println(" " + operation + " - выполнено");
        } catch (SQLException e) {
            // Игнорируем ошибки "уже существует"
            if (e.getMessage().contains("already exists") || e.getMessage().contains("существует")) {
                System.out.println(" " + operation + " - уже существует");
            } else {
                throw e;
            }
        }
    }

    private void addForeignKey(String table, String column, String refTable, String refColumn, String constraintName) throws SQLException {
        String sql = "ALTER TABLE " + table + " ADD CONSTRAINT " + constraintName +
                " FOREIGN KEY (" + column + ") REFERENCES " + refTable + "(" + refColumn + ")";
        executeSQL(sql, "Foreign key " + constraintName);
    }

    public boolean checkDatabaseReady() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            String checkTables = """
                SELECT COUNT(*) as table_count 
                FROM information_schema.tables 
                WHERE table_schema = 'public' 
                AND table_name IN ('order_status', 'products', 'customers', 'orders')
                """;

            ResultSet rs = stmt.executeQuery(checkTables);
            if (rs.next()) {
                int tableCount = rs.getInt("table_count");
                System.out.println("Найдено таблиц: " + tableCount + "/4");
                return tableCount == 4;
            }

        } catch (SQLException e) {
            System.err.println("Ошибка проверки БД: " + e.getMessage());
        }
        return false;
    }

    public void checkData() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            System.out.println("\n ПРОВЕРКА ДАННЫХ:");

            String[] checks = {
                    "SELECT COUNT(*) as count FROM order_status",
                    "SELECT COUNT(*) as count FROM products",
                    "SELECT COUNT(*) as count FROM customers",
                    "SELECT COUNT(*) as count FROM orders"
            };

            String[] tableNames = {"order_status", "products", "customers", "orders"};

            for (int i = 0; i < checks.length; i++) {
                try {
                    ResultSet rs = stmt.executeQuery(checks[i]);
                    if (rs.next()) {
                        System.out.println(" " + tableNames[i] + ": " + rs.getInt("count") + " записей");
                    }
                } catch (SQLException e) {
                    System.out.println(" " + tableNames[i] + ": ошибка - " + e.getMessage());
                }
            }

        } catch (SQLException e) {
            System.err.println("Ошибка проверки данных: " + e.getMessage());
        }
    }

    // Дополнительный метод для принудительного сброса данных
    public void resetData() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            System.out.println("Принудительный сброс данных...");

            // Очищаем таблицы в правильном порядке
            stmt.execute("DELETE FROM orders");
            stmt.execute("DELETE FROM customers");
            stmt.execute("DELETE FROM products");
            stmt.execute("DELETE FROM order_status");

            // Сбрасываем sequences
            stmt.execute("ALTER SEQUENCE orders_id_seq RESTART WITH 1");
            stmt.execute("ALTER SEQUENCE customers_id_seq RESTART WITH 1");
            stmt.execute("ALTER SEQUENCE products_id_seq RESTART WITH 1");
            stmt.execute("ALTER SEQUENCE order_status_id_seq RESTART WITH 1");

            System.out.println("Все данные сброшены, sequences перезапущены");

            // Заполняем данные заново
            insertTestDataSafe();

        } catch (SQLException e) {
            System.err.println("Ошибка при сбросе данных: " + e.getMessage());
        }
    }
}