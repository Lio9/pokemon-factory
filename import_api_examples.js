/**
 * 统一数据导入API测试脚本
 * 使用JavaScript/Node.js测试DataImportController的所有接口
 */

const BASE_URL = 'http://localhost:8080/api/import';
const fs = require('fs');

async function testApi() {
    console.log('==========================================');
    console.log('Pokemon Factory 数据导入API测试');
    console.log('==========================================');

    try {
        // 1. 获取导入状态
        console.log('\n1. 获取当前数据状态:');
        let response = await fetch(`${BASE_URL}/status`);
        let data = await response.json();
        console.log('状态:', JSON.stringify(data, null, 2));

        // 2. 清空所有数据
        console.log('\n\n2. 清空所有数据:');
        response = await fetch(`${BASE_URL}/all`, { method: 'DELETE' });
        data = await response.json();
        console.log('清空结果:', JSON.stringify(data, null, 2));

        // 3. 导入所有数据（从PokeAPI）
        console.log('\n\n3. 导入所有数据（从PokeAPI）:');
        response = await fetch(`${BASE_URL}/all`, { method: 'POST' });
        data = await response.json();
        console.log('导入结果:', JSON.stringify(data, null, 2));

        // 4. 导入宝可梦数据（指定范围）
        console.log('\n\n4. 导入宝可梦数据（范围1-50）:');
        response = await fetch(`${BASE_URL}/pokemon?startId=1&count=50`, { method: 'POST' });
        data = await response.json();
        console.log('导入结果:', JSON.stringify(data, null, 2));

        // 5. 导入技能数据
        console.log('\n\n5. 导入技能数据:');
        response = await fetch(`${BASE_URL}/moves`, { method: 'POST' });
        data = await response.json();
        console.log('导入结果:', JSON.stringify(data, null, 2));

        // 6. 导入物品数据
        console.log('\n\n6. 导入物品数据:');
        response = await fetch(`${BASE_URL}/items`, { method: 'POST' });
        data = await response.json();
        console.log('导入结果:', JSON.stringify(data, null, 2));

        // 7. 获取导入状态
        console.log('\n\n7. 获取导入状态:');
        response = await fetch(`${BASE_URL}/status`);
        data = await response.json();
        console.log('状态:', JSON.stringify(data, null, 2));

        // 8. 清空指定表数据
        console.log('\n\n8. 清空宝可梦表数据:');
        response = await fetch(`${BASE_URL}/table/pokemon`, { method: 'DELETE' });
        data = await response.json();
        console.log('清空结果:', JSON.stringify(data, null, 2));

        // 9. 批量导入宝可梦数据
        console.log('\n\n9. 批量导入宝可梦数据:');
        const pokemonBatch = [
            {
                name: "测试宝可梦1",
                nameEn: "Test Pokemon 1",
                nameJp: "テストポケモン1",
                height: 10,
                weight: 100,
                baseExperience: 100,
                order: 10001,
                isDefault: true,
                abilities: [],
                forms: [],
                gameIndices: [],
                heldItems: [],
                locationAreaEncounters: [],
                moves: [],
                species: {},
                sprites: {},
                stats: [],
                types: []
            },
            {
                name: "测试宝可梦2",
                nameEn: "Test Pokemon 2",
                nameJp: "テストポケモン2",
                height: 15,
                weight: 150,
                baseExperience: 150,
                order: 10002,
                isDefault: true,
                abilities: [],
                forms: [],
                gameIndices: [],
                heldItems: [],
                locationAreaEncounters: [],
                moves: [],
                species: {},
                sprites: {},
                stats: [],
                types: []
            }
        ];

        response = await fetch(`${BASE_URL}/pokemon/batch`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(pokemonBatch)
        });
        data = await response.json();
        console.log('批量导入结果:', JSON.stringify(data, null, 2));

        // 10. 批量导入物品数据
        console.log('\n\n10. 批量导入物品数据:');
        const itemsBatch = [
            {
                name: "测试精灵球",
                nameEn: "Test Poké Ball",
                nameJp: "テストモンスターボール",
                category: "其他",
                price: 100,
                effect: "用于测试捕捉宝可梦",
                description: "用于测试的数据"
            },
            {
                name: "测试高级球",
                nameEn: "Test Great Ball",
                nameJp: "テストハイパーボール",
                category: "其他",
                price: 300,
                effect: "测试用的高级捕捉球",
                description: "用于测试的高级捕捉球"
            }
        ];

        response = await fetch(`${BASE_URL}/items/batch`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(itemsBatch)
        });
        data = await response.json();
        console.log('批量导入结果:', JSON.stringify(data, null, 2));

        // 11. 从文件导入数据
        console.log('\n\n11. 从文件导入宝可梦数据:');
        // 创建临时文件
        fs.writeFileSync('pokemon_batch.json', JSON.stringify(pokemonBatch, null, 2));
        
        const FormData = require('form-data');
        const form = new FormData();
        form.append('file', fs.createReadStream('pokemon_batch.json'));
        form.append('type', 'pokemon');

        response = await fetch(`${BASE_URL}/from-file`, {
            method: 'POST',
            body: form
        });
        data = await response.json();
        console.log('文件导入结果:', JSON.stringify(data, null, 2));

        console.log('\n\n==========================================');
        console.log('测试完成！');
        console.log('==========================================');

        // 清理临时文件
        fs.unlinkSync('pokemon_batch.json');

    } catch (error) {
        console.error('测试失败:', error);
    }
}

// 运行测试
testApi();
