<template>
  <header class="modern-navbar" :class="{ 'scrolled': isScrolled, 'dark-mode': isDarkMode }">
    <div class="navbar-container">
      <!-- Logo -->
      <router-link to="/" class="navbar-logo">
        <div class="logo-icon">
          <svg viewBox="0 0 100 100" class="logo-svg">
            <defs>
              <linearGradient id="pokeball-gradient" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" style="stop-color:#ff6b6b" />
                <stop offset="100%" style="stop-color:#ee5a24" />
              </linearGradient>
              <filter id="pokeball-shadow">
                <feDropShadow dx="0" dy="2" stdDeviation="3" flood-color="rgba(0,0,0,0.3)" />
              </filter>
            </defs>
            <circle cx="50" cy="50" r="46" fill="url(#pokeball-gradient)" stroke="#1a1a1a" stroke-width="4" filter="url(#pokeball-shadow)" />
            <rect x="4" y="48" width="92" height="4" fill="#1a1a1a" />
            <circle cx="50" cy="50" r="46" fill="none" stroke="#1a1a1a" stroke-width="4" />
            <circle cx="50" cy="50" r="12" fill="#fff" stroke="#1a1a1a" stroke-width="4" />
            <circle cx="50" cy="50" r="6" fill="#fff" stroke="#1a1a1a" stroke-width="2" />
            <circle cx="50" cy="50" r="48" fill="none" stroke="rgba(255,255,255,0.3)" stroke-width="1" />
            <path d="M4,50 A46,46 0 0,1 50,4" fill="none" stroke="rgba(255,255,255,0.2)" stroke-width="2" />
          </svg>
        </div>
        <div class="logo-text">
          <h1 class="logo-title">
            <span class="text-gradient-red">Pokemon</span>
            <span class="text-neutral">Factory</span>
          </h1>
          <p class="logo-subtitle">DEX & BATTLE SIM</p>
        </div>
      </router-link>

      <!-- 桌面导航 -->
      <nav class="desktop-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ 'active': isActiveRoute(item.path) }"
        >
          <span class="nav-icon">{{ item.icon }}</span>
          <span class="nav-text">{{ item.name }}</span>
          <span class="nav-badge" v-if="item.badge">{{ item.badge }}</span>
        </router-link>
      </nav>

      <!-- 右侧操作区 -->
      <div class="navbar-actions">
        <!-- 主题切换 -->
        <button class="theme-toggle" @click="toggleTheme" :title="isDarkMode ? '切换到浅色模式' : '切换到深色模式'">
          <span class="theme-icon">{{ isDarkMode ? '☀️' : '🌙' }}</span>
        </button>

        <!-- 用户菜单 -->
        <div class="user-menu" v-if="isAuthenticated">
          <button class="user-button" @click="toggleUserMenu">
            <div class="user-avatar">
              <span class="avatar-text">{{ userInitial }}</span>
            </div>
            <span class="user-name">{{ userName }}</span>
            <svg class="menu-arrow" :class="{ 'open': showUserMenu }" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clip-rule="evenodd" />
            </svg>
          </button>

          <!-- 下拉菜单 -->
          <transition name="dropdown">
            <div class="user-dropdown" v-if="showUserMenu">
              <div class="dropdown-header">
                <div class="dropdown-avatar">
                  <span class="avatar-text">{{ userInitial }}</span>
                </div>
                <div class="dropdown-info">
                  <div class="dropdown-name">{{ userName }}</div>
                  <div class="dropdown-email">{{ userEmail }}</div>
                </div>
              </div>
              <div class="dropdown-divider"></div>
              <router-link to="/profile" class="dropdown-item" @click="closeUserMenu">
                <span class="item-icon">👤</span>
                <span class="item-text">个人资料</span>
              </router-link>
              <router-link to="/settings" class="dropdown-item" @click="closeUserMenu">
                <span class="item-icon">⚙️</span>
                <span class="item-text">设置</span>
              </router-link>
              <div class="dropdown-divider"></div>
              <button class="dropdown-item logout" @click="logout">
                <span class="item-icon">🚪</span>
                <span class="item-text">退出登录</span>
              </button>
            </div>
          </transition>
        </div>

        <!-- 登录按钮 -->
        <router-link v-else to="/login" class="login-button">
          <span class="login-icon">👤</span>
          <span class="login-text">登录</span>
        </router-link>

        <!-- 移动端菜单按钮 -->
        <button class="mobile-menu-button" @click="toggleMobileMenu">
          <span class="menu-icon" :class="{ 'open': showMobileMenu }">
            <span></span>
            <span></span>
            <span></span>
          </span>
        </button>
      </div>
    </div>

    <!-- 移动端菜单 -->
    <transition name="mobile-menu">
      <div class="mobile-menu" v-if="showMobileMenu">
        <nav class="mobile-nav">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="mobile-nav-item"
            :class="{ 'active': isActiveRoute(item.path) }"
            @click="closeMobileMenu"
          >
            <span class="mobile-nav-icon">{{ item.icon }}</span>
            <span class="mobile-nav-text">{{ item.name }}</span>
          </router-link>
        </nav>
        <div class="mobile-menu-footer">
          <button class="mobile-theme-toggle" @click="toggleTheme">
            <span class="theme-icon">{{ isDarkMode ? '☀️' : '🌙' }}</span>
            <span class="theme-text">{{ isDarkMode ? '浅色模式' : '深色模式' }}</span>
          </button>
        </div>
      </div>
    </transition>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

// 响应式状态
const isScrolled = ref(false)
const isDarkMode = ref(false)
const showUserMenu = ref(false)
const showMobileMenu = ref(false)

// 用户信息（示例）
const isAuthenticated = ref(false)
const userName = ref('用户')
const userEmail = ref('user@example.com')

// 导航项
const navItems = [
  { path: '/', name: '首页', icon: '🏠' },
  { path: '/pokemon', name: '图鉴', icon: '📚' },
  { path: '/battle', name: '对战', icon: '⚔️' },
  { path: '/factory', name: '工厂', icon: '🏭' },
  { path: '/damage-calc', name: '伤害计算', icon: '🧮' }
]

// 计算属性
const userInitial = computed(() => {
  return userName.value ? userName.value.charAt(0).toUpperCase() : 'U'
})

// 方法
function isActiveRoute(path) {
  return route.path === path
}

function toggleTheme() {
  isDarkMode.value = !isDarkMode.value
  document.body.classList.toggle('dark-mode', isDarkMode.value)
  localStorage.setItem('theme', isDarkMode.value ? 'dark' : 'light')
}

function toggleUserMenu() {
  showUserMenu.value = !showUserMenu.value
}

function closeUserMenu() {
  showUserMenu.value = false
}

function toggleMobileMenu() {
  showMobileMenu.value = !showMobileMenu.value
  document.body.style.overflow = showMobileMenu.value ? 'hidden' : ''
}

function closeMobileMenu() {
  showMobileMenu.value = false
  document.body.style.overflow = ''
}

function logout() {
  // 实现登出逻辑
  isAuthenticated.value = false
  closeUserMenu()
}

// 滚动监听
function handleScroll() {
  isScrolled.value = window.scrollY > 10
}

// 生命周期
onMounted(() => {
  window.addEventListener('scroll', handleScroll)
  
  // 初始化主题
  const savedTheme = localStorage.getItem('theme')
  if (savedTheme === 'dark') {
    isDarkMode.value = true
    document.body.classList.add('dark-mode')
  }
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.modern-navbar {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.modern-navbar.scrolled {
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.modern-navbar.dark-mode {
  background: rgba(15, 23, 42, 0.9);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.modern-navbar.dark-mode.scrolled {
  background: rgba(15, 23, 42, 0.95);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.navbar-container {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 1rem;
  height: 4rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

@media (min-width: 640px) {
  .navbar-container {
    padding: 0 1.5rem;
    height: 4.5rem;
  }
}

/* Logo */
.navbar-logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  text-decoration: none;
  color: inherit;
  transition: transform 0.2s ease;
}

.navbar-logo:hover {
  transform: scale(1.02);
}

.logo-icon {
  width: 2.5rem;
  height: 2.5rem;
  position: relative;
}

.logo-svg {
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
  transition: transform 0.3s ease;
}

.navbar-logo:hover .logo-svg {
  transform: rotate(10deg) scale(1.1);
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.logo-title {
  font-size: 1.25rem;
  font-weight: 800;
  line-height: 1.2;
  margin: 0;
}

.text-gradient-red {
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.text-neutral {
  color: #374151;
}

.dark-mode .text-neutral {
  color: #f9fafb;
}

.logo-subtitle {
  font-size: 0.625rem;
  font-weight: 600;
  color: #9ca3af;
  letter-spacing: 0.1em;
  margin: 0;
}

/* 桌面导航 */
.desktop-nav {
  display: none;
  align-items: center;
  gap: 0.25rem;
}

@media (min-width: 768px) {
  .desktop-nav {
    display: flex;
  }
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 0.75rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: #6b7280;
  text-decoration: none;
  transition: all 0.2s ease;
  position: relative;
}

.nav-item:hover {
  color: #1f2937;
  background: rgba(0, 0, 0, 0.05);
}

.nav-item.active {
  color: #dc2626;
  background: rgba(220, 38, 38, 0.1);
}

.nav-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 2rem;
  height: 2px;
  background: #dc2626;
  border-radius: 1px;
}

.dark-mode .nav-item {
  color: #9ca3af;
}

.dark-mode .nav-item:hover {
  color: #f9fafb;
  background: rgba(255, 255, 255, 0.1);
}

.dark-mode .nav-item.active {
  color: #f87171;
  background: rgba(248, 113, 113, 0.1);
}

.nav-icon {
  font-size: 1rem;
}

.nav-text {
  display: none;
}

@media (min-width: 1024px) {
  .nav-text {
    display: inline;
  }
}

.nav-badge {
  background: #dc2626;
  color: white;
  font-size: 0.625rem;
  font-weight: 700;
  padding: 0.125rem 0.375rem;
  border-radius: 9999px;
  min-width: 1.25rem;
  text-align: center;
}

/* 右侧操作区 */
.navbar-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

/* 主题切换 */
.theme-toggle {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.05);
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.theme-toggle:hover {
  background: rgba(0, 0, 0, 0.1);
  transform: scale(1.05);
}

.dark-mode .theme-toggle {
  background: rgba(255, 255, 255, 0.1);
}

.dark-mode .theme-toggle:hover {
  background: rgba(255, 255, 255, 0.2);
}

.theme-icon {
  font-size: 1.25rem;
}

/* 用户菜单 */
.user-menu {
  position: relative;
}

.user-button {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.75rem;
  border-radius: 0.75rem;
  background: rgba(0, 0, 0, 0.05);
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.user-button:hover {
  background: rgba(0, 0, 0, 0.1);
}

.dark-mode .user-button {
  background: rgba(255, 255, 255, 0.1);
}

.dark-mode .user-button:hover {
  background: rgba(255, 255, 255, 0.2);
}

.user-avatar {
  width: 2rem;
  height: 2rem;
  border-radius: 0.5rem;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-text {
  color: white;
  font-size: 0.875rem;
  font-weight: 700;
}

.user-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: #374151;
}

.dark-mode .user-name {
  color: #f9fafb;
}

.menu-arrow {
  width: 1rem;
  height: 1rem;
  color: #6b7280;
  transition: transform 0.2s ease;
}

.menu-arrow.open {
  transform: rotate(180deg);
}

/* 用户下拉菜单 */
.user-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 0.5rem;
  width: 20rem;
  background: white;
  border-radius: 1rem;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(0, 0, 0, 0.1);
  overflow: hidden;
  z-index: 1000;
}

.dark-mode .user-dropdown {
  background: #1e293b;
  border-color: rgba(255, 255, 255, 0.1);
}

.dropdown-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: linear-gradient(135deg, #f8fafc, #f1f5f9);
}

.dark-mode .dropdown-header {
  background: linear-gradient(135deg, #334155, #1e293b);
}

.dropdown-avatar {
  width: 3rem;
  height: 3rem;
  border-radius: 0.75rem;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  display: flex;
  align-items: center;
  justify-content: center;
}

.dropdown-info {
  flex: 1;
}

.dropdown-name {
  font-size: 1rem;
  font-weight: 700;
  color: #1f2937;
}

.dark-mode .dropdown-name {
  color: #f9fafb;
}

.dropdown-email {
  font-size: 0.875rem;
  color: #6b7280;
}

.dropdown-divider {
  height: 1px;
  background: #e5e7eb;
  margin: 0.5rem 0;
}

.dark-mode .dropdown-divider {
  background: #374151;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  color: #374151;
  text-decoration: none;
  transition: all 0.2s ease;
  border: none;
  background: none;
  width: 100%;
  cursor: pointer;
}

.dropdown-item:hover {
  background: #f3f4f6;
}

.dark-mode .dropdown-item {
  color: #d1d5db;
}

.dark-mode .dropdown-item:hover {
  background: #374151;
}

.dropdown-item.logout {
  color: #dc2626;
}

.dropdown-item.logout:hover {
  background: #fef2f2;
}

.dark-mode .dropdown-item.logout:hover {
  background: #451a1a;
}

.item-icon {
  font-size: 1.125rem;
}

.item-text {
  font-size: 0.875rem;
  font-weight: 500;
}

/* 登录按钮 */
.login-button {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: white;
  border-radius: 0.75rem;
  text-decoration: none;
  font-weight: 600;
  font-size: 0.875rem;
  transition: all 0.2s ease;
  box-shadow: 0 4px 6px -1px rgba(59, 130, 246, 0.3);
}

.login-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 10px -2px rgba(59, 130, 246, 0.4);
}

.login-icon {
  font-size: 1rem;
}

/* 移动端菜单按钮 */
.mobile-menu-button {
  display: flex;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 0.75rem;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.05);
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.mobile-menu-button:hover {
  background: rgba(0, 0, 0, 0.1);
}

.dark-mode .mobile-menu-button {
  background: rgba(255, 255, 255, 0.1);
}

.dark-mode .mobile-menu-button:hover {
  background: rgba(255, 255, 255, 0.2);
}

@media (min-width: 768px) {
  .mobile-menu-button {
    display: none;
  }
}

.menu-icon {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  width: 1.25rem;
  height: 1rem;
  position: relative;
}

.menu-icon span {
  display: block;
  width: 100%;
  height: 2px;
  background: #374151;
  border-radius: 1px;
  transition: all 0.3s ease;
}

.dark-mode .menu-icon span {
  background: #f9fafb;
}

.menu-icon.open span:nth-child(1) {
  transform: rotate(45deg) translate(0.25rem, 0.25rem);
}

.menu-icon.open span:nth-child(2) {
  opacity: 0;
}

.menu-icon.open span:nth-child(3) {
  transform: rotate(-45deg) translate(0.25rem, -0.25rem);
}

/* 移动端菜单 */
.mobile-menu {
  position: fixed;
  top: 4rem;
  left: 0;
  right: 0;
  bottom: 0;
  background: white;
  z-index: 999;
  overflow-y: auto;
  padding: 1rem;
}

.dark-mode .mobile-menu {
  background: #0f172a;
}

@media (min-width: 768px) {
  .mobile-menu {
    display: none;
  }
}

.mobile-nav {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 2rem;
}

.mobile-nav-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border-radius: 1rem;
  color: #374151;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.2s ease;
}

.mobile-nav-item:hover {
  background: #f3f4f6;
}

.mobile-nav-item.active {
  background: rgba(220, 38, 38, 0.1);
  color: #dc2626;
}

.dark-mode .mobile-nav-item {
  color: #d1d5db;
}

.dark-mode .mobile-nav-item:hover {
  background: #1e293b;
}

.dark-mode .mobile-nav-item.active {
  background: rgba(248, 113, 113, 0.1);
  color: #f87171;
}

.mobile-nav-icon {
  font-size: 1.5rem;
}

.mobile-nav-text {
  font-size: 1.125rem;
}

.mobile-menu-footer {
  padding-top: 1rem;
  border-top: 1px solid #e5e7eb;
}

.dark-mode .mobile-menu-footer {
  border-top-color: #374151;
}

.mobile-theme-toggle {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  width: 100%;
  border-radius: 1rem;
  background: none;
  border: none;
  cursor: pointer;
  color: #374151;
  font-weight: 600;
  transition: all 0.2s ease;
}

.mobile-theme-toggle:hover {
  background: #f3f4f6;
}

.dark-mode .mobile-theme-toggle {
  color: #d1d5db;
}

.dark-mode .mobile-theme-toggle:hover {
  background: #1e293b;
}

.theme-text {
  font-size: 1.125rem;
}

/* 下拉动画 */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.3s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.95);
}

/* 移动端菜单动画 */
.mobile-menu-enter-active,
.mobile-menu-leave-active {
  transition: all 0.3s ease;
}

.mobile-menu-enter-from,
.mobile-menu-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}
</style>