#!/bin/bash

# 统一数据导入API测试脚本
# 使用curl测试DataImportController的所有接口

BASE_URL="http://localhost:8080/api/import"
AUTH_HEADER="-H \"Authorization: Bearer your-token-here\""

echo "=========================================="
echo "Pokemon Factory 数据导入API测试"
echo "=========================================="

# 1. 获取导入状态
echo -e "\n1. 获取当前数据状态:"
curl -X GET "$BASE_URL/status"

# 2. 清空所有数据
echo -e "\n\n2. 清空所有数据:"
curl -X DELETE "$BASE_URL/all"

# 3. 导入所有数据（从PokeAPI）
echo -e "\n\n3. 导入所有数据（从PokeAPI）:"
curl -X POST "$BASE_URL/all"

# 4. 导入宝可梦数据（指定范围）
echo -e "\n\n4. 导入宝可梦数据（范围1-50）:"
curl -X POST "$BASE_URL/pokemon?startId=1&count=50"

# 5. 导入技能数据
echo -e "\n\n5. 导入技能数据:"
curl -X POST "$BASE_URL/moves"

# 6. 导入物品数据
echo -e "\n\n6. 导入物品数据:"
curl -X POST "$BASE_URL/items"

# 7. 获取导入状态
echo -e "\n\n7. 获取导入状态:"
curl -X GET "$BASE_URL/status"

# 8. 清空指定表数据
echo -e "\n\n8. 清空宝可梦表数据:"
curl -X DELETE "$BASE_URL/table/pokemon"

# 9. 批量导入宝可梦数据
echo -e "\n\n9. 批量导入宝可梦数据:"
cat > pokemon_batch.json << 'EOF'
[
  {
    "name": "测试宝可梦1",
    "nameEn": "Test Pokemon 1",
    "nameJp": "テストポケモン1",
    "height": 10,
    "weight": 100,
    "baseExperience": 100,
    "order": 10001,
    "isDefault": true,
    "abilities": [],
    "forms": [],
    "gameIndices": [],
    "heldItems": [],
    "locationAreaEncounters": [],
    "moves": [],
    "species": {},
    "sprites": {},
    "stats": [],
    "types": []
  },
  {
    "name": "测试宝可梦2",
    "nameEn": "Test Pokemon 2",
    "nameJp": "テストポケモン2",
    "height": 15,
    "weight": 150,
    "baseExperience": 150,
    "order": 10002,
    "isDefault": true,
    "abilities": [],
    "forms": [],
    "gameIndices": [],
    "heldItems": [],
    "locationAreaEncounters": [],
    "moves": [],
    "species": {},
    "sprites": {},
    "stats": [],
    "types": []
  }
]
EOF

curl -X POST "$BASE_URL/pokemon/batch" \
  -H "Content-Type: application/json" \
  -d @pokemon_batch.json

# 10. 批量导入物品数据
echo -e "\n\n10. 批量导入物品数据:"
cat > items_batch.json << 'EOF'
[
  {
    "name": "测试精灵球",
    "nameEn": "Test Poké Ball",
    "nameJp": "テストモンスターボール",
    "category": "其他",
    "price": 100,
    "effect": "用于测试捕捉宝可梦",
    "description": "用于测试的数据"
  },
  {
    "name": "测试高级球",
    "nameEn": "Test Great Ball",
    "nameJp": "テストハイパーボール",
    "category": "其他",
    "price": 300,
    "effect": "测试用的高级捕捉球",
    "description": "用于测试的高级捕捉球"
  }
]
EOF

curl -X POST "$BASE_URL/items/batch" \
  -H "Content-Type: application/json" \
  -d @items_batch.json

# 11. 从文件导入数据
echo -e "\n\n11. 从文件导入宝可梦数据:"
curl -X POST "$BASE_URL/from-file" \
  -F "file=@pokemon_batch.json" \
  -F "type=pokemon"

echo -e "\n\n=========================================="
echo "测试完成！"
echo "=========================================="

# 清理临时文件
rm -f pokemon_batch.json items_batch.json