<template>
  <div class="modern-home">
    <!-- 英雄区域 -->
    <section class="hero-section">
      <div class="hero-background">
        <div class="hero-gradient"></div>
        <div class="hero-pattern"></div>
        <div class="hero-particles">
          <div v-for="i in 20" :key="i" class="particle" :style="getParticleStyle(i)"></div>
        </div>
      </div>
      
      <div class="hero-content">
        <div class="hero-text">
          <div class="hero-badge">
            <span class="badge-dot"></span>
            <span class="badge-text">v2.0 全新上线</span>
          </div>
          
          <h1 class="hero-title">
            <span class="title-line">欢迎来到</span>
            <span class="title-highlight">Pokemon Factory</span>
          </h1>
          
          <p class="hero-description">
            宝可梦图鉴、技能查询、伤害计算与对战模拟平台
            <br>
            <span class="description-highlight">探索宝可梦世界的无限可能</span>
          </p>
          
          <div class="hero-actions">
            <router-link to="/pokemon" class="hero-button primary">
              <span class="button-icon">📚</span>
              <span class="button-text">浏览图鉴</span>
              <span class="button-arrow">→</span>
            </router-link>
            
            <router-link to="/battle" class="hero-button secondary">
              <span class="button-icon">⚔️</span>
              <span class="button-text">开始对战</span>
            </router-link>
          </div>
          
          <div class="hero-stats">
            <div class="stat-item">
              <div class="stat-number">1025</div>
              <div class="stat-label">宝可梦</div>
            </div>
            <div class="stat-item">
              <div class="stat-number">937</div>
              <div class="stat-label">技能</div>
            </div>
            <div class="stat-item">
              <div class="stat-number">358</div>
              <div class="stat-label">特性</div>
            </div>
            <div class="stat-item">
              <div class="stat-number">2135</div>
              <div class="stat-label">道具</div>
            </div>
          </div>
        </div>
        
        <div class="hero-visual">
          <div class="pokeball-container">
            <div class="pokeball">
              <div class="pokeball-top"></div>
              <div class="pokeball-center">
                <div class="pokeball-button"></div>
              </div>
              <div class="pokeball-bottom"></div>
            </div>
            <div class="pokeball-shadow"></div>
          </div>
          
          <div class="floating-pokemon">
            <div v-for="(pokemon, index) in floatingPokemon" :key="index" 
                 class="pokemon-float" 
                 :style="getPokemonStyle(index)">
              <img :src="pokemon.image" :alt="pokemon.name" class="pokemon-image">
            </div>
          </div>
        </div>
      </div>
      
      <div class="scroll-indicator">
        <div class="scroll-mouse">
          <div class="scroll-wheel"></div>
        </div>
        <span class="scroll-text">向下滚动探索更多</span>
      </div>
    </section>

    <!-- 功能特色 -->
    <section class="features-section">
      <div class="section-header">
        <h2 class="section-title">强大功能</h2>
        <p class="section-description">一站式宝可梦对战平台，满足你所有需求</p>
      </div>
      
      <div class="features-grid">
        <div v-for="(feature, index) in features" :key="index" class="feature-card" :style="{ animationDelay: `${index * 0.1}s` }">
          <div class="feature-icon" :style="{ background: feature.gradient }">
            <span class="icon-emoji">{{ feature.icon }}</span>
          </div>
          <h3 class="feature-title">{{ feature.title }}</h3>
          <p class="feature-description">{{ feature.description }}</p>
          <div class="feature-tags">
            <span v-for="tag in feature.tags" :key="tag" class="feature-tag">{{ tag }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 最新动态 -->
    <section class="news-section">
      <div class="section-header">
        <h2 class="section-title">最新动态</h2>
        <p class="section-description">了解最新的更新和活动</p>
      </div>
      
      <div class="news-grid">
        <div v-for="(news, index) in latestNews" :key="index" class="news-card" :style="{ animationDelay: `${index * 0.1}s` }">
          <div class="news-image">
            <img :src="news.image" :alt="news.title">
            <div class="news-category">{{ news.category }}</div>
          </div>
          <div class="news-content">
            <div class="news-meta">
              <span class="news-date">{{ news.date }}</span>
              <span class="news-read-time">{{ news.readTime }}</span>
            </div>
            <h3 class="news-title">{{ news.title }}</h3>
            <p class="news-excerpt">{{ news.excerpt }}</p>
            <router-link :to="news.link" class="news-link">
              阅读更多 →
            </router-link>
          </div>
        </div>
      </div>
    </section>

    <!-- 数据统计 -->
    <section class="stats-section">
      <div class="stats-background">
        <div class="stats-gradient"></div>
        <div class="stats-pattern"></div>
      </div>
      
      <div class="stats-content">
        <div class="stats-header">
          <h2 class="stats-title">平台数据</h2>
          <p class="stats-description">实时更新的平台使用数据</p>
        </div>
        
        <div class="stats-grid">
          <div v-for="(stat, index) in platformStats" :key="index" class="stat-card" :style="{ animationDelay: `${index * 0.1}s` }">
            <div class="stat-icon" :style="{ background: stat.gradient }">
              <span class="stat-emoji">{{ stat.icon }}</span>
            </div>
            <div class="stat-number">{{ stat.number }}</div>
            <div class="stat-label">{{ stat.label }}</div>
            <div class="stat-change" :class="stat.changeType">
              <span class="change-icon">{{ stat.changeType === 'positive' ? '↑' : '↓' }}</span>
              <span class="change-text">{{ stat.change }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 快速开始 -->
    <section class="cta-section">
      <div class="cta-content">
        <h2 class="cta-title">准备好了吗？</h2>
        <p class="cta-description">
          立即开始你的宝可梦对战之旅，探索无限可能
        </p>
        <div class="cta-actions">
          <router-link to="/pokemon" class="cta-button primary">
            <span class="button-icon">🚀</span>
            <span class="button-text">立即开始</span>
          </router-link>
          <router-link to="/battle" class="cta-button secondary">
            <span class="button-icon">⚔️</span>
            <span class="button-text">挑战对战</span>
          </router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

// 浮动宝可梦数据
const floatingPokemon = ref([
  { name: 'Pikachu', image: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png' },
  { name: 'Charizard', image: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/6.png' },
  { name: 'Blastoise', image: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/9.png' },
  { name: 'Venusaur', image: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/3.png' },
  { name: 'Mewtwo', image: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/150.png' }
])

// 功能特色
const features = ref([
  {
    icon: '📚',
    title: '完整图鉴',
    description: '包含所有世代的宝可梦数据，详细属性、技能、特性一览无余',
    gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    tags: ['1025个宝可梦', '全世代', '详细数据']
  },
  {
    icon: '⚔️',
    title: '对战模拟',
    description: '真实的对战引擎，支持单打、双打、VGC等多种规则',
    gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    tags: ['真实引擎', '多种规则', 'AI对手']
  },
  {
    icon: '🧮',
    title: '伤害计算',
    description: '精确的伤害计算器，考虑所有因素的准确计算',
    gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    tags: ['精确计算', '所有因素', '实时结果']
  },
  {
    icon: '🏭',
    title: '工厂挑战',
    description: '随机队伍挑战模式，考验你的对战策略和应变能力',
    gradient: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
    tags: ['随机队伍', '策略考验', '无限挑战']
  },
  {
    icon: '📊',
    title: '数据分析',
    description: '详细的使用率统计、胜率分析，助你优化队伍配置',
    gradient: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
    tags: ['使用率', '胜率分析', '队伍优化']
  },
  {
    icon: '🎯',
    title: '精准匹配',
    description: '智能匹配系统，找到实力相当的对手进行对战',
    gradient: 'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)',
    tags: ['智能匹配', '实力相当', '公平对战']
  }
])

// 最新动态
const latestNews = ref([
  {
    title: 'v2.0 版本更新：全新对战引擎上线',
    excerpt: '我们很高兴地宣布 v2.0 版本正式发布！全新的对战引擎带来了更真实的战斗体验...',
    image: 'https://images.unsplash.com/photo-1613771404822-7e26e3a7c4e4?w=400&h=250&fit=crop',
    category: '版本更新',
    date: '2026-09-15',
    readTime: '5分钟',
    link: '/news/v2-release'
  },
  {
    title: '宝可梦朱紫 DLC 数据更新完成',
    excerpt: '蓝之圆盘 DLC 的所有宝可梦数据已更新完毕，包括新技能和特性...',
    image: 'https://images.unsplash.com/photo-1611601322175-ef85ff8d6b41?w=400&h=250&fit=crop',
    category: '数据更新',
    date: '2026-09-10',
    readTime: '3分钟',
    link: '/news/scarlet-violet-dlc'
  },
  {
    title: 'VGC 2026 规则调整公告',
    excerpt: '根据官方最新公告，VGC 2026 赛季规则有所调整，以下是详细说明...',
    image: 'https://images.unsplash.com/photo-1542751371-adc38448a05e?w=400&h=250&fit=crop',
    category: '规则公告',
    date: '2026-09-05',
    readTime: '4分钟',
    link: '/news/vgc-2026-rules'
  }
])

// 平台统计
const platformStats = ref([
  {
    icon: '👥',
    number: '12,847',
    label: '活跃用户',
    change: '+12.5%',
    changeType: 'positive',
    gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
  },
  {
    icon: '⚔️',
    number: '89,432',
    label: '对战场次',
    change: '+23.7%',
    changeType: 'positive',
    gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
  },
  {
    icon: '📚',
    number: '1,025',
    label: '宝可梦数据',
    change: '+15',
    changeType: 'positive',
    gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)'
  },
  {
    icon: '🎯',
    number: '98.7%',
    label: '用户满意度',
    change: '+2.3%',
    changeType: 'positive',
    gradient: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)'
  }
])

// 方法
function getParticleStyle(index) {
  const size = Math.random() * 10 + 5
  const left = Math.random() * 100
  const top = Math.random() * 100
  const animationDuration = Math.random() * 10 + 10
  const animationDelay = Math.random() * 5
  
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    top: `${top}%`,
    animationDuration: `${animationDuration}s`,
    animationDelay: `${animationDelay}s`
  }
}

function getPokemonStyle(index) {
  const positions = [
    { top: '10%', left: '10%', animationDuration: '6s' },
    { top: '20%', right: '15%', animationDuration: '7s' },
    { bottom: '25%', left: '20%', animationDuration: '8s' },
    { bottom: '15%', right: '10%', animationDuration: '9s' },
    { top: '50%', left: '50%', animationDuration: '10s' }
  ]
  
  return {
    ...positions[index],
    animationDelay: `${index * 0.5}s`
  }
}

onMounted(() => {
  // 初始化动画
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('animate-in')
      }
    })
  }, { threshold: 0.1 })
  
  document.querySelectorAll('.feature-card, .news-card, .stat-card').forEach(el => {
    observer.observe(el)
  })
})
</script>

<style scoped>
.modern-home {
  min-height: 100vh;
  overflow-x: hidden;
}

/* 英雄区域 */
.hero-section {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  overflow: hidden;
}

.hero-background {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.hero-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  opacity: 0.9;
}

.hero-pattern {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle at 25% 25%, rgba(255,255,255,0.2) 0%, transparent 50%),
                     radial-gradient(circle at 75% 75%, rgba(255,255,255,0.1) 0%, transparent 50%);
}

.hero-particles {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.particle {
  position: absolute;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  animation: float-particle linear infinite;
}

@keyframes float-particle {
  0% {
    transform: translateY(0) rotate(0deg);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  100% {
    transform: translateY(-100vh) rotate(720deg);
    opacity: 0;
  }
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  width: 100%;
  display: flex;
  align-items: center;
  gap: 4rem;
}

@media (max-width: 1024px) {
  .hero-content {
    flex-direction: column;
    text-align: center;
    gap: 3rem;
  }
}

.hero-text {
  flex: 1;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border-radius: 2rem;
  border: 1px solid rgba(255, 255, 255, 0.3);
  margin-bottom: 1.5rem;
  animation: fadeInUp 0.6s ease-out;
}

.badge-dot {
  width: 0.5rem;
  height: 0.5rem;
  background: #4ade80;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.badge-text {
  font-size: 0.875rem;
  font-weight: 600;
  color: white;
}

.hero-title {
  font-size: 3.5rem;
  font-weight: 800;
  line-height: 1.1;
  margin-bottom: 1.5rem;
  animation: fadeInUp 0.6s ease-out 0.1s both;
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 2.5rem;
  }
}

.title-line {
  display: block;
  color: rgba(255, 255, 255, 0.9);
}

.title-highlight {
  display: block;
  background: linear-gradient(135deg, #ffd700, #ff6b6b);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-description {
  font-size: 1.25rem;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.6;
  margin-bottom: 2rem;
  animation: fadeInUp 0.6s ease-out 0.2s both;
}

.description-highlight {
  color: #ffd700;
  font-weight: 600;
}

.hero-actions {
  display: flex;
  gap: 1rem;
  margin-bottom: 3rem;
  animation: fadeInUp 0.6s ease-out 0.3s both;
}

@media (max-width: 480px) {
  .hero-actions {
    flex-direction: column;
  }
}

.hero-button {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 2rem;
  border-radius: 1rem;
  font-weight: 700;
  font-size: 1.125rem;
  text-decoration: none;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.hero-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
  transition: left 0.5s;
}

.hero-button:hover::before {
  left: 100%;
}

.hero-button.primary {
  background: white;
  color: #667eea;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.hero-button.primary:hover {
  transform: translateY(-3px);
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.3);
}

.hero-button.secondary {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: 2px solid rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
}

.hero-button.secondary:hover {
  background: rgba(255, 255, 255, 0.3);
  border-color: rgba(255, 255, 255, 0.6);
  transform: translateY(-3px);
}

.button-icon {
  font-size: 1.25rem;
}

.button-arrow {
  transition: transform 0.3s ease;
}

.hero-button:hover .button-arrow {
  transform: translateX(4px);
}

.hero-stats {
  display: flex;
  gap: 2rem;
  animation: fadeInUp 0.6s ease-out 0.4s both;
}

@media (max-width: 480px) {
  .hero-stats {
    flex-wrap: wrap;
    justify-content: center;
    gap: 1rem;
  }
}

.stat-item {
  text-align: center;
}

.stat-number {
  font-size: 2rem;
  font-weight: 800;
  color: white;
  line-height: 1;
  margin-bottom: 0.25rem;
}

.stat-label {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

/* 视觉元素 */
.hero-visual {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  min-height: 400px;
}

@media (max-width: 1024px) {
  .hero-visual {
    min-height: 300px;
  }
}

.pokeball-container {
  position: relative;
  width: 200px;
  height: 200px;
  animation: float 6s ease-in-out infinite;
}

.pokeball {
  width: 100%;
  height: 100%;
  position: relative;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.pokeball-top {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 50%;
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
}

.pokeball-bottom {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 50%;
  background: white;
}

.pokeball-center {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 20px;
  background: #1a1a1a;
  transform: translateY(-50%);
  z-index: 1;
}

.pokeball-button {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 40px;
  height: 40px;
  background: white;
  border: 8px solid #1a1a1a;
  border-radius: 50%;
  z-index: 2;
}

.pokeball-shadow {
  position: absolute;
  bottom: -20px;
  left: 50%;
  transform: translateX(-50%);
  width: 180px;
  height: 20px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 50%;
  filter: blur(10px);
}

.floating-pokemon {
  position: absolute;
  inset: 0;
}

.pokemon-float {
  position: absolute;
  animation: float-pokemon 6s ease-in-out infinite;
}

@keyframes float-pokemon {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(5deg);
  }
}

.pokemon-image {
  width: 80px;
  height: 80px;
  object-fit: contain;
  filter: drop-shadow(0 10px 20px rgba(0, 0, 0, 0.3));
  transition: transform 0.3s ease;
}

.pokemon-image:hover {
  transform: scale(1.2);
}

/* 滚动指示器 */
.scroll-indicator {
  position: absolute;
  bottom: 2rem;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  animation: fadeInUp 0.6s ease-out 0.8s both;
}

.scroll-mouse {
  width: 2rem;
  height: 3rem;
  border: 2px solid rgba(255, 255, 255, 0.6);
  border-radius: 1rem;
  position: relative;
}

.scroll-wheel {
  position: absolute;
  top: 0.5rem;
  left: 50%;
  transform: translateX(-50%);
  width: 0.25rem;
  height: 0.5rem;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 0.125rem;
  animation: scroll-wheel 2s infinite;
}

@keyframes scroll-wheel {
  0% {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
  100% {
    opacity: 0;
    transform: translateX(-50%) translateY(1rem);
  }
}

.scroll-text {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

/* 功能特色 */
.features-section {
  padding: 5rem 2rem;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
}

.section-header {
  text-align: center;
  margin-bottom: 4rem;
}

.section-title {
  font-size: 2.5rem;
  font-weight: 800;
  color: #1f2937;
  margin-bottom: 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.section-description {
  font-size: 1.125rem;
  color: #6b7280;
  max-width: 600px;
  margin: 0 auto;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.feature-card {
  background: white;
  border-radius: 1.5rem;
  padding: 2rem;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  opacity: 0;
  transform: translateY(20px);
}

.feature-card.animate-in {
  opacity: 1;
  transform: translateY(0);
}

.feature-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
}

.feature-icon {
  width: 4rem;
  height: 4rem;
  border-radius: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1.5rem;
  font-size: 2rem;
}

.feature-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 0.75rem;
}

.feature-description {
  font-size: 0.875rem;
  color: #6b7280;
  line-height: 1.6;
  margin-bottom: 1.5rem;
}

.feature-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.feature-tag {
  padding: 0.25rem 0.75rem;
  background: #f3f4f6;
  color: #374151;
  font-size: 0.75rem;
  font-weight: 500;
  border-radius: 2rem;
}

/* 最新动态 */
.news-section {
  padding: 5rem 2rem;
  background: white;
}

.news-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.news-card {
  background: white;
  border-radius: 1.5rem;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  opacity: 0;
  transform: translateY(20px);
}

.news-card.animate-in {
  opacity: 1;
  transform: translateY(0);
}

.news-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
}

.news-image {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.news-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.news-card:hover .news-image img {
  transform: scale(1.1);
}

.news-category {
  position: absolute;
  top: 1rem;
  left: 1rem;
  padding: 0.375rem 0.75rem;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 0.5rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: #374151;
}

.news-content {
  padding: 1.5rem;
}

.news-meta {
  display: flex;
  gap: 1rem;
  margin-bottom: 0.75rem;
  font-size: 0.75rem;
  color: #9ca3af;
}

.news-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 0.75rem;
  line-height: 1.4;
}

.news-excerpt {
  font-size: 0.875rem;
  color: #6b7280;
  line-height: 1.6;
  margin-bottom: 1rem;
}

.news-link {
  font-size: 0.875rem;
  font-weight: 600;
  color: #667eea;
  text-decoration: none;
  transition: color 0.2s ease;
}

.news-link:hover {
  color: #764ba2;
}

/* 数据统计 */
.stats-section {
  position: relative;
  padding: 5rem 2rem;
  overflow: hidden;
}

.stats-background {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.stats-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 50%, #8b5cf6 100%);
}

.stats-pattern {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle at 25% 25%, rgba(255,255,255,0.1) 0%, transparent 50%),
                     radial-gradient(circle at 75% 75%, rgba(255,255,255,0.05) 0%, transparent 50%);
}

.stats-content {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
}

.stats-header {
  text-align: center;
  margin-bottom: 4rem;
}

.stats-title {
  font-size: 2.5rem;
  font-weight: 800;
  color: white;
  margin-bottom: 1rem;
}

.stats-description {
  font-size: 1.125rem;
  color: rgba(255, 255, 255, 0.8);
  max-width: 600px;
  margin: 0 auto;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 2rem;
}

.stat-card {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  border-radius: 1.5rem;
  padding: 2rem;
  text-align: center;
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
  opacity: 0;
  transform: translateY(20px);
}

.stat-card.animate-in {
  opacity: 1;
  transform: translateY(0);
}

.stat-card:hover {
  transform: translateY(-5px);
  background: rgba(255, 255, 255, 0.15);
}

.stat-icon {
  width: 4rem;
  height: 4rem;
  border-radius: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.5rem;
  font-size: 2rem;
}

.stat-number {
  font-size: 2.5rem;
  font-weight: 800;
  color: white;
  margin-bottom: 0.5rem;
}

.stat-label {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 1rem;
}

.stat-change {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.75rem;
  border-radius: 2rem;
  font-size: 0.75rem;
  font-weight: 600;
}

.stat-change.positive {
  background: rgba(74, 222, 128, 0.2);
  color: #4ade80;
}

.stat-change.negative {
  background: rgba(248, 113, 113, 0.2);
  color: #f87171;
}

/* 快速开始 */
.cta-section {
  padding: 5rem 2rem;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
}

.cta-content {
  max-width: 800px;
  margin: 0 auto;
  text-align: center;
}

.cta-title {
  font-size: 2.5rem;
  font-weight: 800;
  color: #1f2937;
  margin-bottom: 1rem;
}

.cta-description {
  font-size: 1.125rem;
  color: #6b7280;
  margin-bottom: 2rem;
  line-height: 1.6;
}

.cta-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

@media (max-width: 480px) {
  .cta-actions {
    flex-direction: column;
    align-items: center;
  }
}

.cta-button {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 2rem;
  border-radius: 1rem;
  font-weight: 700;
  font-size: 1.125rem;
  text-decoration: none;
  transition: all 0.3s ease;
}

.cta-button.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
}

.cta-button.primary:hover {
  transform: translateY(-3px);
  box-shadow: 0 15px 40px rgba(102, 126, 234, 0.5);
}

.cta-button.secondary {
  background: white;
  color: #667eea;
  border: 2px solid #667eea;
}

.cta-button.secondary:hover {
  background: #667eea;
  color: white;
  transform: translateY(-3px);
}
</style>