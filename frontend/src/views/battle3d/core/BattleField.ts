/**
 * Battlefield.ts - 3D Battle Environment Manager
 * Battlefield.ts - 3D 战场环境管理器
 * 
 * Creates and manages a Pokémon-style 3D battlefield with:
 * - Elliptical grass platform with gradient coloring
 * - Player and opponent sides with distinct visual themes
 * - Position markers for Pokémon placement
 * - Decorative elements (flowers, rocks, grass tufts)
 * - Semi-transparent boundary walls
 * 
 * 创建和管理宝可梦风格的 3D 战场环境，包含：
 * - 椭圆形草地平台，带渐变色彩
 * - 玩家侧和对手侧，视觉主题不同
 * - 宝可梦站立位置标记
 * - 装饰物（花草、岩石、草丛）
 * - 半透明边界围墙
 * 
 * @example
 * ```typescript
 * const scene = new THREE.Scene();
 * const battlefield = new Battlefield(scene);
 * 
 * // Highlight a position
 * battlefield.setPositionMarker('player', 0, true);
 * 
 * // Get world position for Pokémon placement
 * const pos = battlefield.getSlotWorldPosition('opponent', 1);
 * 
 * // Clean up when done
 * battlefield.dispose();
 * ```
 * 
 * @author MiMo
 * @version 1.0.0
 */

import * as THREE from 'three';

/**
 * Side identifier type
 * 阵营标识类型
 */
export type Side = 'player' | 'opponent';

/**
 * Slot index type (0 or 1)
 * 槽位索引类型（0 或 1）
 */
export type SlotIndex = 0 | 1;

/**
 * Slot configuration structure
 * 槽位配置结构
 */
interface SlotConfig {
  /** Position offset from center X / X 轴偏移 */
  x: number;
  /** Position offset from center Z / Z 轴偏移 */
  z: number;
}

/**
 * Battlefield class - Manages 3D battle environment
 * Battlefield 类 - 管理 3D 战场环境
 * 
 * Creates a Pokémon-style arena with grass platform, position markers,
 * decorative elements, and boundary walls.
 * 
 * 创建宝可梦风格的竞技场，包含草地平台、位置标记、装饰物和边界围墙。
 */
export class Battlefield {
  /** Three.js scene reference / Three.js 场景引用 */
  private scene: THREE.Scene;
  
  /** Main battlefield group for easy management / 主战场组，便于管理 */
  private battlefieldGroup: THREE.Group;
  
  /** Position marker meshes mapped by side and slot / 位置标记网格，按阵营和槽位索引 */
  private positionMarkers: Map<string, THREE.Mesh> = new Map();
  
  /** Materials that need disposal / 需要清理的材质 */
  private materials: THREE.Material[] = [];
  
  /** Geometries that need disposal / 需要清理的几何体 */
  private geometries: THREE.BufferGeometry[] = [];
  
  /** Textures that need disposal / 需要清理的纹理 */
  private textures: THREE.Texture[] = [];

  /** Slot configurations for each side / 每侧的槽位配置 */
  private readonly slotConfigs: Record<Side, SlotConfig[]> = {
    player: [
      { x: -3.5, z: 3.5 },  // Left slot / 左侧槽位
      { x: 3.5, z: 3.5 }    // Right slot / 右侧槽位
    ],
    opponent: [
      { x: -3.5, z: -3.5 }, // Left slot / 左侧槽位
      { x: 3.5, z: -3.5 }   // Right slot / 右侧槽位
    ]
  };

  /** Battlefield dimensions / 战场尺寸 */
  private readonly FIELD_WIDTH = 20;
  private readonly FIELD_DEPTH = 14;
  private readonly PLATFORM_HEIGHT = 0.3;
  private readonly MARKER_RADIUS = 1.2;
  private readonly WALL_HEIGHT = 1.0;

  /**
   * Constructor - Creates the battlefield
   * 构造函数 - 创建战场
   * 
   * @param scene - Three.js scene to add battlefield to / 要添加战场的 Three.js 场景
   */
  constructor(scene: THREE.Scene) {
    this.scene = scene;
    this.battlefieldGroup = new THREE.Group();
    this.battlefieldGroup.name = 'Battlefield';
    
    // Create battlefield elements / 创建战场元素
    this.createGroundPlatform();
    this.createPositionMarkers();
    this.createDecorations();
    this.createBoundaryWalls();
    
    // Add to scene / 添加到场景
    this.scene.add(this.battlefieldGroup);
  }

  /**
   * Create the main elliptical grass platform
   * 创建主椭圆形草地平台
   * 
   * Uses BufferGeometry for optimized performance.
   * Uses MeshStandardMaterial with gradient from center to edge.
   * 
   * 使用 BufferGeometry 优化性能。
   * 使用 MeshStandardMaterial，从中心到边缘渐变。
   */
  private createGroundPlatform(): void {
    // Create elliptical shape / 创建椭圆形
    const shape = new THREE.Shape();
    const segments = 64;
    const radiusX = this.FIELD_WIDTH / 2;
    const radiusZ = this.FIELD_DEPTH / 2;
    
    for (let i = 0; i <= segments; i++) {
      const theta = (i / segments) * Math.PI * 2;
      const x = radiusX * Math.cos(theta);
      const z = radiusZ * Math.sin(theta);
      
      if (i === 0) {
        shape.moveTo(x, z);
      } else {
        shape.lineTo(x, z);
      }
    }
    
    // Extrude to create platform / 挤出创建平台
    const extrudeSettings = {
      depth: this.PLATFORM_HEIGHT,
      bevelEnabled: true,
      bevelThickness: 0.05,
      bevelSize: 0.05,
      bevelSegments: 3
    };
    
    const geometry = new THREE.ExtrudeGeometry(shape, extrudeSettings);
    geometry.rotateX(-Math.PI / 2); // Lay flat / 平放
    geometry.translate(0, -this.PLATFORM_HEIGHT / 2, 0);
    this.geometries.push(geometry);
    
    // Create gradient texture for grass effect / 创建渐变纹理实现草地效果
    const canvas = document.createElement('canvas');
    canvas.width = 512;
    canvas.height = 512;
    const ctx = canvas.getContext('2d')!;
    
    // Radial gradient: center bright green to edge dark green
    // 径向渐变：中心亮绿到边缘深绿
    const gradient = ctx.createRadialGradient(256, 256, 0, 256, 256, 256);
    gradient.addColorStop(0, '#4ade80');   // Bright green / 亮绿
    gradient.addColorStop(0.5, '#22c55e'); // Medium green / 中绿
    gradient.addColorStop(1, '#15803d');   // Dark green / 深绿
    
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, 512, 512);
    
    // Add some texture noise for realism / 添加纹理噪声增加真实感
    for (let i = 0; i < 5000; i++) {
      const x = Math.random() * 512;
      const y = Math.random() * 512;
      const radius = Math.random() * 3 + 1;
      ctx.fillStyle = `rgba(${30 + Math.random() * 40}, ${100 + Math.random() * 60}, ${20 + Math.random() * 30}, 0.3)`;
      ctx.beginPath();
      ctx.arc(x, y, radius, 0, Math.PI * 2);
      ctx.fill();
    }
    
    const texture = new THREE.CanvasTexture(canvas);
    texture.wrapS = THREE.RepeatWrapping;
    texture.wrapT = THREE.RepeatWrapping;
    this.textures.push(texture);
    
    const material = new THREE.MeshStandardMaterial({
      map: texture,
      roughness: 0.8,
      metalness: 0.1
    });
    this.materials.push(material);
    
    const platform = new THREE.Mesh(geometry, material);
    platform.receiveShadow = true;
    platform.name = 'GroundPlatform';
    this.battlefieldGroup.add(platform);
    
    // Add divider line between player and opponent sides
    // 添加玩家侧和对手侧之间的分割线
    this.createCenterDivider();
  }

  /**
   * Create center divider line between the two sides
   * 创建两侧之间的中心分割线
   */
  private createCenterDivider(): void {
    const points = [
      new THREE.Vector3(-this.FIELD_WIDTH / 2 + 2, 0.02, 0),
      new THREE.Vector3(this.FIELD_WIDTH / 2 - 2, 0.02, 0)
    ];
    
    const lineGeometry = new THREE.BufferGeometry().setFromPoints(points);
    const lineMaterial = new THREE.LineBasicMaterial({ 
      color: 0xffffff,
      linewidth: 2,
      transparent: true,
      opacity: 0.6
    });
    
    const divider = new THREE.Line(lineGeometry, lineMaterial);
    divider.name = 'CenterDivider';
    this.battlefieldGroup.add(divider);
    
    // Add subtle glow effect / 添加微妙的光晕效果
    const glowGeometry = new THREE.PlaneGeometry(this.FIELD_WIDTH - 4, 0.3);
    const glowMaterial = new THREE.MeshBasicMaterial({
      color: 0xffffff,
      transparent: true,
      opacity: 0.15,
      side: THREE.DoubleSide
    });
    
    const glow = new THREE.Mesh(glowGeometry, glowMaterial);
    glow.rotation.x = -Math.PI / 2;
    glow.position.set(0, 0.01, 0);
    glow.name = 'CenterDividerGlow';
    this.battlefieldGroup.add(glow);
  }

  /**
   * Create position markers for Pokémon placement
   * 创建宝可梦站立位置标记
   * 
   * Each side has 2 slots. Markers are semi-transparent circles.
   * 每侧有 2 个槽位。标记为半透明圆形。
   */
  private createPositionMarkers(): void {
    const sides: Side[] = ['player', 'opponent'];
    
    sides.forEach(side => {
      this.slotConfigs[side].forEach((config, slotIndex) => {
        const marker = this.createSingleMarker(side, slotIndex as SlotIndex, config);
        this.positionMarkers.set(`${side}-${slotIndex}`, marker);
        this.battlefieldGroup.add(marker);
      });
    });
  }

  /**
   * Create a single position marker
   * 创建单个位置标记
   * 
   * @param side - Which side (player/opponent) / 哪一侧
   * @param slot - Slot index (0 or 1) / 槽位索引
   * @param config - Position configuration / 位置配置
   * @returns The marker mesh / 标记网格
   */
  private createSingleMarker(side: Side, slot: SlotIndex, config: SlotConfig): THREE.Mesh {
    const geometry = new THREE.RingGeometry(
      this.MARKER_RADIUS * 0.7,
      this.MARKER_RADIUS,
      32
    );
    geometry.rotateX(-Math.PI / 2);
    this.geometries.push(geometry);
    
    // Different colors for player vs opponent / 玩家和对手使用不同颜色
    const baseColor = side === 'player' ? 0x3b82f6 : 0xef4444; // Blue for player, red for opponent / 玩家蓝色，对手红色
    const hoverColor = side === 'player' ? 0x60a5fa : 0xf87171;
    
    const material = new THREE.MeshStandardMaterial({
      color: baseColor,
      transparent: true,
      opacity: 0.4,
      roughness: 0.5,
      metalness: 0.3,
      emissive: baseColor,
      emissiveIntensity: 0.2
    });
    this.materials.push(material);
    
    const marker = new THREE.Mesh(geometry, material);
    marker.position.set(config.x, 0.02, config.z);
    marker.receiveShadow = true;
    marker.name = `Marker-${side}-${slot}`;
    
    // Store original color for highlight toggle / 存储原始颜色用于高亮切换
    marker.userData = {
      side,
      slot,
      baseColor,
      hoverColor,
      isHighlighted: false
    };
    
    // Add inner circle for visual depth / 添加内圆增加视觉深度
    const innerGeometry = new THREE.CircleGeometry(this.MARKER_RADIUS * 0.5, 32);
    innerGeometry.rotateX(-Math.PI / 2);
    this.geometries.push(innerGeometry);
    
    const innerMaterial = new THREE.MeshStandardMaterial({
      color: baseColor,
      transparent: true,
      opacity: 0.2,
      roughness: 0.7,
      metalness: 0.2
    });
    this.materials.push(innerMaterial);
    
    const innerCircle = new THREE.Mesh(innerGeometry, innerMaterial);
    innerCircle.position.set(config.x, 0.015, config.z);
    innerCircle.receiveShadow = true;
    innerCircle.name = `MarkerInner-${side}-${slot}`;
    marker.add(innerCircle);
    
    // Add slot number label indicator / 添加槽位编号指示
    const dotGeometry = new THREE.SphereGeometry(0.15, 16, 16);
    const dotMaterial = new THREE.MeshStandardMaterial({
      color: 0xffffff,
      emissive: 0xffffff,
      emissiveIntensity: 0.5
    });
    
    const dot = new THREE.Mesh(dotGeometry, dotMaterial);
    dot.position.set(0, 0.1, 0);
    dot.name = `MarkerDot-${side}-${slot}`;
    marker.add(dot);
    
    return marker;
  }

  /**
   * Create decorative elements (flowers, rocks, grass tufts)
   * 创建装饰物（花草、岩石、草丛）
   * 
   * Uses simple geometries for performance optimization.
   * 使用简单几何体优化性能。
   */
  private createDecorations(): void {
    const decorationsGroup = new THREE.Group();
    decorationsGroup.name = 'Decorations';
    
    // Create flowers around the battlefield / 在战场周围创建花朵
    this.createFlowers(decorationsGroup);
    
    // Create rocks at strategic positions / 在关键位置创建岩石
    this.createRocks(decorationsGroup);
    
    // Create grass tufts / 创建草丛
    this.createGrassTufts(decorationsGroup);
    
    this.battlefieldGroup.add(decorationsGroup);
  }

  /**
   * Create decorative flowers
   * 创建装饰花朵
   * 
   * @param group - Parent group to add flowers to / 要添加花朵的父组
   */
  private createFlowers(group: THREE.Group): void {
    const flowerPositions = [
      { x: -8, z: -5, color: 0xff69b4 }, // Pink / 粉色
      { x: 7, z: -6, color: 0xffa500 },  // Orange / 橙色
      { x: -6, z: 5, color: 0xffff00 },  // Yellow / 黄色
      { x: 8, z: 4, color: 0xff69b4 },   // Pink / 粉色
      { x: -4, z: -6, color: 0xff4500 }, // Red-orange / 红橙色
      { x: 5, z: 6, color: 0xffff00 },   // Yellow / 黄色
    ];
    
    flowerPositions.forEach((pos, index) => {
      const flower = this.createSingleFlower(pos.color);
      flower.position.set(pos.x, 0, pos.z);
      flower.scale.setScalar(0.3 + Math.random() * 0.2);
      flower.name = `Flower-${index}`;
      group.add(flower);
    });
  }

  /**
   * Create a single flower mesh
   * 创建单个花朵网格
   * 
   * @param petalColor - Color of flower petals / 花瓣颜色
   * @returns Flower group / 花朵组
   */
  private createSingleFlower(petalColor: number): THREE.Group {
    const flowerGroup = new THREE.Group();
    
    // Stem / 茎
    const stemGeometry = new THREE.CylinderGeometry(0.05, 0.05, 0.8, 8);
    const stemMaterial = new THREE.MeshStandardMaterial({
      color: 0x228b22,
      roughness: 0.8
    });
    
    const stem = new THREE.Mesh(stemGeometry, stemMaterial);
    stem.position.y = 0.4;
    stem.castShadow = true;
    this.geometries.push(stemGeometry);
    this.materials.push(stemMaterial);
    flowerGroup.add(stem);
    
    // Petals / 花瓣
    const petalCount = 5;
    for (let i = 0; i < petalCount; i++) {
      const angle = (i / petalCount) * Math.PI * 2;
      const petalGeometry = new THREE.SphereGeometry(0.15, 8, 8);
      const petalMaterial = new THREE.MeshStandardMaterial({
        color: petalColor,
        roughness: 0.6,
        emissive: petalColor,
        emissiveIntensity: 0.1
      });
      
      const petal = new THREE.Mesh(petalGeometry, petalMaterial);
      petal.position.set(
        Math.cos(angle) * 0.15,
        0.8,
        Math.sin(angle) * 0.15
      );
      petal.scale.set(1, 0.5, 1);
      petal.castShadow = true;
      this.geometries.push(petalGeometry);
      this.materials.push(petalMaterial);
      flowerGroup.add(petal);
    }
    
    // Center / 花心
    const centerGeometry = new THREE.SphereGeometry(0.1, 8, 8);
    const centerMaterial = new THREE.MeshStandardMaterial({
      color: 0xffd700,
      emissive: 0xffd700,
      emissiveIntensity: 0.3
    });
    
    const center = new THREE.Mesh(centerGeometry, centerMaterial);
    center.position.y = 0.8;
    center.castShadow = true;
    this.geometries.push(centerGeometry);
    this.materials.push(centerMaterial);
    flowerGroup.add(center);
    
    return flowerGroup;
  }

  /**
   * Create decorative rocks
   * 创建装饰岩石
   * 
   * @param group - Parent group to add rocks to / 要添加岩石的父组
   */
  private createRocks(group: THREE.Group): void {
    const rockPositions = [
      { x: -9, z: 0, scale: 0.6 },
      { x: 9, z: 0, scale: 0.7 },
      { x: -3, z: -6.5, scale: 0.5 },
      { x: 4, z: 6.5, scale: 0.55 },
      { x: 0, z: -7, scale: 0.4 },
      { x: -7, z: 3, scale: 0.45 },
    ];
    
    rockPositions.forEach((pos, index) => {
      const rock = this.createSingleRock();
      rock.position.set(pos.x, 0, pos.z);
      rock.scale.setScalar(pos.scale);
      rock.rotation.y = Math.random() * Math.PI * 2;
      rock.name = `Rock-${index}`;
      group.add(rock);
    });
  }

  /**
   * Create a single rock mesh using dodecahedron geometry
   * 使用十二面体几何体创建单个岩石网格
   * 
   * @returns Rock mesh / 岩石网格
   */
  private createSingleRock(): THREE.Mesh {
    const geometry = new THREE.DodecahedronGeometry(1, 1);
    
    // Add some randomness to vertices for natural look / 添加随机顶点增加自然感
    const positions = geometry.attributes.position;
    for (let i = 0; i < positions.count; i++) {
      const x = positions.getX(i);
      const y = positions.getY(i);
      const z = positions.getZ(i);
      
      positions.setXYZ(
        i,
        x + (Math.random() - 0.5) * 0.2,
        y + (Math.random() - 0.5) * 0.2,
        z + (Math.random() - 0.5) * 0.2
      );
    }
    
    geometry.computeVertexNormals();
    this.geometries.push(geometry);
    
    const material = new THREE.MeshStandardMaterial({
      color: 0x808080,
      roughness: 0.9,
      metalness: 0.1,
      flatShading: true
    });
    this.materials.push(material);
    
    const rock = new THREE.Mesh(geometry, material);
    rock.castShadow = true;
    rock.receiveShadow = true;
    
    return rock;
  }

  /**
   * Create grass tufts for decoration
   * 创建草丛装饰
   * 
   * @param group - Parent group to add grass to / 要添加草丛的父组
   */
  private createGrassTufts(group: THREE.Group): void {
    const grassPositions = [
      { x: -7, z: -4 },
      { x: 6, z: -5 },
      { x: -5, z: 6 },
      { x: 7, z: 5 },
      { x: 0, z: 7 },
      { x: -8, z: 1 },
      { x: 8, z: -2 },
    ];
    
    grassPositions.forEach((pos, index) => {
      const tuft = this.createGrassTuft();
      tuft.position.set(pos.x, 0, pos.z);
      tuft.name = `GrassTuft-${index}`;
      group.add(tuft);
    });
  }

  /**
   * Create a single grass tuft using plane geometries
   * 使用平面几何体创建单个草丛
   * 
   * @returns Grass tuft group / 草丛组
   */
  private createGrassTuft(): THREE.Group {
    const tuftGroup = new THREE.Group();
    
    const bladeCount = 5;
    const grassMaterial = new THREE.MeshStandardMaterial({
      color: 0x2d8a4e,
      roughness: 0.9,
      side: THREE.DoubleSide
    });
    this.materials.push(grassMaterial);
    
    for (let i = 0; i < bladeCount; i++) {
      const height = 0.3 + Math.random() * 0.4;
      const width = 0.1 + Math.random() * 0.1;
      
      const geometry = new THREE.PlaneGeometry(width, height);
      const blade = new THREE.Mesh(geometry, grassMaterial);
      
      blade.position.set(
        (Math.random() - 0.5) * 0.3,
        height / 2,
        (Math.random() - 0.5) * 0.3
      );
      
      blade.rotation.y = Math.random() * Math.PI;
      blade.rotation.x = (Math.random() - 0.5) * 0.3;
      blade.castShadow = true;
      this.geometries.push(geometry);
      
      tuftGroup.add(blade);
    }
    
    return tuftGroup;
  }

  /**
   * Create boundary walls around the battlefield
   * 创建战场周围的边界围墙
   * 
   * Semi-transparent walls to define the battle area.
   * 半透明围墙定义战斗区域。
   */
  private createBoundaryWalls(): void {
    const wallMaterial = new THREE.MeshStandardMaterial({
      color: 0x4a90d9,
      transparent: true,
      opacity: 0.15,
      roughness: 0.3,
      metalness: 0.5,
      side: THREE.DoubleSide
    });
    this.materials.push(wallMaterial);
    
    // Create elliptical wall using segments / 使用分段创建椭圆形围墙
    const segments = 32;
    const radiusX = this.FIELD_WIDTH / 2 + 0.5;
    const radiusZ = this.FIELD_DEPTH / 2 + 0.5;
    
    for (let i = 0; i < segments; i++) {
      const theta1 = (i / segments) * Math.PI * 2;
      const theta2 = ((i + 1) / segments) * Math.PI * 2;
      
      const x1 = radiusX * Math.cos(theta1);
      const z1 = radiusZ * Math.sin(theta1);
      const x2 = radiusX * Math.cos(theta2);
      const z2 = radiusZ * Math.sin(theta2);
      
      // Calculate wall segment position and rotation
      // 计算墙段位置和旋转
      const midX = (x1 + x2) / 2;
      const midZ = (z1 + z2) / 2;
      const length = Math.sqrt((x2 - x1) ** 2 + (z2 - z1) ** 2);
      
      const wallGeometry = new THREE.PlaneGeometry(length, this.WALL_HEIGHT);
      const wall = new THREE.Mesh(wallGeometry, wallMaterial);
      
      wall.position.set(midX, this.WALL_HEIGHT / 2, midZ);
      wall.lookAt(0, this.WALL_HEIGHT / 2, 0);
      wall.name = `WallSegment-${i}`;
      this.geometries.push(wallGeometry);
      
      this.battlefieldGroup.add(wall);
    }
    
    // Add corner pillars for visual appeal / 添加角柱增加视觉效果
    this.createCornerPillars();
  }

  /**
   * Create decorative corner pillars
   * 创建装饰角柱
   */
  private createCornerPillars(): void {
    const pillarPositions = [
      { x: -this.FIELD_WIDTH / 2, z: -this.FIELD_DEPTH / 2 },
      { x: this.FIELD_WIDTH / 2, z: -this.FIELD_DEPTH / 2 },
      { x: -this.FIELD_WIDTH / 2, z: this.FIELD_DEPTH / 2 },
      { x: this.FIELD_WIDTH / 2, z: this.FIELD_DEPTH / 2 },
    ];
    
    const pillarMaterial = new THREE.MeshStandardMaterial({
      color: 0x4a90d9,
      transparent: true,
      opacity: 0.3,
      roughness: 0.4,
      metalness: 0.6
    });
    this.materials.push(pillarMaterial);
    
    pillarPositions.forEach((pos, index) => {
      const geometry = new THREE.CylinderGeometry(0.3, 0.3, this.WALL_HEIGHT + 0.3, 8);
      const pillar = new THREE.Mesh(geometry, pillarMaterial);
      
      pillar.position.set(pos.x, (this.WALL_HEIGHT + 0.3) / 2, pos.z);
      pillar.castShadow = true;
      pillar.name = `Pillar-${index}`;
      this.geometries.push(geometry);
      
      // Add sphere on top / 添加顶部球体
      const sphereGeometry = new THREE.SphereGeometry(0.2, 16, 16);
      const sphereMaterial = new THREE.MeshStandardMaterial({
        color: 0x60a5fa,
        emissive: 0x60a5fa,
        emissiveIntensity: 0.3
      });
      
      const sphere = new THREE.Mesh(sphereGeometry, sphereMaterial);
      sphere.position.y = (this.WALL_HEIGHT + 0.3) / 2 + 0.2;
      sphere.castShadow = true;
      this.geometries.push(sphereGeometry);
      this.materials.push(sphereMaterial);
      pillar.add(sphere);
      
      this.battlefieldGroup.add(pillar);
    });
  }

  /**
   * Set highlight state for a position marker
   * 设置位置标记的高亮状态
   * 
   * @param side - Which side (player/opponent) / 哪一侧
   * @param slot - Slot index (0 or 1) / 槽位索引
   * @param highlighted - Whether to highlight / 是否高亮
   * 
   * @example
   * ```typescript
   * // Highlight player's first slot
   * battlefield.setPositionMarker('player', 0, true);
   * 
   * // Remove highlight
   * battlefield.setPositionMarker('player', 0, false);
   * ```
   */
  public setPositionMarker(side: Side, slot: SlotIndex, highlighted: boolean): void {
    const key = `${side}-${slot}`;
    const marker = this.positionMarkers.get(key);
    
    if (!marker) {
      console.warn(`Position marker not found: ${key}`);
      return;
    }
    
    const material = marker.material as THREE.MeshStandardMaterial;
    const userData = marker.userData;
    
    if (highlighted) {
      material.color.setHex(userData.hoverColor);
      material.emissive.setHex(userData.hoverColor);
      material.emissiveIntensity = 0.5;
      material.opacity = 0.7;
      userData.isHighlighted = true;
      
      // Scale up slightly when highlighted / 高亮时稍微放大
      marker.scale.setScalar(1.1);
    } else {
      material.color.setHex(userData.baseColor);
      material.emissive.setHex(userData.baseColor);
      material.emissiveIntensity = 0.2;
      material.opacity = 0.4;
      userData.isHighlighted = false;
      
      // Reset scale / 重置缩放
      marker.scale.setScalar(1.0);
    }
  }

  /**
   * Get world position for a specific slot
   * 获取特定槽位的世界坐标
   * 
   * @param side - Which side (player/opponent) / 哪一侧
   * @param slot - Slot index (0 or 1) / 槽位索引
   * @returns World position as Vector3 / 世界坐标 Vector3
   * 
   * @example
   * ```typescript
   * const position = battlefield.getSlotWorldPosition('player', 0);
   * pokemon.position.copy(position);
   * ```
   */
  public getSlotWorldPosition(side: Side, slot: SlotIndex): THREE.Vector3 {
    const config = this.slotConfigs[side][slot];
    const worldPos = new THREE.Vector3(config.x, 0, config.z);
    
    // Apply battlefield group transformations if any
    // 如果战场组有变换则应用
    this.battlefieldGroup.localToWorld(worldPos);
    
    return worldPos;
  }

  /**
   * Get all slot positions for a side
   * 获取一侧的所有槽位位置
   * 
   * @param side - Which side / 哪一侧
   * @returns Array of world positions / 世界坐标数组
   */
  public getSidePositions(side: Side): THREE.Vector3[] {
    return [
      this.getSlotWorldPosition(side, 0),
      this.getSlotWorldPosition(side, 1)
    ];
  }

  /**
   * Check if a position marker is highlighted
   * 检查位置标记是否高亮
   * 
   * @param side - Which side / 哪一侧
   * @param slot - Slot index / 槽位索引
   * @returns Whether the marker is highlighted / 是否高亮
   */
  public isMarkerHighlighted(side: Side, slot: SlotIndex): boolean {
    const key = `${side}-${slot}`;
    const marker = this.positionMarkers.get(key);
    return marker?.userData.isHighlighted ?? false;
  }

  /**
   * Reset all markers to default state
   * 重置所有标记为默认状态
   */
  public resetAllMarkers(): void {
    const sides: Side[] = ['player', 'opponent'];
    sides.forEach(side => {
      [0, 1].forEach(slot => {
        this.setPositionMarker(side, slot as SlotIndex, false);
      });
    });
  }

  /**
   * Get the battlefield group
   * 获取战场组
   * 
   * @returns The battlefield THREE.Group / 战场 THREE.Group
   */
  public getGroup(): THREE.Group {
    return this.battlefieldGroup;
  }

  /**
   * Animate decorations (optional visual enhancement)
   * 动画装饰物（可选视觉增强）
   * 
   * Call this in the animation loop for subtle movement.
   * 在动画循环中调用此方法实现微妙的运动效果。
   * 
   * @param time - Current time in seconds / 当前时间（秒）
   */
  public animate(time: number): void {
    // Subtle flower swaying / 花朵轻微摇摆
    const flowers = this.battlefieldGroup.getObjectByName('Decorations');
    if (flowers) {
      flowers.children.forEach((child, index) => {
        if (child.name.startsWith('Flower')) {
          child.rotation.z = Math.sin(time * 2 + index) * 0.05;
        }
      });
    }
    
    // Pulse highlighted markers / 高亮标记脉冲效果
    this.positionMarkers.forEach((marker) => {
      if (marker.userData.isHighlighted) {
        const material = marker.material as THREE.MeshStandardMaterial;
        material.emissiveIntensity = 0.3 + Math.sin(time * 3) * 0.2;
      }
    });
  }

  /**
   * Dispose all resources to prevent memory leaks
   * 清理所有资源防止内存泄漏
   * 
   * Call this when the battlefield is no longer needed.
   * 当战场不再需要时调用此方法。
   * 
   * @example
   * ```typescript
   * // When leaving battle scene
   * battlefield.dispose();
   * scene.remove(battlefield.getGroup());
   * ```
   */
  public dispose(): void {
    // Dispose geometries / 清理几何体
    this.geometries.forEach(geometry => {
      geometry.dispose();
    });
    this.geometries = [];
    
    // Dispose materials / 清理材质
    this.materials.forEach(material => {
      material.dispose();
    });
    this.materials = [];
    
    // Dispose textures / 清理纹理
    this.textures.forEach(texture => {
      texture.dispose();
    });
    this.textures = [];
    
    // Remove from scene / 从场景移除
    this.scene.remove(this.battlefieldGroup);
    
    // Clear marker references / 清除标记引用
    this.positionMarkers.clear();
    
    console.log('Battlefield resources disposed');
  }
}
