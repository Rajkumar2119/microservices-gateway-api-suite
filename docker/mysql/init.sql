-- Each service owns its own database (database-per-service).
CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS order_db;
GRANT ALL PRIVILEGES ON auth_db.* TO 'app'@'%';
GRANT ALL PRIVILEGES ON order_db.* TO 'app'@'%';
FLUSH PRIVILEGES;
