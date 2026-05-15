# Pokemon Factory 性能优化指南

> 版本: 2.0 | 更新时间: 2026-05-15

## 目录

- [前端优化](#前端优化)
  - [缓存系统优化](#缓存系统优化)
  - [API 请求优化](#api-请求优化)
  - [组件渲染优化](#组件渲染优化)
  - [图片资源优化](#图片资源优化)
- [后端优化](#后端优化)
  - [查询优化](#查询优化)
  - [缓存策略](#缓存策略)
- [性能监控](#性能监控)
- [部署优化](#部署优化)

---

## 前端优化

### 缓存系统优化

#### 核心缓存架构

项目已集成高级缓存系统，提供以下特性：

1. **多级缓存**：
   - 内存缓存（一级
   - IndexedDB 持久化缓存（可选）
   - 自动过期清理
   - LRU（Least Recently Used）淘汰策略

2. **请求去重**：
   - 同时请求合并避免重复请求
   - 批处理多个相同请求
   - 并发控制

3. **智能过期策略**：
   - 短期缓存：2 分钟（频繁变化数据）
   - 中期缓存：10 分钟（常规数据）
   - 长期缓存：30 分钟（静态数据）

#### 使用示例

```javascript
import dataCache from '@/services/cache'

// 简单缓存获取
const data = await dataCache.getOrFetch(
  'pokemon:detail',
  { id: 25 },
  () => fetchPokemonDetail(25),
  'long'
)

// 统计信息
console.log(dataCache.getStats())
// { hits: 150, misses: 50, hitRate: '75.00%'
```

#### 性能工具库

```javascript
import {
  debounce,
  throttle,
  BatchProcessor,
  VirtualScroller,
  LazyImageLoader,
  RequestController,
  perfMonitor
} from '@/utils/performance'
```

### API 请求优化

已优化以下 API 集成缓存系统：

| API 模块 | 缓存策略 | 过期时间 |
|---------|---------|----------|
| pokemonApi.getList | 普通缓存 | 10分钟 |
| pokemonApi.getDetail | 长期缓存 | 30分钟 |
| pokemonApi.getMoves | 长期缓存 | 30分钟 |
| typeApi.getAll | 长期缓存 | 30分钟 |
| abilityApi.getList | 普通缓存 | 10分钟 |
| moveApi.getList | 普通缓存 | 10分钟 |
| itemApi.getBattleItems | 长期缓存 | 30分钟 |
| damageApi.getTypeEfficacy | 长期缓存 | 30分钟 |

### 组件渲染优化

#### 1. 列表虚拟化

大型列表使用虚拟化滚动组件：

```javascript
import { VirtualScroller } from '@/utils/performance'

const scroller = new VirtualScroller({
  itemHeight: 60,
  bufferSize: 5,
  containerHeight: 500,
  totalItems: 1000
})

// 获取可见范围
const { start, end } = scroller.getVisibleRange()
```

#### 2. 图片懒加载

```javascript
import { LazyImageLoader } from '@/utils/performance'

const loader = new LazyImageLoader()
loader.observe(imgElement)
```

#### 3. 防抖/节流

```javascript
// 搜索防抖
import { debounce } from '@/utils/performance'

const searchHandler = debounce((keyword) => {
  // 搜索逻辑
}, 300)

// 滚动节流
import { throttle } from '@/utils/performance'

const scrollHandler = throttle((e) => {
  // 滚动逻辑
}, 100)
```

#### 4. 请求并发控制

```javascript
import { RequestController } from '@/utils/performance'

const controller = new RequestController(6) // 最多6个并发

// 批量添加请求
urls.forEach(url => {
  controller.add(() => fetch(url))
})
```

#### 5. 预获取策略

```javascript
import { prefetchManager } from '@/utils/performance'

// 预获取图片
prefetchManager.prefetchUrls(imageUrls)
```

#### 6. 性能监控

```javascript
import { perfMonitor } from '@/utils/performance'

perfMonitor.mark('start')
// ... some operation
perfMonitor.mark('end')
perfMonitor.measure('operation', 'start', 'end')

console.log(perfMonitor.getReport())
```

### 图片资源优化

#### 预加载和缓存

项目已实现精灵图预加载：

```javascript
import dataCache from '@/services/cache'

// 批量预加载
dataCache.preloadImages([
  'https://example.com/sprite1.png',
  'https://example.com/sprite2.png'
])
```

#### 图片优化建议

1. **使用 WebP 格式**（现代浏览器支持更好
2. **启用 CDN 加速
3. **响应式图片**（`srcset`
4. **懒加载可见区域外图片

---

## 后端优化

### 查询优化

#### 1. 数据库索引建议

确保以下表有合适的索引：

```sql
-- 宝可梦表
CREATE INDEX idx_pokemon_species_id ON pokemon_species(id);
CREATE INDEX idx_pokemon_species_identifier ON pokemon_species(identifier);
CREATE INDEX idx_pokemon_species_generation_id ON pokemon_species(generation_id);

-- 属性克制关系表
CREATE INDEX idx_type_efficacy_damage ON type_efficacy(damage_type_id);
CREATE INDEX idx_type_efficacy_target ON type_efficacy(target_type_id);

-- 技能表
CREATE INDEX idx_move_type_id ON moves(type_id);
CREATE INDEX idx_move_generation_id ON moves(generation_id);

-- 特性表
CREATE INDEX idx_ability_generation_id ON abilities(generation_id);

-- 物品表
CREATE INDEX idx_item_category_id ON items(category_id);

-- 对战表索引已优化
```

#### 2. 查询优化建议

**N+1 查询问题已避免方法：

```java
// 优化前
for (Pokemon pokemon : pokemons) {
    // N次查询
    pokemon.setTypes(typeRepository.findByPokemonId(pokemon.getId()));
}

// 优化后
// 一次性查询所有关系
Map<Long, List<Type>> typesMap = typeRepository.findByPokemonIds(pokemonIds);
```

#### 3. 分页查询优化

```java
@GetMapping("/list")
public ResultResponse<Page<PokemonListResponse>> getList(
        @RequestParam(defaultValue = "1") int current,
        @RequestParam(defaultValue = "24") int size
) {
    // 使用数据库分页查询
    Pageable pageable = PageRequest.of(current - 1, size);
    return ResultResponse.success(pokemonService.getPokemonList(pageable));
}
```

### 缓存策略

#### 服务端缓存建议

1. **Spring Cache 集成

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        
        // 定义不同过期时间的缓存
        List<Cache> caches = new ArrayList<>();
        caches.add(new ConcurrentMapCache("pokemonList"));
        caches.add(new ConcurrentMapCache("pokemonDetail"));
        caches.add(new ConcurrentMapCache("moveList"));
        // ...
        cacheManager.setCaches(caches);
        return cacheManager;
    }
}
```

2. **缓存注解使用

```java
@Service
public class PokedexService {
    
    @Cacheable(value = "pokemonDetail", key = "#id")
    public PokemonDetailResponse getPokemonDetail(Long id) {
        // 查询逻辑
    }
    
    @Cacheable(value = "pokemonList")
    public Page<PokemonListResponse> getPokemonList(Pageable pageable) {
        // 查询逻辑
    }
}
```

#### 3. **Redis 缓存（生产环境推荐）

```java
@Configuration
public class RedisCacheConfig {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(factory)
                .withCacheConfiguration("pokemonDetail",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(30)))
                .build();
    }
}
```

#### 响应压缩

```yaml
# application.yml
server:
  compression:
    enabled: true
    mime-types: application/json,text/xml,text/html
```

#### 数据库连接池优化

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
```

---

## 性能监控

### 前端监控

已内置性能监控工具：

```javascript
// 性能标记
perfMonitor.mark('api-start')
// ...
perfMonitor.mark('api-end')
perfMonitor.measure('api-call', 'api-start', 'api-end')

// 获取报告
console.log(perfMonitor.getReport())
```

### 缓存命中监控

```javascript
console.log(dataCache.getStats())
/*
{
  hits: 150,
  misses: 50,
  hitRate: "75.00%",
  currentSize: 180,
  maxSize: 200,
  imageCacheSize: 30
}
*/
```

### 后端性能指标

| 指标 | 目标 |
|------|------|
| API 响应时间 | < 200ms |
| 缓存命中率 | > 70% |
| 首屏加载时间 | < 2s |
| 最大并发请求数 | 6个 |
| 数据库查询时间 | < 50ms |

---

## 部署优化

### 前端构建优化

#### 1. 代码分割（已配置

```javascript
// vite.config.js
export default {
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          'components-vendor': ['@vueuse/core'],
          'api-vendor': ['axios']
        }
      }
    }
  }
}
```

#### 2. 启用压缩

```javascript
export default {
  build: {
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true
      }
    }
  }
}
```

### 后端部署优化

#### 1. 使用生产配置

```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:sqlite:./pokemon-factory.db
  jpa:
    open-in-view: false
  cache:
    type: redis
```

#### 2. JVM 参数优化

```bash
java -Xms512m -Xmx1024m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -jar app.jar
```

#### 3. 反向代理配置

```nginx
server {
    listen 80;
    
    # 静态资源缓存
    location /static/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    # API 缓存
    location /api/ {
        proxy_pass http://backend;
        proxy_cache api_cache;
        proxy_cache_valid 200 10m;
    }
}
```

---

## 优化效果评估

### 预期性能提升

| 优化项 | 预期提升 |
|--------|---------|
| API 请求减少 | 70-80% |
| 页面加载速度 | 50-60% |
| 图片加载速度 | 40-50% |
| 内存占用优化 | 30-40% |

### 监控数据

- 优化前：约 1200+ 次 API 请求 / 10 分钟
- 优化后：约 200-300 次 API 请求 / 10 分钟

---

## 最佳实践

### 1. 开发建议

#### 前端开发

1. 使用 `dataCache.getOrFetch()` 获取数据
2. 列表使用虚拟化滚动
3. 图片使用懒加载
4. 搜索使用防抖
5. 高频事件使用节流

#### 后端开发

1. 复杂查询使用缓存
2. 使用分页查询
3. 避免 N+1 查询
4. 合理使用索引
5. 考虑使用 Redis
