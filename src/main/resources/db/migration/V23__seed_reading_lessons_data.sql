-- Migration: V23__seed_reading_lessons_data.sql
-- Description: Seed reading lessons and vocabulary hints for Part 7

-- Table: reading_lessons (6 rows)
INSERT INTO reading_lessons (id, question_group_id, title, reading_type, status, difficulty, created_at, updated_at, title_vi)
VALUES
  (1, 192, 'p07-q147-148', 'SINGLE', 'PUBLISHED', NULL, '2026-11-06T16:09:53.767032', '2026-11-06T16:12:35.508784', 'Tập trung vào sự hiện diện trên mạng xã hội của bạn'),
  (2, 193, 'p07-q149-150', 'SINGLE', 'PUBLISHED', NULL, '2026-11-06T16:13:20.317335', '2026-11-06T16:13:43.210134', 'Sự kiện Dine Out Darville đã trở lại!'),
  (3, 194, 'p07-q151-152', 'SINGLE', 'PUBLISHED', NULL, '2026-11-06T16:13:51.457839', '2026-11-06T16:16:24.645596', 'Rainsy chuyển trụ sở chính'),
  (4, 202, 'Khảo sát thử nếm và kết quả', 'DOUBLE', 'DRAFT', NULL, '2026-11-06T16:14:08.475174', '2026-11-06T16:14:49.601203', 'Khảo sát thử nếm và kết quả'),
  (5, 205, 'p07-q191-195(1)', 'TRIPLE', 'PUBLISHED', NULL, '2026-11-06T16:14:54.54216', '2026-11-06T16:16:06.2836', 'Crawford và Duval khai trương các cửa hàng truyền thống'),
  (6, 206, 'p07-q196-200(1)', 'TRIPLE', 'PUBLISHED', NULL, '2026-11-06T16:16:39.988993', '2026-11-06T16:18:10.202413', 'Luyện đọc TOEIC Part 7: Osawa Corporate Team Building')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('reading_lessons', 'id'), COALESCE((SELECT MAX(id) FROM reading_lessons), 1), (SELECT MAX(id) IS NOT NULL FROM reading_lessons));

-- Table: reading_vocabulary_hints (46 rows)
INSERT INTO reading_vocabulary_hints (id, reading_lesson_id, passage_id, word, meaning_vi, order_index, created_at, part_of_speech)
VALUES
  (1, 1, 69, 'stand out', 'nổi bật', 0, '2026-11-06T16:12:28.823893', 'phrasal verb'),
  (2, 1, 69, 'competitive', 'có tính cạnh tranh', 1, '2026-11-06T16:12:28.831654', 'adjective'),
  (3, 1, 69, 'target market', 'thị trường mục tiêu', 2, '2026-11-06T16:12:28.833815', 'noun phrase'),
  (4, 1, 69, 'promote', 'quảng bá', 3, '2026-11-06T16:12:28.833815', 'verb'),
  (5, 1, 69, 'entrepreneur', 'doanh nhân', 4, '2026-11-06T16:12:28.834852', 'noun'),
  (6, 1, 69, 'industry trends', 'xu hướng ngành', 5, '2026-11-06T16:12:28.835905', 'noun phrase'),
  (7, 1, 69, 'webinar', 'hội thảo trực tuyến', 6, '2026-11-06T16:12:28.835905', 'noun'),
  (8, 1, 69, 'sign up', 'đăng ký', 7, '2026-11-06T16:12:28.836815', 'phrasal verb'),
  (9, 2, 70, 'revisit', 'ghé thăm lại', 0, '2026-11-06T16:13:40.955792', 'verb'),
  (10, 2, 70, 'weeklong', 'kéo dài một tuần', 1, '2026-11-06T16:13:40.956803', 'adjective'),
  (11, 2, 70, 'four-course dinner', 'bữa tối bốn món', 2, '2026-11-06T16:13:40.957803', 'noun phrase'),
  (12, 2, 70, 'reduced price', 'giá ưu đãi, giá giảm', 3, '2026-11-06T16:13:40.959007', 'noun phrase'),
  (13, 2, 70, 'reservations', 'việc đặt chỗ trước', 4, '2026-11-06T16:13:40.960019', 'noun'),
  (14, 2, 70, 'participating', 'tham gia', 5, '2026-11-06T16:13:40.960019', 'adjective'),
  (24, 4, 92, 'participant', 'người tham gia', 0, '2026-11-06T16:14:49.614668', 'noun'),
  (25, 4, 92, 'texture', 'kết cấu', 1, '2026-11-06T16:14:49.61575', 'noun'),
  (26, 4, 92, 'potential', 'tiềm năng', 2, '2026-11-06T16:14:49.616745', 'noun'),
  (27, 4, 93, 'disappointing', 'đáng thất vọng', 3, '2026-11-06T16:14:49.616745', 'adjective'),
  (28, 4, 93, 'consistent', 'nhất quán', 4, '2026-11-06T16:14:49.616745', 'adjective'),
  (29, 4, 93, 'target market', 'thị trường mục tiêu', 5, '2026-11-06T16:14:49.618287', 'phrase'),
  (30, 4, 93, 'adjust', 'điều chỉnh', 6, '2026-11-06T16:14:49.618287', 'verb'),
  (31, 4, 93, 'proceed', 'tiến hành', 7, '2026-11-06T16:14:49.619294', 'verb'),
  (32, 4, 93, 'no later than', 'chậm nhất là', 8, '2026-11-06T16:14:49.620298', 'phrase'),
  (33, 5, 86, 'brick-and-mortar', 'truyền thống (cửa hàng có địa điểm vật lý)', 0, '2026-11-06T16:15:49.02516', 'adjective'),
  (34, 5, 86, 'handcrafted', 'được làm thủ công', 1, '2026-11-06T16:15:49.026377', 'adjective'),
  (35, 5, 86, 'merchandise', 'hàng hóa', 2, '2026-11-06T16:15:49.027303', 'noun'),
  (36, 5, 87, 'loyal', 'trung thành', 3, '2026-11-06T16:15:49.028303', 'adjective'),
  (37, 5, 87, 'consult', 'tham khảo ý kiến, tư vấn', 4, '2026-11-06T16:15:49.029342', 'verb'),
  (38, 5, 87, 'convenient', 'thuận tiện', 5, '2026-11-06T16:15:49.029342', 'adjective'),
  (39, 5, 88, 'receipt', 'biên lai', 6, '2026-11-06T16:15:49.030301', 'noun'),
  (40, 5, 88, 'sub total', 'tổng phụ', 7, '2026-11-06T16:15:49.030301', 'noun'),
  (41, 3, 71, 'headquarters', 'trụ sở chính', 0, '2026-11-06T16:16:23.681231', 'noun'),
  (42, 3, 71, 'data storage', 'lưu trữ dữ liệu', 1, '2026-11-06T16:16:23.682226', 'noun phrase'),
  (43, 3, 71, 'base of operations', 'cơ sở hoạt động', 2, '2026-11-06T16:16:23.682226', 'phrase'),
  (44, 3, 71, 'executive team', 'đội ngũ điều hành', 3, '2026-11-06T16:16:23.683151', 'noun phrase'),
  (45, 3, 71, 'relocate', 'di chuyển, chuyển địa điểm', 4, '2026-11-06T16:16:23.683151', 'verb'),
  (46, 3, 71, 'workforce', 'lực lượng lao động, nhân lực', 5, '2026-11-06T16:16:23.683151', 'noun'),
  (47, 6, 89, 'promote', 'thúc đẩy', 0, '2026-11-06T16:18:07.171979', 'verb'),
  (48, 6, 89, 'engagement', 'sự gắn kết', 1, '2026-11-06T16:18:07.172977', 'noun'),
  (49, 6, 89, 'predetermined', 'được định trước', 2, '2026-11-06T16:18:07.172977', 'adjective'),
  (50, 6, 90, 'participants', 'người tham gia', 3, '2026-11-06T16:18:07.172977', 'noun'),
  (51, 6, 90, 'business days', 'ngày làm việc', 4, '2026-11-06T16:18:07.174247', 'phrase'),
  (52, 6, 90, 'quote', 'báo giá', 5, '2026-11-06T16:18:07.174247', 'noun'),
  (53, 6, 91, 'facilitator', 'người điều phối', 6, '2026-11-06T16:18:07.174247', 'noun'),
  (54, 6, 91, 'feedback', 'phản hồi', 7, '2026-11-06T16:18:07.175374', 'noun'),
  (55, 6, 91, 'downside', 'điểm trừ, nhược điểm', 8, '2026-11-06T16:18:07.175374', 'noun')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('reading_vocabulary_hints', 'id'), COALESCE((SELECT MAX(id) FROM reading_vocabulary_hints), 1), (SELECT MAX(id) IS NOT NULL FROM reading_vocabulary_hints));

