/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // 宝可梦品牌色系
        brand: {
          50: '#fef7ee',
          100: '#fdedd3',
          200: '#f9d7a5',
          300: '#f5b96d',
          400: '#f09333',
          500: '#ec7a11',
          600: '#dd5f07',
          700: '#b74609',
          800: '#92380e',
          900: '#76300f',
          // 保留旧的蓝色系用于辅助
          blue: {
            50: '#eff6ff', 100: '#dbeafe', 200: '#bfdbfe', 300: '#93c5fd',
            400: '#60a5fa', 500: '#3b82f6', 600: '#2563eb', 700: '#1d4ed8',
            800: '#1e40af', 900: '#1e3a8a',
          }
        },
        // Pokemon 正作红 (经典红白球红)
        poke: {
          red: '#DC2626',
          redDark: '#B91C1C',
          redLight: '#FEE2E2',
          blue: '#1E40AF',
          blueDark: '#1E3A8A',
          blueLight: '#DBEAFE',
          yellow: '#FACC15',
          yellowLight: '#FEF9C3',
          cream: '#FFFBEB',
        },
        // 宝可梦 18 属性色 (更饱和、更正作)
        type: {
          normal: '#A8A878',
          fire: '#F08030',
          water: '#6890F0',
          electric: '#F8D030',
          grass: '#78C850',
          ice: '#98D8D8',
          fighting: '#C03028',
          poison: '#A040A0',
          ground: '#E0C068',
          flying: '#A890F0',
          psychic: '#F85888',
          bug: '#A8B820',
          rock: '#B8A038',
          ghost: '#705898',
          dragon: '#7038F8',
          dark: '#705848',
          steel: '#B8B8D0',
          fairy: '#EE99AC',
        },
        // 状态色
        hp: {
          low: '#ef4444',
          mid: '#f59e0b',
          high: '#22c55e',
        }
      },
      fontFamily: {
        display: ['"Inter"', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        pokemon: ['"Pokemon GB"', '"Press Start 2P"', 'monospace'],
      },
      borderRadius: {
        'poke': '0.75rem',
        'poke-lg': '1rem',
        'poke-xl': '1.5rem',
      },
      boxShadow: {
        'poke': '0 2px 0 0 rgba(0,0,0,0.15), inset 0 1px 0 1px rgba(255,255,255,0.3)',
        'poke-lg': '0 4px 0 0 rgba(0,0,0,0.15), inset 0 2px 0 1px rgba(255,255,255,0.3)',
        'poke-inset': 'inset 0 2px 4px 0 rgba(0,0,0,0.08)',
        'poke-card': '0 2px 0 0 rgba(0,0,0,0.1), 0 8px 24px -4px rgba(0,0,0,0.08)',
        'poke-glow': '0 0 20px -4px',
      },
      borderWidth: {
        '3': '3px',
      },
      animation: {
        'fade-in': 'fadeIn 0.3s ease-out',
        'slide-up': 'slideUp 0.35s ease-out',
        'slide-down': 'slideDown 0.35s ease-out',
        'scale-in': 'scaleIn 0.25s ease-out',
        'pulse-soft': 'pulseSoft 2s ease-in-out infinite',
        'shimmer': 'shimmer 2s linear infinite',
        'pokeball-spin': 'pokeballSpin 1.2s linear infinite',
        'pokeball-bounce': 'pokeballBounce 0.6s ease-in-out infinite alternate',
        'float': 'float 3s ease-in-out infinite',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(16px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        slideDown: {
          '0%': { opacity: '0', transform: 'translateY(-8px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        scaleIn: {
          '0%': { opacity: '0', transform: 'scale(0.95)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
        pulseSoft: {
          '0%, 100%': { opacity: '1' },
          '50%': { opacity: '0.7' },
        },
        shimmer: {
          '0%': { backgroundPosition: '200% 0' },
          '100%': { backgroundPosition: '-200% 0' },
        },
        pokeballSpin: {
          '0%': { transform: 'rotate(0deg)' },
          '100%': { transform: 'rotate(360deg)' },
        },
        pokeballBounce: {
          '0%': { transform: 'translateY(0) scale(1)' },
          '100%': { transform: 'translateY(-8px) scale(1.05)' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-10px)' },
        },
      },
      backdropBlur: {
        xs: '2px',
      },
    },
  },
  plugins: [],
}
