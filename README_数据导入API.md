# Pokemon Factory 统一数据导入API文档

## 概述

统一数据导入API提供了完整的数据导入功能，整合了宝可梦、技能、物品等所有数据的导入操作。支持从PokeAPI获取数据、批量导入、文件导入等多种方式。

## 基础信息

- **基础URL**: `http://localhost:8080/api/import`
- **跨域支持**: 已启用，允许所有来源访问
- **Content-Type**: 支持 `application/json` 和 `multipart/form-data`

## API接口列表

### 1. 统一导入所有数据

**接口**: `POST /api/import/all`

**描述**: 从PokeAPI获取并导入所有宝可梦相关数据

**响应示例**:
```json
{
  "code": 200,
  "message": "导入完成",
  "data": {
    "pokemonCount": 151,
    "moveCount": 625,
    "itemCount": 1120,
    "abilityCount": 247,
    "typeCount": 18,
    "success": true
  }
}
```

### 2. 导入宝可梦数据

**接口**: `POST /api/import/pokemon`

**参数**:
- `startId` (可选): 起始ID，默认为1
- `count` (可选): 导入数量，默认为151

**示例**:
```bash
# 导入前50个宝可梦
curl -X POST "http://localhost:8080/api/import/pokemon?startId=1&count=50"

# 导入第152-301个宝可梦
curl -X POST "http://localhost:8080/api/import/pokemon?startId=152&count=150"
```

### 3. 导入技能数据

**接口**: `POST /api/import/moves`

**描述**: 从PokeAPI获取并导入技能数据

**响应示例**:
```json
{
  "code": 200,
  "message": "技能数据导入完成",
  "data": {
    "successCount": 625,
    "failCount": 0,
    "totalCount": 625
  }
}
```

### 4. 导入物品数据

**接口**: `POST /api/import/items`

**描述**: 从PokeAPI获取并导入物品数据

**响应示例**:
```json
{
  "code": 200,
  "message": "物品数据导入完成",
  "data": {
    "successCount": 1120,
    "failCount": 0,
    "totalCount": 1120
  }
}
```

### 5. 批量导入宝可梦数据

**接口**: `POST /api/import/pokemon/batch`

**Content-Type**: `application/json`

**请求体示例**:
```json
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
  }
]
```

**响应示例**:
```json
{
  "code": 200,
  "message": "批量导入完成",
  "data": {
    "total": 1,
    "success": 1,
    "fail": 0
  }
}
```

### 6. 批量导入物品数据

**接口**: `POST /api/import/items/batch`

**Content-Type**: `application/json`

**请求体示例**:
```json
[
  {
    "name": "测试精灵球",
    "nameEn": "Test Poké Ball",
    "nameJp": "テストモンスターボール",
    "category": "其他",
    "price": 100,
    "effect": "用于测试捕捉宝可梦",
    "description": "用于测试的数据"
  }
]
```

### 7. 清空所有数据

**接口**: `DELETE /api/import/all`

**描述**: 清空所有表中的数据（谨慎使用）

**响应示例**:
```json
{
  "code": 200,
  "message": "数据清空完成"
}
```

### 8. 清空指定表数据

**接口**: `DELETE /api/import/table/{tableName}`

**路径参数**:
- `tableName`: 表名（pokemon, item, move, ability, type）

**示例**:
```bash
# 清空宝可梦表
curl -X DELETE "http://localhost:8080/api/import/table/pokemon"

# 清空物品表
curl -X DELETE "http://localhost:8080/api/import/table/item"
```

### 9. 获取导入状态

**接口**: `GET /api/import/status`

**描述**: 获取当前数据库中的数据统计信息

**响应示例**:
```json
{
  "code": 200,
  "message": "状态获取成功",
  "data": {
    "pokemonCount": 151,
    "itemCount": 1120
  }
}
```

### 10. 从文件导入数据

**接口**: `POST /api/import/from-file`

**Content-Type**: `multipart/form-data`

**参数**:
- `file`: 文件（JSON格式）
- `type`: 数据类型（pokemon, move, item）

**示例**:
```bash
# 从JSON文件导入宝可梦数据
curl -X POST "http://localhost:8080/api/import/from-file" \
  -F "file=@pokemon_data.json" \
  -F "type=pokemon"
```

## 使用示例

### 1. 完整数据导入流程

```bash
# 1. 清空现有数据
curl -X DELETE "http://localhost:8080/api/import/all"

# 2. 导入所有数据
curl -X POST "http://localhost:8080/api/import/all"

# 3. 检查导入状态
curl -X GET "http://localhost:8080/api/import/status"
```

### 2. 分步导入数据

```bash
# 1. 先导入基础数据（类型、技能、物品）
curl -X POST "http://localhost:8080/api/import/moves"
curl -X POST "http://localhost:8080/api/import/items"

# 2. 再导入宝可梦数据
curl -X POST "http://localhost:8080/api/import/pokemon?startId=1&count=151"
```

### 3. 批量导入测试数据

```bash
# 创建测试数据文件
cat > test_data.json << EOF
[
  {
    "name": "测试宝可梦",
    "nameEn": "Test Pokemon",
    "nameJp": "テストポケモン",
    "height": 10,
    "weight": 100,
    "baseExperience": 100,
    "order": 99999,
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

# 执行批量导入
curl -X POST "http://localhost:8080/api/import/pokemon/batch" \
  -H "Content-Type: application/json" \
  -d @test_data.json

# 清理测试文件
rm test_data.json
```

### 4. 脚本化导入

使用提供的测试脚本：

```bash
# Bash脚本测试
chmod +x import_api_examples.sh
./import_api_examples.sh

# Node.js脚本测试
node import_api_examples.js
```

## 错误处理

所有接口都遵循统一的错误响应格式：

```json
{
  "code": 500,
  "message": "错误描述信息"
}
```

常见错误码：
- `200`: 成功
- `400`: 请求参数错误
- `404`: 资源不存在
- `500`: 服务器内部错误

## 注意事项

1. **数据清空风险**: `DELETE /api/import/all` 和 `DELETE /api/import/table/{tableName}` 操作不可逆，请谨慎使用
2. **网络依赖**: 从PokeAPI导入数据需要网络连接
3. **数据验证**: 批量导入时会进行基本的数据验证，无效数据会被跳过
4. **事务安全**: 所有导入操作都在事务中执行，确保数据一致性
5. **性能考虑**: 大量数据导入时建议分批进行，避免长时间占用数据库连接

## 测试文件

项目中提供了以下测试文件：
- `import_api_examples.sh`: Bash脚本测试
- `import_api_examples.js`: Node.js脚本测试
- `test_item_import.json`: 测试数据示例

## 扩展建议

1. **进度监控**: 可以添加导入进度实时监控功能
2. **数据验证**: 增加更严格的数据格式验证
3. **导入历史**: 记录导入操作的历史记录
4. **批量操作**: 支持多个表的批量导入
5. **数据导出**: 添加数据导出功能用于备份