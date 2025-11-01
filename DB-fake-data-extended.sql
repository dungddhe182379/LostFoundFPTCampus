-- ================================================
--  LOST & FOUND FPT CAMPUS - EXTENDED FAKE DATA
--  Author: GitHub Copilot (for testing new features)
--  Date: November 2, 2025
--  Purpose: Test data for DetailItem, Map, QR, Leaderboard
-- ================================================

USE lostfound_fptcampus;

-- ------------------------------------------------
-- CLEAR OLD TEST DATA (Optional - comment out if you want to keep existing data)
-- ------------------------------------------------
-- DELETE FROM notifications WHERE id > 0;
-- DELETE FROM karma_logs WHERE id > 0;
-- DELETE FROM histories WHERE id > 0;
-- DELETE FROM photos WHERE id > 0;
-- DELETE FROM items WHERE id > 0;
-- DELETE FROM user_roles WHERE user_id > 0;
-- DELETE FROM users WHERE id > 0;
-- ALTER TABLE users AUTO_INCREMENT = 1;
-- ALTER TABLE items AUTO_INCREMENT = 1;

-- ------------------------------------------------
-- USERS (50 users for Leaderboard testing)
-- ------------------------------------------------
INSERT INTO users (uuid, name, email, password_hash, phone, avatar_url, karma, created_at) VALUES
(UUID(), 'Nguyễn Văn An', 'an.nv@fpt.edu.vn', SHA2('123456',256), '0905123001', 'https://i.pravatar.cc/150?img=1', 520, DATE_SUB(NOW(), INTERVAL 90 DAY)),
(UUID(), 'Trần Thị Bình', 'binh.tt@fpt.edu.vn', SHA2('123456',256), '0905123002', 'https://i.pravatar.cc/150?img=2', 480, DATE_SUB(NOW(), INTERVAL 85 DAY)),
(UUID(), 'Phạm Minh Cường', 'cuong.pm@fpt.edu.vn', SHA2('123456',256), '0905123003', 'https://i.pravatar.cc/150?img=3', 450, DATE_SUB(NOW(), INTERVAL 80 DAY)),
(UUID(), 'Lê Hồng Dung', 'dung.lh@fpt.edu.vn', SHA2('123456',256), '0905123004', 'https://i.pravatar.cc/150?img=4', 420, DATE_SUB(NOW(), INTERVAL 75 DAY)),
(UUID(), 'Hoàng Văn Em', 'em.hv@fpt.edu.vn', SHA2('123456',256), '0905123005', 'https://i.pravatar.cc/150?img=5', 390, DATE_SUB(NOW(), INTERVAL 70 DAY)),
(UUID(), 'Vũ Thị Phương', 'phuong.vt@fpt.edu.vn', SHA2('123456',256), '0905123006', 'https://i.pravatar.cc/150?img=6', 360, DATE_SUB(NOW(), INTERVAL 65 DAY)),
(UUID(), 'Đặng Quốc Gia', 'gia.dq@fpt.edu.vn', SHA2('123456',256), '0905123007', 'https://i.pravatar.cc/150?img=7', 340, DATE_SUB(NOW(), INTERVAL 60 DAY)),
(UUID(), 'Bùi Thị Hà', 'ha.bt@fpt.edu.vn', SHA2('123456',256), '0905123008', 'https://i.pravatar.cc/150?img=8', 320, DATE_SUB(NOW(), INTERVAL 55 DAY)),
(UUID(), 'Ngô Văn Hùng', 'hung.nv@fpt.edu.vn', SHA2('123456',256), '0905123009', 'https://i.pravatar.cc/150?img=9', 300, DATE_SUB(NOW(), INTERVAL 50 DAY)),
(UUID(), 'Lý Thị Kiều', 'kieu.lt@fpt.edu.vn', SHA2('123456',256), '0905123010', 'https://i.pravatar.cc/150?img=10', 280, DATE_SUB(NOW(), INTERVAL 45 DAY)),
(UUID(), 'Võ Văn Long', 'long.vv@fpt.edu.vn', SHA2('123456',256), '0905123011', 'https://i.pravatar.cc/150?img=11', 260, DATE_SUB(NOW(), INTERVAL 40 DAY)),
(UUID(), 'Trương Thị Mai', 'mai.tt@fpt.edu.vn', SHA2('123456',256), '0905123012', 'https://i.pravatar.cc/150?img=12', 240, DATE_SUB(NOW(), INTERVAL 35 DAY)),
(UUID(), 'Phan Văn Nam', 'nam.pv@fpt.edu.vn', SHA2('123456',256), '0905123013', 'https://i.pravatar.cc/150?img=13', 220, DATE_SUB(NOW(), INTERVAL 30 DAY)),
(UUID(), 'Đỗ Thị Oanh', 'oanh.dt@fpt.edu.vn', SHA2('123456',256), '0905123014', 'https://i.pravatar.cc/150?img=14', 200, DATE_SUB(NOW(), INTERVAL 28 DAY)),
(UUID(), 'Nguyễn Văn Phúc', 'phuc.nv@fpt.edu.vn', SHA2('123456',256), '0905123015', 'https://i.pravatar.cc/150?img=15', 180, DATE_SUB(NOW(), INTERVAL 25 DAY)),
(UUID(), 'Lê Thị Quỳnh', 'quynh.lt@fpt.edu.vn', SHA2('123456',256), '0905123016', 'https://i.pravatar.cc/150?img=16', 160, DATE_SUB(NOW(), INTERVAL 22 DAY)),
(UUID(), 'Trần Văn Sơn', 'son.tv@fpt.edu.vn', SHA2('123456',256), '0905123017', 'https://i.pravatar.cc/150?img=17', 150, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(UUID(), 'Hoàng Thị Tâm', 'tam.ht@fpt.edu.vn', SHA2('123456',256), '0905123018', 'https://i.pravatar.cc/150?img=18', 140, DATE_SUB(NOW(), INTERVAL 18 DAY)),
(UUID(), 'Vũ Văn Tùng', 'tung.vv@fpt.edu.vn', SHA2('123456',256), '0905123019', 'https://i.pravatar.cc/150?img=19', 130, DATE_SUB(NOW(), INTERVAL 15 DAY)),
(UUID(), 'Đặng Thị Uyên', 'uyen.dt@fpt.edu.vn', SHA2('123456',256), '0905123020', 'https://i.pravatar.cc/150?img=20', 120, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(UUID(), 'Bùi Văn Vinh', 'vinh.bv@fpt.edu.vn', SHA2('123456',256), '0905123021', 'https://i.pravatar.cc/150?img=21', 110, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(UUID(), 'Ngô Thị Xuân', 'xuan.nt@fpt.edu.vn', SHA2('123456',256), '0905123022', 'https://i.pravatar.cc/150?img=22', 100, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(UUID(), 'Lý Văn Yên', 'yen.lv@fpt.edu.vn', SHA2('123456',256), '0905123023', 'https://i.pravatar.cc/150?img=23', 95, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(UUID(), 'Võ Thị Ánh', 'anh.vt@fpt.edu.vn', SHA2('123456',256), '0905123024', 'https://i.pravatar.cc/150?img=24', 90, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(UUID(), 'Trương Văn Bảo', 'bao.tv@fpt.edu.vn', SHA2('123456',256), '0905123025', 'https://i.pravatar.cc/150?img=25', 85, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(UUID(), 'Phan Thị Châu', 'chau.pt@fpt.edu.vn', SHA2('123456',256), '0905123026', 'https://i.pravatar.cc/150?img=26', 80, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(UUID(), 'Đỗ Văn Đạt', 'dat.dv@fpt.edu.vn', SHA2('123456',256), '0905123027', 'https://i.pravatar.cc/150?img=27', 75, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(UUID(), 'Nguyễn Thị Evy', 'evy.nt@fpt.edu.vn', SHA2('123456',256), '0905123028', 'https://i.pravatar.cc/150?img=28', 70, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(UUID(), 'Lê Văn Phát', 'phat.lv@fpt.edu.vn', SHA2('123456',256), '0905123029', 'https://i.pravatar.cc/150?img=29', 65, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(UUID(), 'Trần Thị Giang', 'giang.tt@fpt.edu.vn', SHA2('123456',256), '0905123030', 'https://i.pravatar.cc/150?img=30', 60, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(UUID(), 'Hoàng Văn Hiệp', 'hiep.hv@fpt.edu.vn', SHA2('123456',256), '0905123031', 'https://i.pravatar.cc/150?img=31', 55, NOW()),
(UUID(), 'Vũ Thị Ivy', 'ivy.vt@fpt.edu.vn', SHA2('123456',256), '0905123032', 'https://i.pravatar.cc/150?img=32', 50, NOW()),
(UUID(), 'Đặng Văn Khoa', 'khoa.dv@fpt.edu.vn', SHA2('123456',256), '0905123033', 'https://i.pravatar.cc/150?img=33', 45, NOW()),
(UUID(), 'Bùi Thị Linh', 'linh.bt@fpt.edu.vn', SHA2('123456',256), '0905123034', 'https://i.pravatar.cc/150?img=34', 40, NOW()),
(UUID(), 'Ngô Văn Minh', 'minh.nv@fpt.edu.vn', SHA2('123456',256), '0905123035', 'https://i.pravatar.cc/150?img=35', 35, NOW()),
(UUID(), 'Lý Thị Nga', 'nga.lt@fpt.edu.vn', SHA2('123456',256), '0905123036', 'https://i.pravatar.cc/150?img=36', 30, NOW()),
(UUID(), 'Võ Văn Oai', 'oai.vv@fpt.edu.vn', SHA2('123456',256), '0905123037', 'https://i.pravatar.cc/150?img=37', 25, NOW()),
(UUID(), 'Trương Thị Phượng', 'phuong.tt@fpt.edu.vn', SHA2('123456',256), '0905123038', 'https://i.pravatar.cc/150?img=38', 20, NOW()),
(UUID(), 'Phan Văn Quang', 'quang.pv@fpt.edu.vn', SHA2('123456',256), '0905123039', 'https://i.pravatar.cc/150?img=39', 15, NOW()),
(UUID(), 'Đỗ Thị Rạng', 'rang.dt@fpt.edu.vn', SHA2('123456',256), '0905123040', 'https://i.pravatar.cc/150?img=40', 10, NOW()),
(UUID(), 'Nguyễn Văn Sáng', 'sang.nv@fpt.edu.vn', SHA2('123456',256), '0905123041', 'https://i.pravatar.cc/150?img=41', 8, NOW()),
(UUID(), 'Lê Thị Tuyết', 'tuyet.lt@fpt.edu.vn', SHA2('123456',256), '0905123042', 'https://i.pravatar.cc/150?img=42', 6, NOW()),
(UUID(), 'Trần Văn Uy', 'uy.tv@fpt.edu.vn', SHA2('123456',256), '0905123043', 'https://i.pravatar.cc/150?img=43', 4, NOW()),
(UUID(), 'Hoàng Thị Vân', 'van.ht@fpt.edu.vn', SHA2('123456',256), '0905123044', 'https://i.pravatar.cc/150?img=44', 2, NOW()),
(UUID(), 'Vũ Văn Xô', 'xo.vv@fpt.edu.vn', SHA2('123456',256), '0905123045', 'https://i.pravatar.cc/150?img=45', 1, NOW()),
(UUID(), 'Đặng Thị Yến', 'yen.dt@fpt.edu.vn', SHA2('123456',256), '0905123046', 'https://i.pravatar.cc/150?img=46', 0, NOW()),
(UUID(), 'Bùi Văn Zung', 'zung.bv@fpt.edu.vn', SHA2('123456',256), '0905123047', 'https://i.pravatar.cc/150?img=47', 0, NOW()),
(UUID(), 'Ngô Thị Anh', 'anh.nt@fpt.edu.vn', SHA2('123456',256), '0905123048', 'https://i.pravatar.cc/150?img=48', 0, NOW()),
(UUID(), 'Lý Văn Bình', 'binh.lv@fpt.edu.vn', SHA2('123456',256), '0905123049', 'https://i.pravatar.cc/150?img=49', 0, NOW()),
(UUID(), 'Võ Thị Cúc', 'cuc.vt@fpt.edu.vn', SHA2('123456',256), '0905123050', 'https://i.pravatar.cc/150?img=50', 0, NOW());

-- ------------------------------------------------
-- USER_ROLES (assign students and some helpers)
-- ------------------------------------------------
INSERT INTO user_roles (user_id, role_id) 
SELECT id, 1 FROM users WHERE id BETWEEN 1 AND 50;

-- Promote top 10 users to helpers
INSERT INTO user_roles (user_id, role_id) 
SELECT id, 2 FROM users WHERE id BETWEEN 1 AND 10;

-- ------------------------------------------------
-- ITEMS (30+ items spread across FPT Campus map)
-- FPT University HCMC coordinates: ~10.762622, 106.682223
-- ------------------------------------------------
INSERT INTO items (uuid, user_id, title, description, category, status, latitude, longitude, image_url, created_at) VALUES
-- Lost items
(UUID(), 1, 'Ví da màu đen Samsung', 'Rơi tại căn tin khu A lúc 9h sáng. Bên trong có CMND và thẻ ATM.', 'wallet', 'lost', 10.762622, 106.682223, 'https://images.unsplash.com/photo-1627123424574-724758594e93?w=400', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(UUID(), 3, 'Laptop Dell XPS 15', 'Quên ở thư viện tầng 2, màu bạc có sticker React', 'laptop', 'lost', 10.762910, 106.682800, 'https://images.unsplash.com/photo-1593642632823-8f785ba67e45?w=400', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(UUID(), 5, 'Xe đạp màu xanh', 'Mất tại bãi gửi xe khu B. Biển số FPT001', 'bicycle', 'lost', 10.762300, 106.681900, 'https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=400', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(UUID(), 7, 'Chìa khóa xe máy Honda', 'Móc keychain hình gấu trúc. Rơi ở cổng chính.', 'key', 'lost', 10.763100, 106.682500, 'https://images.unsplash.com/photo-1582139329536-e7284fece509?w=400', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(UUID(), 9, 'Thẻ sinh viên FPT', 'Thẻ màu xanh, tên Ngô Văn Hùng, MSSV: SE161234', 'card', 'lost', 10.762800, 106.682400, 'https://images.unsplash.com/photo-1614680376593-902f74cf0d41?w=400', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(UUID(), 11, 'Kính cận gọng đen', 'Để quên trên bàn ở tầng 3 Alpha', 'glasses', 'lost', 10.762700, 106.682600, 'https://images.unsplash.com/photo-1574258495973-f010dfbb5371?w=400', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(UUID(), 13, 'Điện thoại iPhone 13 Pro', 'Vỏ màu xanh navy, có ốp lưng trong suốt', 'phone', 'lost', 10.762500, 106.682100, 'https://images.unsplash.com/photo-1592286927505-2fd0dc3d28d8?w=400', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(UUID(), 15, 'Áo khoác The North Face', 'Màu đen, size M, để trong tủ phòng lab B102', 'clothes', 'lost', 10.762400, 106.682000, 'https://images.unsplash.com/photo-1551028719-00167b16eac5?w=400', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(UUID(), 17, 'Sổ tay Moleskine', 'Sổ da màu nâu, có nhiều ghi chú quan trọng', 'book', 'lost', 10.762950, 106.682750, 'https://images.unsplash.com/photo-1517971129774-8a2b38fa128e?w=400', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(UUID(), 19, 'USB SanDisk 64GB', 'USB màu đỏ đen, có file đồ án cuối kỳ', 'usb', 'lost', 10.762600, 106.682300, 'https://images.unsplash.com/photo-1588636142470-a1c48888f2e3?w=400', DATE_SUB(NOW(), INTERVAL 1 HOUR)),

-- Found items
(UUID(), 2, 'Tai nghe AirPods Pro', 'Nhặt được gần thư viện hôm qua, còn trong hộp', 'earphone', 'found', 10.762910, 106.682800, 'https://images.unsplash.com/photo-1606841837239-c5a1a4a07af7?w=400', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(UUID(), 4, 'Balo The North Face', 'Nhặt ở sân bóng, bên trong có vài quyển sách', 'bag', 'found', 10.762100, 106.683000, 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=400', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(UUID(), 6, 'Ô dù màu xanh', 'Tìm thấy tại sảnh tòa nhà Alpha', 'umbrella', 'found', 10.762850, 106.682550, 'https://images.unsplash.com/photo-1534787238916-9ba6764efd4f?w=400', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(UUID(), 8, 'Bình nước Hydro Flask', 'Bình màu hồng, tìm thấy ở phòng gym', 'bottle', 'found', 10.762200, 106.682900, 'https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=400', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(UUID(), 10, 'Sạc dự phòng Anker 20000mAh', 'Nhặt được tại căn tin, còn đầy pin', 'charger', 'found', 10.762650, 106.682250, 'https://images.unsplash.com/photo-1609091839311-d5365f9ff1c5?w=400', DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(UUID(), 12, 'Mũ lưỡi trai Nike', 'Màu đen, logo trắng, tìm ở ghế đá ngoài sân', 'hat', 'found', 10.762750, 106.682700, 'https://images.unsplash.com/photo-1588850561407-ed78c282e89b?w=400', DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(UUID(), 14, 'Dây chuyền bạc', 'Nhặt được ở toilet tầng 2, có mặt dây chữ T', 'jewelry', 'found', 10.762900, 106.682650, 'https://images.unsplash.com/photo-1599643477877-530eb83abc8e?w=400', DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(UUID(), 16, 'Đồng hồ Casio G-Shock', 'Màu xanh dương, tìm ở phòng tập gym', 'watch', 'found', 10.762250, 106.682850, 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400', DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(UUID(), 18, 'Túi vải tote', 'Màu trắng có in logo FPT, nhặt ở cổng sau', 'bag', 'found', 10.763050, 106.682450, 'https://images.unsplash.com/photo-1590874103328-eac38a683ce7?w=400', DATE_SUB(NOW(), INTERVAL 9 HOUR)),
(UUID(), 20, 'Máy tính khoa học Casio', 'Loại FX-580VN PLUS, nhặt ở phòng thi', 'calculator', 'found', 10.762550, 106.682350, 'https://images.unsplash.com/photo-1587145820266-a5951ee6f620?w=400', DATE_SUB(NOW(), INTERVAL 3 HOUR)),

-- Returned items
(UUID(), 1, 'Thẻ ATM Vietcombank', 'Đã trao trả thành công qua QR code', 'card', 'returned', 10.762450, 106.682150, 'https://images.unsplash.com/photo-1563013544-824ae1b704d3?w=400', DATE_SUB(NOW(), INTERVAL 15 DAY)),
(UUID(), 2, 'Giày thể thao Nike Air', 'Đã trả cho chủ nhân, size 42', 'shoes', 'returned', 10.762350, 106.682050, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400', DATE_SUB(NOW(), INTERVAL 20 DAY)),
(UUID(), 3, 'Chuột gaming Logitech', 'Đã xác nhận trao đồ thành công', 'mouse', 'returned', 10.762800, 106.682500, 'https://images.unsplash.com/photo-1527814050087-3793815479db?w=400', DATE_SUB(NOW(), INTERVAL 12 DAY)),
(UUID(), 4, 'Khăn choàng len', 'Màu be, đã trả lại chủ nhân', 'scarf', 'returned', 10.762700, 106.682400, 'https://images.unsplash.com/photo-1520903920243-00d872a2d1c9?w=400', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(UUID(), 5, 'Móc khóa Doraemon', 'Đã trao trả qua mã QR', 'keychain', 'returned', 10.762600, 106.682300, 'https://images.unsplash.com/photo-1587212095751-8b5ca3b9b82e?w=400', DATE_SUB(NOW(), INTERVAL 25 DAY)),

-- More spread out items for better map display
(UUID(), 21, 'Áo mưa poncho', 'Màu vàng, nhặt ở bãi xe', 'clothes', 'found', 10.761900, 106.682200, 'https://images.unsplash.com/photo-1578916171728-46686eac8d58?w=400', NOW()),
(UUID(), 22, 'Thẻ thành viên Gym', 'Thẻ từ màu xanh', 'card', 'found', 10.763200, 106.682100, 'https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=400', NOW()),
(UUID(), 23, 'Bàn phím cơ Keychron', 'K2 RGB, switch blue', 'keyboard', 'lost', 10.762400, 106.683100, 'https://images.unsplash.com/photo-1595225476474-87563907a212?w=400', NOW()),
(UUID(), 24, 'Kẹp tóc ngọc trai', 'Màu trắng, nhặt ở toilet nữ', 'accessory', 'found', 10.763000, 106.682300, 'https://images.unsplash.com/photo-1535632066927-ab7c9ab60908?w=400', NOW()),
(UUID(), 25, 'Hộp cơm giữ nhiệt', 'Màu hồng, có hình gấu', 'lunchbox', 'found', 10.762300, 106.682700, 'https://images.unsplash.com/photo-1580274455191-1c62238fa333?w=400', NOW());

-- ------------------------------------------------
-- PHOTOS (multiple photos for items)
-- ------------------------------------------------
INSERT INTO photos (item_id, url, is_primary) VALUES
-- Item 1: Ví da
(1, 'https://images.unsplash.com/photo-1627123424574-724758594e93?w=800', TRUE),
(1, 'https://images.unsplash.com/photo-1606760227091-3dd870d97f1d?w=800', FALSE),

-- Item 2: Laptop Dell
(2, 'https://images.unsplash.com/photo-1593642632823-8f785ba67e45?w=800', TRUE),
(2, 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800', FALSE),

-- Item 11: AirPods
(11, 'https://images.unsplash.com/photo-1606841837239-c5a1a4a07af7?w=800', TRUE),
(11, 'https://images.unsplash.com/photo-1588423771073-b8903fbb85b5?w=800', FALSE),

-- Item 12: Balo
(12, 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800', TRUE),
(12, 'https://images.unsplash.com/photo-1622560480605-d83c853bc5c3?w=800', FALSE),

-- Item 7: iPhone
(7, 'https://images.unsplash.com/photo-1592286927505-2fd0dc3d28d8?w=800', TRUE),
(7, 'https://images.unsplash.com/photo-1611472173362-3f53dbd65d80?w=800', FALSE),
(7, 'https://images.unsplash.com/photo-1632633728024-e1fd4bef561a?w=800', FALSE);

-- ------------------------------------------------
-- HISTORIES (QR confirmations for returned items)
-- ------------------------------------------------
INSERT INTO histories (item_id, giver_id, receiver_id, qr_token, confirmed_at) VALUES
(21, 2, 1, SHA2(CONCAT('token_', UUID()), 256), DATE_SUB(NOW(), INTERVAL 15 DAY)),
(22, 3, 2, SHA2(CONCAT('token_', UUID()), 256), DATE_SUB(NOW(), INTERVAL 20 DAY)),
(23, 4, 3, SHA2(CONCAT('token_', UUID()), 256), DATE_SUB(NOW(), INTERVAL 12 DAY)),
(24, 5, 4, SHA2(CONCAT('token_', UUID()), 256), DATE_SUB(NOW(), INTERVAL 8 DAY)),
(25, 6, 5, SHA2(CONCAT('token_', UUID()), 256), DATE_SUB(NOW(), INTERVAL 25 DAY)),

-- Pending confirmations (confirmed_at is NULL)
(5, 7, 9, SHA2(CONCAT('pending_', UUID()), 256), NULL),
(10, 10, 12, SHA2(CONCAT('pending_', UUID()), 256), NULL);

-- ------------------------------------------------
-- KARMA LOGS (activity history for leaderboard)
-- ------------------------------------------------
INSERT INTO karma_logs (user_id, change_value, reason, created_at) VALUES
-- Top user activities
(1, +50, 'Trao trả thành công Thẻ ATM qua QR', DATE_SUB(NOW(), INTERVAL 15 DAY)),
(1, +30, 'Đăng bài Lost: Ví da', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, +40, 'Giúp 5 người tìm đồ thành công', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(1, +100, 'Top Helper của tháng', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1, +300, 'Đạt milestone 10 lần trao đồ', DATE_SUB(NOW(), INTERVAL 30 DAY)),

(2, +50, 'Trao trả Giày Nike qua QR', DATE_SUB(NOW(), INTERVAL 20 DAY)),
(2, +30, 'Đăng Found: Tai nghe AirPods', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, +100, 'Verified Helper badge', DATE_SUB(NOW(), INTERVAL 25 DAY)),
(2, +200, 'Đạt 15 lần giúp đỡ', DATE_SUB(NOW(), INTERVAL 35 DAY)),
(2, +100, 'Top 3 Helper tuần này', DATE_SUB(NOW(), INTERVAL 7 DAY)),

(3, +50, 'Trao trả Chuột gaming qua QR', DATE_SUB(NOW(), INTERVAL 12 DAY)),
(3, +30, 'Đăng Lost: Laptop Dell', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3, +70, 'Giúp 7 người', DATE_SUB(NOW(), INTERVAL 18 DAY)),
(3, +200, 'Đạt milestone 12 lần trao đồ', DATE_SUB(NOW(), INTERVAL 40 DAY)),
(3, +100, 'Super Helper badge', DATE_SUB(NOW(), INTERVAL 15 DAY)),

(4, +50, 'Trao trả Khăn choàng len qua QR', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(4, +30, 'Đăng Found: Balo North Face', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4, +140, 'Giúp 14 người tìm đồ', DATE_SUB(NOW(), INTERVAL 20 DAY)),
(4, +100, 'Gold Helper badge', DATE_SUB(NOW(), INTERVAL 12 DAY)),
(4, +100, 'Active 30 ngày liên tục', DATE_SUB(NOW(), INTERVAL 5 DAY)),

(5, +50, 'Trao trả Móc khóa qua QR', DATE_SUB(NOW(), INTERVAL 25 DAY)),
(5, +30, 'Đăng Lost: Xe đạp xanh', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, +110, 'Giúp 11 người', DATE_SUB(NOW(), INTERVAL 22 DAY)),
(5, +100, 'Helper of the week', DATE_SUB(NOW(), INTERVAL 14 DAY)),
(5, +100, 'Fast responder badge', DATE_SUB(NOW(), INTERVAL 8 DAY)),

-- More karma logs for variety
(6, +30, 'Đăng Found: Ô dù xanh', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(6, +80, 'Giúp 8 người', DATE_SUB(NOW(), INTERVAL 16 DAY)),
(6, +50, 'Active member badge', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(6, +200, 'Đạt 10 lần trao đồ', DATE_SUB(NOW(), INTERVAL 28 DAY)),

(7, +30, 'Đăng Lost: Chìa khóa xe', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(7, +60, 'Giúp 6 người', DATE_SUB(NOW(), INTERVAL 14 DAY)),
(7, +50, 'Quick helper badge', DATE_SUB(NOW(), INTERVAL 9 DAY)),
(7, +200, 'Top 5 this month', DATE_SUB(NOW(), INTERVAL 20 DAY)),

(8, +30, 'Đăng Found: Bình nước', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(8, +70, 'Giúp 7 người', DATE_SUB(NOW(), INTERVAL 12 DAY)),
(8, +50, 'Friendly helper', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(8, +170, 'Milestone 8 lần trao đồ', DATE_SUB(NOW(), INTERVAL 24 DAY)),

(9, +30, 'Đăng Lost: Thẻ sinh viên', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(9, +90, 'Giúp 9 người', DATE_SUB(NOW(), INTERVAL 15 DAY)),
(9, +80, 'Rising star badge', DATE_SUB(NOW(), INTERVAL 11 DAY)),
(9, +100, 'Community hero', DATE_SUB(NOW(), INTERVAL 19 DAY)),

(10, +30, 'Đăng Found: Sạc dự phòng', DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(10, +50, 'Giúp 5 người', DATE_SUB(NOW(), INTERVAL 13 DAY)),
(10, +100, 'Good finder badge', DATE_SUB(NOW(), INTERVAL 17 DAY)),
(10, +100, 'Trusted member', DATE_SUB(NOW(), INTERVAL 23 DAY)),

-- Add more karma for users 11-30
(11, +30, 'Đăng Lost: Kính cận', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(11, +60, 'Giúp 6 người', DATE_SUB(NOW(), INTERVAL 11 DAY)),
(11, +170, 'Active 20 days', DATE_SUB(NOW(), INTERVAL 21 DAY)),

(12, +30, 'Đăng Found: Mũ Nike', DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(12, +50, 'Giúp 5 người', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(12, +160, 'Milestone 7 lần trao đồ', DATE_SUB(NOW(), INTERVAL 22 DAY)),

(13, +30, 'Đăng Lost: iPhone 13', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(13, +40, 'Giúp 4 người', DATE_SUB(NOW(), INTERVAL 9 DAY)),
(13, +150, 'Good community member', DATE_SUB(NOW(), INTERVAL 19 DAY)),

(14, +30, 'Đăng Found: Dây chuyền', DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(14, +70, 'Giúp 7 người', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(14, +100, 'Helpful badge', DATE_SUB(NOW(), INTERVAL 18 DAY)),

(15, +30, 'Đăng Lost: Áo khoác', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(15, +50, 'Giúp 5 người', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(15, +100, 'Active member', DATE_SUB(NOW(), INTERVAL 17 DAY)),

(16, +30, 'Đăng Found: Đồng hồ G-Shock', DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(16, +60, 'Giúp 6 người', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(16, +70, 'Community supporter', DATE_SUB(NOW(), INTERVAL 16 DAY)),

(17, +30, 'Đăng Lost: Sổ tay', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(17, +50, 'Giúp 5 người', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(17, +70, 'Helpful member', DATE_SUB(NOW(), INTERVAL 15 DAY)),

(18, +30, 'Đăng Found: Túi tote', DATE_SUB(NOW(), INTERVAL 9 HOUR)),
(18, +40, 'Giúp 4 người', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(18, +70, 'Good finder', DATE_SUB(NOW(), INTERVAL 14 DAY)),

(19, +30, 'Đăng Lost: USB 64GB', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(19, +50, 'Giúp 5 người', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(19, +50, 'Active user', DATE_SUB(NOW(), INTERVAL 13 DAY)),

(20, +30, 'Đăng Found: Máy tính Casio', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(20, +40, 'Giúp 4 người', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(20, +50, 'Community member', DATE_SUB(NOW(), INTERVAL 12 DAY)),

-- Continue for users 21-30 with smaller karma
(21, +30, 'Đăng Found: Áo mưa', NOW()),
(21, +40, 'Giúp 4 người', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(21, +40, 'New helper', DATE_SUB(NOW(), INTERVAL 10 DAY)),

(22, +30, 'Đăng Found: Thẻ Gym', NOW()),
(22, +30, 'Giúp 3 người', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(22, +40, 'Active', DATE_SUB(NOW(), INTERVAL 9 DAY)),

(23, +30, 'Đăng Lost: Bàn phím cơ', NOW()),
(23, +30, 'Giúp 3 người', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(23, +35, 'New member', DATE_SUB(NOW(), INTERVAL 8 DAY)),

(24, +30, 'Đăng Found: Kẹp tóc', NOW()),
(24, +30, 'Giúp 3 người', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(24, +30, 'Helper badge', DATE_SUB(NOW(), INTERVAL 7 DAY)),

(25, +30, 'Đăng Found: Hộp cơm', NOW()),
(25, +30, 'Giúp 3 người', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(25, +25, 'Active', DATE_SUB(NOW(), INTERVAL 6 DAY));

-- ------------------------------------------------
-- NOTIFICATIONS (recent activities)
-- ------------------------------------------------
INSERT INTO notifications (user_id, title, body, is_read, created_at) VALUES
(1, 'Chúc mừng! 🎉', 'Bạn đã đạt 520 Karma và là TOP 1 Leaderboard!', FALSE, NOW()),
(1, 'Cảm ơn bạn đã giúp đỡ', 'Ví da của bạn đã có 5 người quan tâm', FALSE, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(2, 'Đạt milestone mới! 🏆', 'Bạn đã giúp trao trả thành công 15 món đồ', FALSE, NOW()),
(2, 'AirPods của bạn', 'Có người đang liên hệ về tai nghe bạn tìm được', TRUE, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(3, 'Xác nhận QR thành công', 'Chuột gaming đã được trao tay an toàn. +50 Karma!', FALSE, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(3, 'Laptop của bạn', 'Có 3 người đang theo dõi bài đăng của bạn', FALSE, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(4, 'Super Helper 🌟', 'Bạn được thăng hạng lên Super Helper!', FALSE, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5, 'Near you! 📍', 'Có 2 món đồ mới được tìm thấy gần vị trí của bạn', FALSE, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(6, 'Cảm ơn bạn', 'Ô dù bạn tìm được đã có người nhận', TRUE, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(7, 'Chìa khóa của bạn', 'Có người báo tìm thấy chìa khóa tương tự!', FALSE, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(8, 'Karma +30', 'Bạn được cộng 30 Karma cho bài đăng mới', TRUE, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(9, 'Thẻ sinh viên', 'Đã tìm thấy! Vui lòng liên hệ để nhận lại', FALSE, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(10, 'Top 10 this week! 📊', 'Bạn đang ở vị trí thứ 10 bảng xếp hạng tuần này', FALSE, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(11, 'Kính cận của bạn', 'Có người đang liên hệ qua chat', FALSE, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(12, 'Verified Helper ✓', 'Bạn đã trở thành Helper được xác minh', FALSE, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(13, 'iPhone 13 Pro', 'Có 8 người đang theo dõi bài đăng này', TRUE, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(14, 'Dây chuyền bạc', 'Chủ nhân đã liên hệ, chuẩn bị trao đồ qua QR', FALSE, DATE_SUB(NOW(), INTERVAL 7 HOUR)),
(15, 'Welcome bonus 🎁', 'Bạn nhận được 10 Karma chào mừng thành viên mới', TRUE, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(20, 'Máy tính Casio', 'Có người cần xác nhận đây có phải máy của họ không', FALSE, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(25, 'First post! 🚀', 'Bài đăng đầu tiên của bạn đã được duyệt', FALSE, NOW());

-- ------------------------------------------------
-- SUMMARY
-- ------------------------------------------------
SELECT 'Extended fake data inserted successfully!' AS status;
SELECT COUNT(*) AS total_users FROM users;
SELECT COUNT(*) AS total_items FROM items;
SELECT COUNT(*) AS total_photos FROM photos;
SELECT COUNT(*) AS total_histories FROM histories;
SELECT COUNT(*) AS total_karma_logs FROM karma_logs;
SELECT COUNT(*) AS total_notifications FROM notifications;

-- Top 10 Leaderboard preview
SELECT 
    u.id,
    u.name,
    u.email,
    u.karma,
    COUNT(DISTINCT i.id) AS items_posted,
    COUNT(DISTINCT h.id) AS items_returned
FROM users u
LEFT JOIN items i ON u.id = i.user_id
LEFT JOIN histories h ON u.id = h.giver_id AND h.confirmed_at IS NOT NULL
GROUP BY u.id, u.name, u.email, u.karma
ORDER BY u.karma DESC
LIMIT 10;

-- Items distribution by status
SELECT status, COUNT(*) AS count FROM items GROUP BY status;

-- Items distribution on map (for map testing)
SELECT 
    id,
    title,
    category,
    status,
    latitude,
    longitude,
    ROUND(SQRT(POW(latitude - 10.762622, 2) + POW(longitude - 106.682223, 2)) * 111.32, 2) AS distance_km
FROM items
ORDER BY distance_km;
