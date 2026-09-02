/**
 * CombatSystem - 战斗系统
 *
 * 处理 3D 战斗的实际逻辑：队伍管理、伤害计算、回合结算、胜负判定。
 * Handles core battle logic: team management, damage calculation, turn resolution, and win/lose checks.
 *
 * @module CombatSystem
 */

// ============================================================
// Types / 类型定义
// ============================================================

/**
 * 状态效果类型
 * Status condition types.
 */
export type StatusCondition = 'burn' | 'poison' | 'paralysis' | 'sleep' | null;

/**
 * 属性类型
 * Pokémon type.
 */
export type PokemonType =
  | 'normal' | 'fire' | 'water' | 'grass' | 'electric'
  | 'ice' | 'fighting' | 'poison' | 'ground' | 'flying'
  | 'psychic' | 'bug' | 'rock' | 'ghost' | 'dragon'
  | 'dark' | 'steel' | 'fairy'
  | string; // 允许扩展属性 / Allow extended types

/**
 * 招式数据
 * Move data.
 */
export interface MoveData {
  /** 招式名称（中文） / Move name (Chinese) */
  name: string;
  /** 招式英文名 / Move name (English) */
  name_en?: string;
  /** 招式威力 / Move power */
  power: number;
  /** 招式属性 / Move type */
  type: PokemonType;
  /** 命中率（0-100） / Accuracy (0-100) */
  accuracy?: number;
  /** PP 剩余 / Remaining PP */
  pp?: number;
  /** 是否特殊招式（特攻） / Whether it's a special move */
  isSpecial?: boolean;
}

/**
 * 宝可梦数据
 * Pokémon data.
 */
export interface PokemonData {
  /** 宝可梦名称（中文） / Name (Chinese) */
  name: string;
  /** 宝可梦英文名 / Name (English) */
  name_en?: string;
  /** 等级 / Level */
  level: number;
  /** 当前 HP / Current HP */
  currentHp: number;
  /** 最大 HP / Max HP */
  maxHp: number;
  /** 种族值及能力值 / Base stats & computed stats */
  stats: {
    hp: number;
    attack: number;
    defense: number;
    spAttack: number;
    spDefense: number;
    speed: number;
  };
  /** 属性列表 / Type(s) */
  types: PokemonType[];
  /** 招式列表 / Moves */
  moves: MoveData[];
  /** 状态效果 / Status condition */
  status: StatusCondition;
  /** 是否倒下 / Whether fainted */
  fainted: boolean;
  /** 队伍索引 / Team index */
  teamIndex: number;
}

/**
 * 战斗方（玩家 / 对手）
 * Battle side (player / opponent).
 */
export type BattleSide = 'player' | 'opponent';

/**
 * 战斗结果
 * Battle result.
 */
export type BattleResult = null | 'player' | 'opponent';

/**
 * 战斗状态快照
 * Battle state snapshot.
 */
export interface BattleStateSnapshot {
  /** 战斗状态 / Battle status */
  status: 'idle' | 'preview' | 'running' | 'completed';
  /** 当前回合 / Current round */
  currentRound: number;
  /** 玩家队伍 / Player team */
  playerTeam: PokemonData[];
  /** 对手队伍 / Opponent team */
  opponentTeam: PokemonData[];
  /** 玩家场上位置（队伍索引） / Player active slots (team indices) */
  playerActiveSlots: number[];
  /** 对手场上位置（队伍索引） / Opponent active slots (team indices) */
  opponentActiveSlots: number[];
  /** 战斗结果 / Battle result */
  result: BattleResult;
  /** 事件日志 / Event log */
  events: CombatEvent[];
}

/**
 * 战斗事件
 * Combat event (for logging / replay).
 */
export interface CombatEvent {
  /** 事件类型 / Event type */
  type:
    | 'attack'
    | 'damage'
    | 'heal'
    | 'status_apply'
    | 'status_damage'
    | 'faint'
    | 'switch'
    | 'turn_start'
    | 'turn_end'
    | 'battle_end'
    | 'info';
  /** 源方 / Source side */
  sourceSide?: BattleSide;
  /** 源宝可梦队伍索引 / Source team index */
  sourceIndex?: number;
  /** 目标方 / Target side */
  targetSide?: BattleSide;
  /** 目标宝可梦队伍索引 / Target team index */
  targetIndex?: number;
  /** 额外数据 / Additional data */
  data?: Record<string, any>;
  /** 消息（中文） / Message (Chinese) */
  message?: string;
  /** 时间戳 / Timestamp */
  timestamp: number;
}

/**
 * 后端 payload 数据结构（兼容后端 summary）
 * Backend payload structure (compatible with backend summary).
 */
export interface BattlePayload {
  status: 'preview' | 'running' | 'completed';
  phase?: 'team-preview' | 'action' | 'replacement';
  winner?: 'player' | 'opponent';
  currentRound?: number;
  playerTeam?: any[];
  opponentTeam?: any[];
  playerActiveSlots?: number[];
  opponentActiveSlots?: number[];
  rounds?: { events: any[] }[];
}

// ============================================================
// Constants / 常量
// ============================================================

/**
 * 属性克制表（简化版）
 * Type effectiveness chart (simplified).
 *
 * 值: 0 = 无效, 0.5 = 抵抗, 1 = 一般, 2 = 效果拔群
 * Values: 0 = immune, 0.5 = resist, 1 = neutral, 2 = super effective
 */
const TYPE_CHART: Record<string, Record<string, number>> = {
  normal:   { rock: 0.5, ghost: 0, steel: 0.5 },
  fire:     { fire: 0.5, water: 0.5, grass: 2, ice: 2, bug: 2, rock: 0.5, dragon: 0.5, steel: 2 },
  water:    { fire: 2, water: 0.5, grass: 0.5, ground: 2, rock: 2, dragon: 0.5 },
  grass:    { fire: 0.5, water: 2, grass: 0.5, poison: 0.5, ground: 2, flying: 0.5, bug: 0.5, rock: 2, dragon: 0.5, steel: 0.5 },
  electric: { water: 2, grass: 0.5, electric: 0.5, ground: 0, flying: 2, dragon: 0.5 },
  ice:      { fire: 0.5, water: 0.5, grass: 2, ice: 0.5, ground: 2, flying: 2, dragon: 2, steel: 0.5 },
  fighting: { normal: 2, ice: 2, poison: 0.5, flying: 0.5, psychic: 0.5, bug: 0.5, rock: 2, ghost: 0, dark: 2, steel: 2, fairy: 0.5 },
  poison:   { grass: 2, poison: 0.5, ground: 0.5, rock: 0.5, ghost: 0.5, steel: 0, fairy: 2 },
  ground:   { fire: 2, electric: 2, grass: 0.5, poison: 2, flying: 0, bug: 0.5, rock: 2, steel: 2 },
  flying:   { electric: 0.5, grass: 2, fighting: 2, bug: 2, rock: 0.5, steel: 0.5 },
  psychic:  { fighting: 2, poison: 2, psychic: 0.5, dark: 0, steel: 0.5 },
  bug:      { fire: 0.5, grass: 2, fighting: 0.5, poison: 0.5, flying: 0.5, psychic: 2, ghost: 0.5, dark: 2, steel: 0.5, fairy: 0.5 },
  rock:     { fire: 2, ice: 2, fighting: 0.5, ground: 0.5, flying: 2, bug: 2, steel: 0.5 },
  ghost:    { normal: 0, psychic: 2, ghost: 2, dark: 0.5 },
  dragon:   { dragon: 2, steel: 0.5, fairy: 0 },
  dark:     { fighting: 0.5, psychic: 2, ghost: 2, dark: 0.5, fairy: 0.5 },
  steel:    { fire: 0.5, water: 0.5, electric: 0.5, ice: 2, rock: 2, steel: 0.5, fairy: 2 },
  fairy:    { fire: 0.5, fighting: 2, poison: 0.5, dragon: 2, dark: 2, steel: 0.5 },
};

/**
 * 状态效果每回合伤害（占最大 HP 的比例）
 * Status damage per turn as fraction of max HP.
 */
const STATUS_DAMAGE_RATIO: Record<string, number> = {
  burn: 1 / 16,
  poison: 1 / 8,
};

/**
 * 随机伤害浮动范围
 * Random damage multiplier range.
 */
const RANDOM_MIN = 0.85;
const RANDOM_MAX = 1.0;

// ============================================================
// Utility functions / 工具函数
// ============================================================

/**
 * 生成 [min, max) 范围内的随机数
 * Generate a random number in [min, max).
 */
function randomInRange(min: number, max: number): number {
  return min + Math.random() * (max - min);
}

/**
 * 计算属性相性倍率
 * Calculate type effectiveness multiplier.
 *
 * @param moveType - 招式属性 / Move type
 * @param defenderTypes - 防御方属性列表 / Defender's types
 * @returns 倍率 / Multiplier
 */
function getTypeEffectiveness(moveType: PokemonType, defenderTypes: PokemonType[]): number {
  let multiplier = 1;
  for (const defType of defenderTypes) {
    const chart = TYPE_CHART[moveType as string];
    if (chart && defType in chart) {
      multiplier *= chart[defType as string];
    }
  }
  return multiplier;
}

/**
 * 计算 STAB（属性一致加成）
 * Calculate Same-Type Attack Bonus (STAB).
 *
 * @param attackerTypes - 攻击方属性 / Attacker's types
 * @param moveType - 招式属性 / Move type
 * @returns STAB 倍率 / STAB multiplier
 */
function getSTAB(attackerTypes: PokemonType[], moveType: PokemonType): number {
  return attackerTypes.includes(moveType) ? 1.5 : 1;
}

/**
 * 从后端数据解析宝可梦
 * Parse a Pokémon object from backend data.
 */
function parsePokemonFromBackend(raw: any, index: number): PokemonData {
  const stats = raw.stats ?? {};
  const hpStat = stats.hp ?? raw.maxHp ?? 100;
  return {
    name: raw.name ?? `宝可梦${index + 1}`,
    name_en: raw.name_en ?? '',
    level: raw.level ?? 50,
    currentHp: raw.currentHp ?? hpStat,
    maxHp: hpStat,
    stats: {
      hp: hpStat,
      attack: stats.attack ?? stats.atk ?? 80,
      defense: stats.defense ?? stats.def ?? 80,
      spAttack: stats.spAttack ?? stats.spa ?? 80,
      spDefense: stats.spDefense ?? stats.spd ?? 80,
      speed: stats.speed ?? stats.spe ?? 80,
    },
    types: raw.types ?? ['normal'],
    moves: (raw.moves ?? []).map((m: any) => ({
      name: m.name ?? '',
      name_en: m.name_en ?? '',
      power: m.power ?? 40,
      type: m.type ?? 'normal',
      accuracy: m.accuracy ?? 100,
      pp: m.pp,
      isSpecial: m.isSpecial ?? false,
    })),
    status: raw.status ?? null,
    fainted: raw.fainted ?? (raw.currentHp ?? hpStat) <= 0,
    teamIndex: index,
  };
}

// ============================================================
// CombatSystem
// ============================================================

/**
 * 战斗系统
 * Combat system.
 *
 * 管理玩家和对手的队伍数据，处理招式选择与执行、伤害计算、状态效果、胜负判定。
 * Manages player/opponent teams, handles move selection & execution,
 * damage calculation, status effects, and win/lose determination.
 *
 * @example
 * ```ts
 * const combat = new CombatSystem();
 * combat.applyBattlePayload(backendPayload);
 *
 * // 获取场上宝可梦
 * const [p1] = combat.getActivePokemon('player');
 * const [o1] = combat.getActivePokemon('opponent');
 *
 * // 计算伤害
 * const dmg = combat.calculateDamage(p1, o1, p1.moves[0]);
 * ```
 */
export class CombatSystem {
  /** 玩家队伍 / Player team */
  private _playerTeam: PokemonData[] = [];

  /** 对手队伍 / Opponent team */
  private _opponentTeam: PokemonData[] = [];

  /** 玩家场上槽位（队伍索引） / Player active slots (team indices) */
  private _playerActiveSlots: number[] = [];

  /** 对手场上槽位（队伍索引） / Opponent active slots (team indices) */
  private _opponentActiveSlots: number[] = [];

  /** 当前回合 / Current round */
  private _currentRound: number = 0;

  /** 战斗结果 / Battle result */
  private _result: BattleResult = null;

  /** 战斗状态 / Battle status */
  private _status: 'idle' | 'preview' | 'running' | 'completed' = 'idle';

  /** 事件日志 / Event log */
  private _events: CombatEvent[] = [];

  // ----------------------------------------------------------
  // Getters / 属性访问
  // ----------------------------------------------------------

  /** 玩家队伍 / Player team */
  get playerTeam(): ReadonlyArray<PokemonData> {
    return this._playerTeam;
  }

  /** 对手队伍 / Opponent team */
  get opponentTeam(): ReadonlyArray<PokemonData> {
    return this._opponentTeam;
  }

  /** 当前回合 / Current round */
  get currentRound(): number {
    return this._currentRound;
  }

  /** 战斗结果 / Battle result */
  get result(): BattleResult {
    return this._result;
  }

  /** 战斗状态 / Battle status */
  get status(): string {
    return this._status;
  }

  /** 事件日志 / Event log */
  get events(): ReadonlyArray<CombatEvent> {
    return this._events;
  }

  // ----------------------------------------------------------
  // Data loading / 数据加载
  // ----------------------------------------------------------

  /**
   * 从后端 payload 更新战斗状态
   * Update battle state from a backend payload.
   *
   * 这是与后端 API 对接的主要入口。
   * This is the primary entry point for backend API integration.
   *
   * @param payload - 后端战斗数据 / Backend battle payload
   */
  applyBattlePayload(payload: BattlePayload): void {
    // 更新状态
    if (payload.status) {
      this._status = payload.status;
    }

    // 更新回合
    if (payload.currentRound !== undefined) {
      this._currentRound = payload.currentRound;
    }

    // 更新结果
    if (payload.winner) {
      this._result = payload.winner;
    }

    // 解析队伍
    if (payload.playerTeam) {
      this._playerTeam = payload.playerTeam.map((raw, i) => parsePokemonFromBackend(raw, i));
    }

    if (payload.opponentTeam) {
      this._opponentTeam = payload.opponentTeam.map((raw, i) => parsePokemonFromBackend(raw, i));
    }

    // 更新场上槽位
    if (payload.playerActiveSlots) {
      this._playerActiveSlots = [...payload.playerActiveSlots];
    }

    if (payload.opponentActiveSlots) {
      this._opponentActiveSlots = [...payload.opponentActiveSlots];
    }

    // 解析回合事件
    if (payload.rounds) {
      for (const round of payload.rounds) {
        for (const rawEvent of round.events ?? []) {
          this._events.push({
            type: rawEvent.type ?? 'info',
            sourceSide: rawEvent.sourceSide,
            sourceIndex: rawEvent.sourceIndex,
            targetSide: rawEvent.targetSide,
            targetIndex: rawEvent.targetIndex,
            data: rawEvent.data,
            message: rawEvent.message ?? '',
            timestamp: rawEvent.timestamp ?? Date.now(),
          });
        }
      }
    }
  }

  // ----------------------------------------------------------
  // Team queries / 队伍查询
  // ----------------------------------------------------------

  /**
   * 获取指定方的场上宝可梦
   * Get active Pokémon for a given side.
   *
   * @param side - 'player' | 'opponent'
   * @returns 场上宝可梦数组 / Array of active Pokémon
   */
  getActivePokemon(side: BattleSide): PokemonData[] {
    const team = side === 'player' ? this._playerTeam : this._opponentTeam;
    const slots = side === 'player' ? this._playerActiveSlots : this._opponentActiveSlots;
    return slots
      .map((idx) => team[idx])
      .filter((p): p is PokemonData => p !== undefined && !p.fainted);
  }

  /**
   * 获取指定方的队伍
   * Get team for a given side.
   *
   * @param side - 'player' | 'opponent'
   * @returns 队伍数组 / Team array
   */
  getTeam(side: BattleSide): ReadonlyArray<PokemonData> {
    return side === 'player' ? this._playerTeam : this._opponentTeam;
  }

  /**
   * 获取指定方尚未倒下的宝可梦
   * Get non-fainted Pokémon for a given side.
   *
   * @param side - 'player' | 'opponent'
   * @returns 存活的宝可梦 / Surviving Pokémon
   */
  getAlivePokemon(side: BattleSide): PokemonData[] {
    const team = side === 'player' ? this._playerTeam : this._opponentTeam;
    return team.filter((p) => !p.fainted);
  }

  // ----------------------------------------------------------
  // Damage calculation / 伤害计算
  // ----------------------------------------------------------

  /**
   * 计算伤害
   * Calculate damage.
   *
   * 公式 / Formula:
   * ```
   * damage = ((2 * level / 5 + 2) * power * (atk / def)) / 50 + 2
   *          * STAB * typeEffectiveness * random
   * ```
   *
   * @param attacker - 攻击方 / Attacker
   * @param defender - 防御方 / Defender
   * @param move - 使用的招式 / Move used
   * @returns 计算后的伤害值（整数，最少 1） / Calculated damage (integer, min 1)
   */
  calculateDamage(attacker: PokemonData, defender: PokemonData, move: MoveData): number {
    const level = attacker.level;
    const power = move.power;

    // 选择攻击/防御值
    // Choose attack/defense stats based on whether the move is special.
    const atk = move.isSpecial ? attacker.stats.spAttack : attacker.stats.attack;
    const def = move.isSpecial ? defender.stats.spDefense : defender.stats.defense;

    // 基础伤害 / Base damage
    const baseDamage = ((2 * level / 5 + 2) * power * (atk / def)) / 50 + 2;

    // STAB / 属性一致加成
    const stab = getSTAB(attacker.types, move.type);

    // 属性相性 / Type effectiveness
    const effectiveness = getTypeEffectiveness(move.type, defender.types);

    // 随机浮动 / Random factor
    const random = randomInRange(RANDOM_MIN, RANDOM_MAX);

    // 烧伤惩罚（物理招式） / Burn penalty (physical moves)
    const burnPenalty = attacker.status === 'burn' && !move.isSpecial ? 0.5 : 1;

    // 最终伤害 / Final damage
    const rawDamage = baseDamage * stab * effectiveness * random * burnPenalty;

    return Math.max(1, Math.floor(rawDamage));
  }

  /**
   * 计算属性相性倍率（对外暴露）
   * Get type effectiveness multiplier (public).
   *
   * @param moveType - 招式属性 / Move type
   * @param defenderTypes - 防御方属性 / Defender's types
   * @returns 倍率 / Multiplier
   */
  getTypeEffectiveness(moveType: PokemonType, defenderTypes: PokemonType[]): number {
    return getTypeEffectiveness(moveType, defenderTypes);
  }

  /**
   * 计算 STAB（对外暴露）
   * Get STAB (public).
   *
   * @param attackerTypes - 攻击方属性 / Attacker's types
   * @param moveType - 招式属性 / Move type
   * @returns STAB 倍率 / STAB multiplier
   */
  getSTAB(attackerTypes: PokemonType[], moveType: PokemonType): number {
    return getSTAB(attackerTypes, moveType);
  }

  // ----------------------------------------------------------
  // Battle actions / 战斗行动
  // ----------------------------------------------------------

  /**
   * 对目标造成伤害
   * Apply damage to a target.
   *
   * @param target - 目标宝可梦 / Target Pokémon
   * @param damage - 伤害值 / Damage amount
   * @param sourceSide - 来源方 / Source side
   * @param sourceIndex - 来源队伍索引 / Source team index
   * @returns 实际伤害 & 是否倒下 / Actual damage & whether fainted
   */
  applyDamage(
    target: PokemonData,
    damage: number,
    sourceSide?: BattleSide,
    sourceIndex?: number,
  ): { actualDamage: number; fainted: boolean } {
    const actualDamage = Math.min(damage, target.currentHp);
    target.currentHp -= actualDamage;

    this._addEvent({
      type: 'damage',
      sourceSide,
      sourceIndex,
      targetSide: undefined,
      targetIndex: target.teamIndex,
      data: { damage: actualDamage, remainingHp: target.currentHp, maxHp: target.maxHp },
      message: `${target.name} 受到 ${actualDamage} 点伤害`,
    });

    let fainted = false;
    if (target.currentHp <= 0) {
      target.currentHp = 0;
      target.fainted = true;
      fainted = true;

      this._addEvent({
        type: 'faint',
        targetIndex: target.teamIndex,
        data: { name: target.name, name_en: target.name_en },
        message: `${target.name} 倒下了！`,
      });
    }

    return { actualDamage, fainted };
  }

  /**
   * 治疗宝可梦
   * Heal a Pokémon.
   *
   * @param target - 目标宝可梦 / Target Pokémon
   * @param amount - 治疗量 / Heal amount
   */
  heal(target: PokemonData, amount: number): void {
    const actualHeal = Math.min(amount, target.maxHp - target.currentHp);
    target.currentHp += actualHeal;

    this._addEvent({
      type: 'heal',
      targetIndex: target.teamIndex,
      data: { heal: actualHeal, currentHp: target.currentHp, maxHp: target.maxHp },
      message: `${target.name} 恢复了 ${actualHeal} HP`,
    });
  }

  /**
   * 应用状态效果
   * Apply a status condition.
   *
   * @param target - 目标宝可梦 / Target Pokémon
   * @param status - 状态效果 / Status condition
   * @param sourceSide - 来源方 / Source side
   */
  applyStatus(target: PokemonData, status: StatusCondition, sourceSide?: BattleSide): boolean {
    // 已有状态则不能覆盖
    // Cannot overwrite existing status
    if (target.status !== null || target.fainted) {
      return false;
    }

    target.status = status;

    this._addEvent({
      type: 'status_apply',
      sourceSide,
      targetIndex: target.teamIndex,
      data: { status },
      message: `${target.name} 陷入了${this._statusLabel(status)}状态！`,
    });

    return true;
  }

  /**
   * 处理状态效果的回合伤害（烧伤/中毒）
   * Process end-of-turn status damage (burn/poison).
   *
   * @param pokemon - 宝可梦 / Pokémon
   * @returns 实际伤害 / Actual damage
   */
  processStatusDamage(pokemon: PokemonData): number {
    if (!pokemon.status || pokemon.fainted) return 0;

    const ratio = STATUS_DAMAGE_RATIO[pokemon.status];
    if (!ratio) return 0;

    const damage = Math.max(1, Math.floor(pokemon.maxHp * ratio));
    this.applyDamage(pokemon, damage);

    this._addEvent({
      type: 'status_damage',
      targetIndex: pokemon.teamIndex,
      data: { status: pokemon.status, damage },
      message: `${pokemon.name} 因${this._statusLabel(pokemon.status)}受到了 ${damage} 点伤害`,
    });

    return damage;
  }

  /**
   * 麻痹检查：是否无法行动
   * Paralysis check: whether the Pokémon cannot act.
   *
   * @param pokemon - 宝可梦 / Pokémon
   * @returns 是否被麻痹限制 / Whether paralyzed and unable to act
   */
  checkParalysis(pokemon: PokemonData): boolean {
    if (pokemon.status !== 'paralysis') return false;
    // 25% 概率无法行动 / 25% chance of being fully paralyzed
    return Math.random() < 0.25;
  }

  /**
   * 睡眠检查：是否仍在睡觉
   * Sleep check: whether the Pokémon is still asleep.
   *
   * @param pokemon - 宝可梦 / Pokémon
   * @returns 是否仍在睡觉（无法行动） / Whether still asleep (cannot act)
   */
  checkSleep(pokemon: PokemonData): boolean {
    if (pokemon.status !== 'sleep') return false;
    // 每回合 33% 概率醒来 / 33% chance to wake up each turn
    if (Math.random() < 0.33) {
      pokemon.status = null;
      this._addEvent({
        type: 'status_apply',
        targetIndex: pokemon.teamIndex,
        data: { status: null },
        message: `${pokemon.name} 醒来了！`,
      });
      return false;
    }
    return true;
  }

  /**
   * 换人操作
   * Switch a Pokémon.
   *
   * @param side - 战斗方 / Battle side
   * @param slotIndex - 场上槽位 / Active slot index
   * @param teamIndex - 替换上来的队伍索引 / Incoming team index
   */
  switchPokemon(side: BattleSide, slotIndex: number, teamIndex: number): boolean {
    const team = side === 'player' ? this._playerTeam : this._opponentTeam;
    const slots = side === 'player' ? this._playerActiveSlots : this._opponentActiveSlots;

    const incoming = team[teamIndex];
    if (!incoming || incoming.fainted) return false;

    const outgoingIndex = slots[slotIndex];
    const outgoing = outgoingIndex !== undefined ? team[outgoingIndex] : undefined;

    slots[slotIndex] = teamIndex;

    this._addEvent({
      type: 'switch',
      sourceSide: side,
      sourceIndex: teamIndex,
      data: {
        outgoingIndex,
        outgoingName: outgoing?.name,
        incomingIndex: teamIndex,
        incomingName: incoming.name,
      },
      message: outgoing
        ? `${outgoing.name} 退场，${incoming.name} 上场！`
        : `${incoming.name} 上场！`,
    });

    return true;
  }

  // ----------------------------------------------------------
  // Battle end check / 胜负判定
  // ----------------------------------------------------------

  /**
   * 检查战斗是否结束
   * Check whether the battle has ended.
   *
   * @returns 战斗结果（null = 未结束） / Battle result (null = not ended)
   */
  checkBattleEnd(): BattleResult {
    const playerAlive = this.getAlivePokemon('player');
    const opponentAlive = this.getAlivePokemon('opponent');

    if (playerAlive.length === 0 && opponentAlive.length === 0) {
      // 双方全灭，按惯例对手获胜
      // Both sides wiped out; conventionally opponent wins
      this._result = 'opponent';
      this._status = 'completed';
      this._addEvent({
        type: 'battle_end',
        data: { winner: 'opponent' },
        message: '双方全灭，对手获胜！',
      });
      return 'opponent';
    }

    if (playerAlive.length === 0) {
      this._result = 'opponent';
      this._status = 'completed';
      this._addEvent({
        type: 'battle_end',
        data: { winner: 'opponent' },
        message: '玩家所有宝可梦倒下，对手获胜！',
      });
      return 'opponent';
    }

    if (opponentAlive.length === 0) {
      this._result = 'player';
      this._status = 'completed';
      this._addEvent({
        type: 'battle_end',
        data: { winner: 'player' },
        message: '对手所有宝可梦倒下，玩家获胜！',
      });
      return 'player';
    }

    return null;
  }

  // ----------------------------------------------------------
  // State snapshot / 状态快照
  // ----------------------------------------------------------

  /**
   * 获取当前战斗状态快照
   * Get a snapshot of the current battle state.
   *
   * @returns 战斗状态快照 / Battle state snapshot
   */
  getCurrentState(): BattleStateSnapshot {
    return {
      status: this._status,
      currentRound: this._currentRound,
      playerTeam: [...this._playerTeam],
      opponentTeam: [...this._opponentTeam],
      playerActiveSlots: [...this._playerActiveSlots],
      opponentActiveSlots: [...this._opponentActiveSlots],
      result: this._result,
      events: [...this._events],
    };
  }

  /**
   * 重置战斗系统
   * Reset the combat system to its initial state.
   */
  reset(): void {
    this._playerTeam = [];
    this._opponentTeam = [];
    this._playerActiveSlots = [];
    this._opponentActiveSlots = [];
    this._currentRound = 0;
    this._result = null;
    this._status = 'idle';
    this._events = [];
  }

  // ----------------------------------------------------------
  // Internal / 内部方法
  // ----------------------------------------------------------

  /**
   * 添加战斗事件
   * Add a combat event.
   */
  private _addEvent(
    event: Omit<CombatEvent, 'timestamp'>,
  ): void {
    this._events.push({
      ...event,
      timestamp: Date.now(),
    });
  }

  /**
   * 状态效果中文标签
   * Status condition Chinese label.
   */
  private _statusLabel(status: StatusCondition): string {
    const labels: Record<string, string> = {
      burn: '烧伤',
      poison: '中毒',
      paralysis: '麻痹',
      sleep: '睡眠',
    };
    return labels[status ?? ''] ?? '';
  }
}

export default CombatSystem;
