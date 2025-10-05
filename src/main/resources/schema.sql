BEGIN TRANSACTION;

-- 1. Таблица статусов заказов
CREATE TABLE IF NOT EXISTS order_status (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- 2. Таблица товаров
CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    quantity INTEGER NOT NULL CHECK (quantity >= 0),
    category VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Таблица клиентов
CREATE TABLE IF NOT EXISTS customers (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Таблица заказов
CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    product_id INTEGER NOT NULL,
    customer_id INTEGER NOT NULL,
    status_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    total_amount DECIMAL(10,2) NOT NULL CHECK (total_amount >= 0),
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (status_id) REFERENCES order_status(id)
);

-- 5. Индексы для улучшения производительности
CREATE INDEX IF NOT EXISTS idx_orders_product_id ON orders(product_id);
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status_id ON orders(status_id);
CREATE INDEX IF NOT EXISTS idx_orders_date ON orders(order_date);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_customers_email ON customers(email);

-- 6. Заполнение таблицы статусов заказов
INSERT INTO order_status (name) VALUES
    ('Новый'),
    ('Подтвержден'),
    ('В обработке'),
    ('Отправлен'),
    ('Доставлен'),
    ('Отменен')
ON CONFLICT (name) DO NOTHING;

-- 7. Заполнение таблицы товаров
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
ON CONFLICT (id) DO NOTHING;

-- 8. Заполнение таблицы клиентов
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
ON CONFLICT (id) DO NOTHING;

-- 9. Заполнение таблицы заказов
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
ON CONFLICT (id) DO NOTHING;

-- Завершаем транзакцию
COMMIT;

-- 10. Проверочный запрос (после COMMIT)
SELECT
    'order_status' as table_name,
    COUNT(*) as record_count
FROM order_status
UNION ALL
SELECT
    'products',
    COUNT(*)
FROM products
UNION ALL
SELECT
    'customers',
    COUNT(*)
FROM customers
UNION ALL
SELECT
    'orders',
    COUNT(*)
FROM orders;