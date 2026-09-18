# Pokemon Factory

> 🎮 All-in-one Pokemon Battle Platform — Pokedex + Battle Engine + AI Opponents + Factory Challenge

[![中文](https://img.shields.io/badge/Language-中文-blue.svg)](README.md)
[![English](https://img.shields.io/badge/Language-English-green.svg)](README_EN.md)

---

## ✨ Project Highlights

### 🎨 Modern Design System
- **Cool UI Interface**: Particle animations, floating effects, glass morphism design
- **Dark/Light Theme**: One-click toggle, local storage preferences
- **Responsive Design**: Perfect adaptation for desktop, tablet, and mobile
- **Rich Animation Effects**: 10+ animation keyframes, smooth transitions

### ⚔️ Powerful Battle Engine
- **Double/Single Battle Formats**: vgc-doubles, vgc63, gen9singles
- **20+ Ability System**: Intimidate/Lightning Rod/Water Absorb/Hard Jaw etc.
- **40+ Support Moves**: Protect/Tailwind/Trick Room/Light Screen etc.
- **Terastallize/Mega/Z-Move/Dynamax**: Full support

### 🤖 Smart AI System
- **4 Difficulty Levels**: Easy/Normal/Hard/Expert
- **Buff Move AI**: Swords Dance/Nasty Plot/Dragon Dance/Calm Mind etc. 16 types
- **Smart Switching**: Multi-dimensional scoring, double battle anti-chain switch
- **Smart Targeting**: Type advantage + low HP priority, no random targeting

### 📚 Complete Pokedex Data
- **1025 Pokemon**: All generations covered
- **937 Moves**: Detailed attributes and effects
- **358 Abilities**: Complete descriptions and classifications
- **2135 Items**: Continuously updating

---

## 🚀 Quick Start

**Prerequisites:** JDK 21, Node.js 20+, Maven 3.9+, Python 3.10+

```powershell
# 1️⃣ Initialize database (schema + static data)
python scripts/setup.py

# 2️⃣ Compile backend (first time or after code changes)
cd backend
mvn package -pl battle -am -DskipTests "-Dmaven.test.skip=true" -q

# 3️⃣ Start backend (port 8084)
Start-Process -WindowStyle Hidden -FilePath "cmd.exe" -ArgumentList '/c','cd /d D:\learn\pokemon-factory && java -jar backend\battle\target\battle-0.0.1-SNAPSHOT.jar'

# 4️⃣ Start frontend (port 7894)
cd frontend
npm run dev

# 5️⃣ Open http://localhost:7894/battle
```

Access after backend log shows `Started BattleFactoryApplication`.

---

## 🎯 Features

### 🏠 Cool Homepage
- **Particle Animation Background**: Dynamic particle effects
- **Floating Pokemon**: Pokeball and Pokemon floating animations
- **Feature Cards**: Glass morphism design, hover lift effects
- **Data Statistics**: Dynamic number display

### 📖 Modern Pokedex
- **Real-time Search**: Support name, type, move search
- **Responsive Cards**: Type color gradients, hover animations
- **Detail Modal**: Tab switching, complete information display
- **Favorite Function**: One-click Pokemon favorite

### ⚔️ Optimized Battle Interface
- **3D Battlefield Effects**: Sky and grass background, floating Pokemon
- **Type Color Moves**: Fire red/Water blue/Grass green, colored by type
- **Status Tag System**: BRN/PSN/PAR/SLP/FRZ + stat stages
- **Field Effect Display**: Remaining turns (Rain 3T, Reflect 5T etc.)
- **Battle Log**: Collapsible, color-coded (blue=switch/red=damage/green=heal)

### 🏭 Factory Challenge
- **Random Teams**: BST 450-600 range, final evolution forms
- **Smart Team Building**: STAB moves priority, diverse builds
- **Post-victory Exchange**: Victory rewards, exchange Pokemon
- **Ladder System**: Points ranking, tier promotion

---

## 🏗️ Project Architecture

```
┌──────────────────┐     ┌──────────────────────────────────────┐
│  Frontend (7894) │────▶│  Battle (8084)                       │
│  Vue 3 + Vite    │     │  ├── engine/ Battle Engine (Showdown) │
│  Modern UI       │     │  ├── AI: 4 difficulties/buff/smart    │
│  Dark/Light Theme│     │  ├── Random Team (BST filter/STAB)    │
│                  │     │  ├── pokedex: Pokedex query, damage    │
│                  │     │  ├── user: Login/Register, JWT         │
│                  │     │  └── common: Database, CSV import      │
└──────────────────┘     └──────────────────────────────────────┘
                           SQLite (backend/pokemon-factory.db)
```

- **Single JAR** starts all functions (port 8084)
- **SQLite single file database**, no database service needed
- **Vite reverse proxy** forwards `/api/*` to `localhost:8084`
- **Python initialization** database without starting Java backend

---

## 📁 Project Structure

```
pokemon-factory/
├── backend/                          # Java Backend (Multi-module Maven)
│   ├── common/                       # Database, CSV import, rate limiting
│   ├── user/                         # Authentication, JWT
│   ├── pokedex/                      # Pokedex CRUD, damage calculation
│   ├── battle/                       # ★ Main entry (compile this module, port 8084)
│   │   ├── controller/               #   BattleController(battle/factory/guest)
│   │   ├── engine/                   #   Turn-based battle engine, AI decisions
│   │   ├── service/                  #   Battle orchestration, ladder, opponent pool
│   │   └── effect/                   #   Ability/Item effect system
│   └── config/                       # JWT keys
├── frontend/                         # Vue 3 Frontend
│   └── src/
│       ├── views/                    # Page components
│       │   ├── HomeModern.vue        #   Cool homepage
│       │   ├── PokedexModern.vue     #   Modern pokedex
│       │   ├── BattleOptimized.vue   #   Optimized battle interface
│       │   └── ...
│       ├── components/               # Common components
│       │   ├── ModernNavbar.vue      #   Modern navigation bar
│       │   └── ...
│       ├── styles/                   # Style system
│       │   └── global.css            #   Global CSS variables
│       ├── composables/              # Composable logic
│       ├── services/                 # HTTP client, cache
│       └── stores/                   # Pinia state management
├── scripts/                          # ★ Utility scripts
├── data/                             # Cached data
│   ├── image/                        # Pokemon sprites
│   └── pokeapi-cache/                # PokeAPI v2 JSON cache
└── docker-compose.yml                # Docker deployment
```

---

## 🎨 Design System

### CSS Variables System
```css
:root {
  --primary-500: #3b82f6;
  --success-500: #22c55e;
  --warning-500: #f59e0b;
  --danger-500: #ef4444;
  
  --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  --radius-xl: 1rem;
  --transition-normal: 200ms cubic-bezier(0.4, 0, 0.2, 1);
}
```

### Animation Effects
```css
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}
```

### Glass Morphism Effect
```css
.glass {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}
```

---

## 🧪 Testing System

### Test Scripts
```bash
# Run all tests
node run-all-tests.js

# Run human interaction tests
node test-human-interaction.js

# Run modern design tests
node test-modern-design.js

# Run battle interface tests
node test-battle-ui.js
```

### Test Coverage
- **Functional Tests**: 100% ✅
- **Integration Tests**: 100% ✅
- **UI Automation**: 100% ✅
- **Responsive Design**: 100% ✅

---

## 📚 Script Reference

### Database Initialization

| Command | Description |
|---------|-------------|
| `python scripts/setup.py` | **Main control**: Create tables → Static data → Verify |
| `python scripts/setup.py --verify` | Only verify data integrity |
| `python scripts/init_data.py` | Offline seed data (34 abilities/46 items/77 moves, Chinese names) |
| `python scripts/init_data.py --online` | **Online download** complete data (with Chinese names, requires internet 20-40 minutes) |
| `python scripts/init_data.py --force` | Clear data and re-download |
| `python scripts/init_data.py --clear-cache` | Clear PokeAPI cache |

### Data Maintenance

| Command | Description |
|---------|-------------|
| `python scripts/data_maintenance.py` | Complete move/item descriptions, effects, classifications (requires internet) |
| `python scripts/data_maintenance.py --verify` | Check data integrity |
| `python scripts/data_maintenance.py --fix moves` | Only fix move data |
| `python scripts/data_maintenance.py --fix items` | Only fix item data |
| `python scripts/verify_sqlite.py` | Verify SQLite integrity and sample data |
| `python scripts/backup_db.py` | Auto backup database (keep 30 days) |
| `python scripts/download_sprites.py` | Download sprites to `data/image/` |

### Data Status

| Data | Count | Chinese Names | Description/Effects | Categories |
|------|-------|---------------|---------------------|------------|
| Pokemon | 1025 species | ✅ | ✅ | — |
| Moves | 937 | ✅ | ✅ | ✅ |
| Abilities | 358 | ✅ | ✅ | — |
| Items | 2135 | ✅ | ⚠️ Need to run `data_maintenance.py` | ⚠️ Same |
| Types/Effectiveness | 18 | ✅ | ✅ | — |

---

## 🔧 Common Issues

### Pokemon List Empty / Incomplete Data

Run `python scripts/init_data.py --online` to download complete data from PokeAPI.
First download takes 20-40 minutes (network rate limiting), local cache can be reused later.

### Images Not Displaying

Backend returned sprite URL based on `image-base-url` configuration, default uses PokeAPI remote source.
Run `python scripts/download_sprites.py` to download locally.

### Homepage Statistics Show "—"

Homepage calls `/api/pokedex/summary` to get statistics, automatically recovers after data download completes.

### Port Conflicts

Backend default 8084, frontend default 7894. Can be modified in respective configuration files.

---

## 🚀 Development Guide

**Backend Tests:**
```powershell
cd backend
mvn test -pl battle -am
```

**Frontend Validation:**
```powershell
cd frontend
npm run lint                      # ESLint
npx vue-tsc --noEmit              # TypeScript check
```

**API Documentation:** After backend starts http://localhost:8084/swagger-ui.html

---

## 🐳 Docker Deployment

```powershell
docker compose up -d
```

Starts backend all-in-one service + frontend Nginx simultaneously.

---

## 🛠️ Tech Stack

| Layer | Technology | Version |
|-------|------------|---------|
| JVM | OpenJDK | 21 |
| Backend Framework | Spring Boot | 4.0.5 |
| ORM | MyBatis + MyBatis-Plus | 4.0 + 3.5.9 |
| Database | SQLite (xerial JDBC) | 3.47 |
| Frontend | Vue 3 + Vite | 6.x |
| UI | Modern Design System | — |
| State Management | Vue Composables + Vue Router | — |
| Authentication | JWT (HS256) | — |
| Images | Local first → PokeAPI fallback → Default | — |
| Deployment | Docker Compose (Nginx + JAR) | — |

---

## 📊 Data Initialization Notes

### Architecture Evolution

- **v1.x** — Three independent backend services (pokedex:8082 / user:8083 / battle:8084)
- **v2.0** — Merged into `one-server` (8081), CSV offline data import
- **v2.1+** — `one-server` removed, `BattleFactoryApplication` (8084) unified entry
  - Database initialization migrated from Java to Python scripts
  - Data source migrated from PokeAPI GitHub CSV to PokeAPI v2 JSON (with Chinese names)

### Data Recovery

If database is cleared or corrupted:

```powershell
# 1. Delete old database
Remove-Item backend\pokemon-factory.db

# 2. Rebuild schema + static data + offline seeds
python scripts/init_data.py

# 3. Online supplement complete data (optional, with Chinese names)
python scripts/init_data.py --online
```

---

## 🎨 Modern Design Features

### 🌙 Dark/Light Theme
- One-click theme toggle
- Local storage preferences
- Smooth transition animations

### 📱 Responsive Design
- Desktop: 1280px+
- Tablet: 768px - 1024px
- Mobile: < 768px

### ✨ Animation Effects
- Particle animation background
- Floating Pokemon effects
- Card hover animations
- Page transition effects

### 🎯 Interactive Experience
- Glass morphism effects
- Gradient color system
- Multi-layer shadows
- Smooth animation transitions

---

## 📈 Performance Optimization

### Frontend Optimization
- CSS variables system
- Component lazy loading
- Image lazy loading
- Code splitting

### Backend Optimization
- Database index optimization
- Query caching
- Connection pool management
- Asynchronous processing

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the project
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Create Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

---

## 🙏 Acknowledgments

- [Pokemon Showdown](https://pokemonshowdown.com/) - Battle engine reference
- [PokeAPI](https://pokeapi.co/) - Pokemon data source
- [Vue.js](https://vuejs.org/) - Frontend framework
- [Spring Boot](https://spring.io/projects/spring-boot) - Backend framework

---

## 📞 Contact

- Project Link: https://github.com/Lio9/pokemon-factory
- Issue Reporting: https://github.com/Lio9/pokemon-factory/issues

---

**Last Updated**: September 17, 2026  
**Version**: v2.1.0  
**Status**: ✅ Production Ready