-- ================================================
--  LOST & FOUND FPT CAMPUS+  (MySQL schema v1.0)
--  Author: ChatGPT (for Duy Dũng)
--  Description: Full database structure for MVC Android app
-- ================================================

CREATE DATABASE IF NOT EXISTS lostfound_fptcampus
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lostfound_fptcampus;

-- ------------------------------------------------
-- 1. USERS
-- ------------------------------------------------
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid            CHAR(36) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    phone           VARCHAR(20),
    avatar_url      VARCHAR(255),
    karma           INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ------------------------------------------------
-- 2. ROLES
-- ------------------------------------------------
CREATE TABLE roles (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        ENUM('student','helper','admin') NOT NULL UNIQUE,
    description VARCHAR(255)
);

INSERT INTO roles (name, description) VALUES
('student', 'Sinh viên thường'),
('helper', 'Sinh viên uy tín hỗ trợ cộng đồng'),
('admin', 'Quản trị viên hệ thống');

-- ------------------------------------------------
-- 3. USER_ROLES (many-to-many)
-- ------------------------------------------------
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- ------------------------------------------------
-- 4. ITEMS
-- ------------------------------------------------
CREATE TABLE items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid            CHAR(36) NOT NULL UNIQUE,
    user_id         BIGINT NOT NULL,
    title           VARCHAR(150) NOT NULL,
    description     TEXT,
    category        VARCHAR(50),
    status          ENUM('lost','found','returned') DEFAULT 'lost',
    latitude        DECIMAL(10,6),
    longitude       DECIMAL(10,6),
    image_url       VARCHAR(255),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
);

-- ------------------------------------------------
-- 5. PHOTOS
-- ------------------------------------------------
CREATE TABLE photos (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id     BIGINT NOT NULL,
    url         VARCHAR(255) NOT NULL,
    is_primary  BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    INDEX idx_item_id (item_id)
);

-- ------------------------------------------------
-- 6. HISTORIES (QR confirmations)
-- ------------------------------------------------
CREATE TABLE histories (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id         BIGINT NOT NULL,
    giver_id        BIGINT,
    receiver_id     BIGINT,
    qr_token        CHAR(64) NOT NULL,
    confirmed_at    TIMESTAMP NULL,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    FOREIGN KEY (giver_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE SET NULL
);

-- ------------------------------------------------
-- 7. KARMA LOGS
-- ------------------------------------------------
CREATE TABLE karma_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    change_value    INT NOT NULL,
    reason          VARCHAR(255),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ------------------------------------------------
-- 8. NOTIFICATIONS
-- ------------------------------------------------
CREATE TABLE notifications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT,
    title           VARCHAR(150),
    body            TEXT,
    is_read         BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ------------------------------------------------
-- DONE
-- ------------------------------------------------
SHOW TABLES;

-- ================================================
--  LOST & FOUND FPT CAMPUS+  (Fake Data v1.0)
--  Author: ChatGPT (for Duy Dũng)
-- ================================================

USE lostfound_fptcampus;

-- ------------------------------------------------
-- ROLES
-- ------------------------------------------------
INSERT INTO roles (id, name, description) VALUES
(1, 'student', 'Sinh viên thường'),
(2, 'helper', 'Sinh viên uy tín hỗ trợ cộng đồng'),
(3, 'admin', 'Quản trị viên hệ thống')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ------------------------------------------------
-- USERS
-- ------------------------------------------------
INSERT INTO users (id, uuid, name, email, password_hash, phone, avatar_url, karma, created_at)
VALUES
(1, UUID(), 'Nguyễn Văn An', 'an@fpt.edu.vn', SHA2('123456',256), '0905123456', 'https://cdn.vietsuky.com/avatar/an.jpg', 20, NOW()),
(2, UUID(), 'Trần Thị Bình', 'binh@fpt.edu.vn', SHA2('123456',256), '0905789123', 'https://cdn.vietsuky.com/avatar/binh.jpg', 120, NOW()),
(3, UUID(), 'Phạm Minh Cường', 'cuong@fpt.edu.vn', SHA2('123456',256), '0905234789', 'https://cdn.vietsuky.com/avatar/cuong.jpg', 0, NOW()),
(4, UUID(), 'Lê Hồng Dung', 'dung@fpt.edu.vn', SHA2('123456',256), '0905000999', 'https://cdn.vietsuky.com/avatar/dung.jpg', 300, NOW());

-- ------------------------------------------------
-- USER_ROLES
-- ------------------------------------------------
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1),
(2, 1),
(2, 2),
(3, 1),
(4, 1),
(4, 2),
(4, 3);

-- ------------------------------------------------
-- ITEMS (đồ thất lạc & nhặt được)
-- ------------------------------------------------
INSERT INTO items (id, uuid, user_id, title, description, category, status, latitude, longitude, image_url, created_at)
VALUES
(1, UUID(), 1, 'Ví sinh viên màu đen', 'Rơi tại căn tin khu A lúc 9h sáng', 'wallet', 'lost', 10.762622, 106.682223, 'https://cdn.vietsuky.com/lost/wallet_black.jpg', NOW()),
(2, UUID(), 2, 'Tai nghe AirPods Pro', 'Nhặt được gần thư viện hôm qua', 'earphone', 'found', 10.762910, 106.682800, 'https://cdn.vietsuky.com/found/airpods.jpg', NOW()),
(3, UUID(), 3, 'Thẻ sinh viên FPT', 'Rơi ở khu B – ghi tên Cường', 'card', 'returned', 10.762300, 106.681900, 'https://cdn.vietsuky.com/lost/student_card.jpg', NOW()),
(4, UUID(), 4, 'Áo khoác xanh FPT', 'Nhặt tại sân bóng chiều qua', 'clothes', 'found', 10.762100, 106.683000, 'https://cdn.vietsuky.com/found/jacket_green.jpg', NOW());

-- ------------------------------------------------
-- PHOTOS
-- ------------------------------------------------
INSERT INTO photos (item_id, url, is_primary) VALUES
(1, 'https://cdn.vietsuky.com/lost/wallet_closeup.jpg', TRUE),
(1, 'https://cdn.vietsuky.com/lost/wallet_inside.jpg', FALSE),
(2, 'https://cdn.vietsuky.com/found/airpods_case.jpg', TRUE),
(3, 'https://cdn.vietsuky.com/lost/student_card_front.jpg', TRUE),
(4, 'https://cdn.vietsuky.com/found/jacket_back.jpg', TRUE);

-- ------------------------------------------------
-- HISTORIES (xác nhận trao đồ qua QR)
-- ------------------------------------------------
INSERT INTO histories (item_id, giver_id, receiver_id, qr_token, confirmed_at)
VALUES
(3, 2, 3, SHA2('token123',256), NOW()),
(1, 4, 1, SHA2('token456',256), NULL);

-- ------------------------------------------------
-- KARMA LOGS
-- ------------------------------------------------
INSERT INTO karma_logs (user_id, change_value, reason)
VALUES
(2, +20, 'Xác nhận trao đồ qua QR'),
(4, +50, 'Giúp người khác tìm đồ'),
(1, +10, 'Đăng bài hợp lệ'),
(3, -5, 'Báo sai vị trí đồ thất lạc');

-- ------------------------------------------------
-- NOTIFICATIONS
-- ------------------------------------------------
INSERT INTO notifications (user_id, title, body, is_read)
VALUES
(1, 'Cảm ơn bạn đã đăng bài', 'Bài viết “Ví sinh viên màu đen” của bạn đã được duyệt.', FALSE),
(2, 'Bạn đã nhận được 20 Karma!', 'Cảm ơn bạn đã giúp trao trả đồ thành công.', FALSE),
(3, 'Cập nhật trạng thái', 'Thẻ sinh viên của bạn đã được tìm thấy và xác nhận trao trả.', TRUE),
(4, 'Xin chúc mừng!', 'Bạn hiện là Helper của cộng đồng Lost&Found FPT Campus+', FALSE);
