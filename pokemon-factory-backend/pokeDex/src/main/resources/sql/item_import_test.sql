-- 测试数据导入
-- 清空现有数据
DELETE FROM item;

-- 插入测试数据
INSERT INTO item (`name`, `name_en`, `name_jp`, `category`, `price`, `effect`, `description`) VALUES
('测试精灵球', 'Test Poké Ball', 'テストモンスターボール', '其他', 100, '用于测试捕捉宝可梦', '用于测试的数据'),
('测试高级球', 'Test Great Ball', 'テストハイパーボール', '其他', 300, '测试用的高级捕捉球', '用于测试的高级捕捉球'),
('测试药水', 'Test Potion', 'テストポーション', '药水', 150, '测试回复宝可梦ＨＰ', '用于测试的回复药水'),
('测试全复球', 'Test Full Restore', 'テスト全回復薬', '药水', 1500, '测试回复宝可梦所有ＨＰ和状态', '用于测试的全恢复药水'),
('测试宝石', 'Test Gem', 'テストジュエル', '宝石', 250, '测试增加宝可梦的属性威力', '用于测试的属性宝石');

-- 验证导入结果
SELECT COUNT(*) as total_items FROM item;
SELECT * FROM item ORDER BY id;