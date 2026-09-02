/**
 * BattleStateMachine - 战斗状态机
 *
 * 管理 3D 战斗场景的阶段流转，采用有限状态机（FSM）模式。
 * Manages phase transitions in the 3D battle scene using a Finite State Machine (FSM) pattern.
 *
 * @module BattleStateMachine
 */

// ============================================================
// Types / 类型定义
// ============================================================

/**
 * 战斗阶段枚举
 * Battle phase enumeration.
 *
 * | 阶段 | 英文 | 说明 |
 * |------|------|------|
 * | idle | Idle | 等待开始 |
 * | team-preview | Team Preview | 队伍预览（选人阶段） |
 * | battle-start | Battle Start | 战斗开始动画 |
 * | action-select | Action Select | 选择行动（招式/换人） |
 * | action-executing | Action Executing | 执行行动动画 |
 * | turn-resolving | Turn Resolving | 回合结算 |
 * | replacement | Replacement | 替补选择 |
 * | battle-end | Battle End | 战斗结束 |
 * | victory | Victory | 胜利 |
 * | defeat | Defeat | 失败 |
 */
export type BattlePhase =
  | 'idle'
  | 'team-preview'
  | 'battle-start'
  | 'action-select'
  | 'action-executing'
  | 'turn-resolving'
  | 'replacement'
  | 'battle-end'
  | 'victory'
  | 'defeat';

/**
 * 阶段标签（中英双语）
 * Phase labels in both Chinese and English.
 */
export interface PhaseLabel {
  /** 中文标签 / Chinese label */
  zh: string;
  /** 英文标签 / English label */
  en: string;
}

/**
 * 状态变化事件载荷
 * Payload emitted on every state transition.
 */
export interface StateChangeEvent {
  /** 上一个阶段 / Previous phase */
  from: BattlePhase;
  /** 新阶段 / New phase */
  to: BattlePhase;
  /** 变化时间戳（ms） / Transition timestamp (ms) */
  timestamp: number;
}

/**
 * 阶段进入/退出钩子上下文
 * Context passed to phase enter/exit hooks.
 */
export interface PhaseHookContext {
  /** 当前阶段 / Current phase */
  phase: BattlePhase;
  /** 前一个阶段 / Previous phase */
  previousPhase: BattlePhase | null;
  /** 变化时间戳 / Transition timestamp */
  timestamp: number;
}

// ============================================================
// Constants / 常量
// ============================================================

/**
 * 合法的状态转换映射表（FSM 邻接表）
 * Valid transition map (FSM adjacency table).
 *
 * 任何未列出的转换均视为非法。
 * Any transition not listed is considered illegal.
 */
const TRANSITION_MAP: Record<BattlePhase, BattlePhase[]> = {
  idle: ['team-preview'],
  'team-preview': ['battle-start', 'idle'],
  'battle-start': ['action-select'],
  'action-select': ['action-executing', 'replacement', 'battle-end'],
  'action-executing': ['turn-resolving'],
  'turn-resolving': ['action-select', 'replacement', 'battle-end'],
  replacement: ['action-select', 'battle-end'],
  'battle-end': ['victory', 'defeat', 'idle'],
  victory: ['idle'],
  defeat: ['idle'],
};

/**
 * 每个阶段的中英文标签
 * Chinese & English labels for every phase.
 */
const PHASE_LABELS: Record<BattlePhase, PhaseLabel> = {
  idle: { zh: '等待开始', en: 'Idle' },
  'team-preview': { zh: '队伍预览', en: 'Team Preview' },
  'battle-start': { zh: '战斗开始', en: 'Battle Start' },
  'action-select': { zh: '选择行动', en: 'Action Select' },
  'action-executing': { zh: '执行行动', en: 'Action Executing' },
  'turn-resolving': { zh: '回合结算', en: 'Turn Resolving' },
  replacement: { zh: '替补选择', en: 'Replacement' },
  'battle-end': { zh: '战斗结束', en: 'Battle End' },
  victory: { zh: '胜利', en: 'Victory' },
  defeat: { zh: '失败', en: 'Defeat' },
};

// ============================================================
// EventEmitter mixin (typed, no external deps)
// ============================================================

/**
 * 简易类型安全事件发射器
 * Minimal type-safe event emitter.
 */
type EventMap = {
  /** 状态变化事件 / State change event */
  change: StateChangeEvent;
  /** 阶段进入钩子 / Phase enter hook */
  enter: PhaseHookContext;
  /** 阶段退出钩子 / Phase exit hook */
  exit: PhaseHookContext;
};

type Listener<T> = (payload: T) => void;

class TypedEventEmitter {
  private _listeners = new Map<string, Set<Listener<any>>>();

  /**
   * 注册事件监听器
   * Register an event listener.
   *
   * @param event - 事件名称 / Event name
   * @param listener - 回调函数 / Callback function
   * @returns 取消订阅函数 / Unsubscribe function
   */
  on<K extends keyof EventMap>(event: K, listener: Listener<EventMap[K]>): () => void {
    if (!this._listeners.has(event as string)) {
      this._listeners.set(event as string, new Set());
    }
    this._listeners.get(event as string)!.add(listener);
    return () => this.off(event, listener);
  }

  /**
   * 移除事件监听器
   * Remove an event listener.
   *
   * @param event - 事件名称 / Event name
   * @param listener - 要移除的回调 / Callback to remove
   */
  off<K extends keyof EventMap>(event: K, listener: Listener<EventMap[K]>): void {
    this._listeners.get(event as string)?.delete(listener);
  }

  /**
   * 触发事件
   * Emit an event.
   *
   * @param event - 事件名称 / Event name
   * @param payload - 事件载荷 / Event payload
   */
  protected _emit<K extends keyof EventMap>(event: K, payload: EventMap[K]): void {
    this._listeners.get(event as string)?.forEach((fn) => fn(payload));
  }

  /**
   * 移除所有监听器
   * Remove all listeners.
   */
  removeAllListeners(): void {
    this._listeners.clear();
  }
}

// ============================================================
// BattleStateMachine
// ============================================================

/**
 * 战斗状态机
 * Battle state machine.
 *
 * 管理战斗的整个生命周期，从 `idle` 开始，经过多个阶段，最终进入 `victory` 或 `defeat`。
 * Manages the entire battle lifecycle, from `idle` through multiple phases to `victory` or `defeat`.
 *
 * @example
 * ```ts
 * const sm = new BattleStateMachine();
 *
 * sm.on('change', ({ from, to }) => {
 *   console.log(`Phase: ${from} -> ${to}`);
 * });
 *
 * sm.on('enter', ({ phase }) => {
 *   console.log(`Entered: ${phase}`);
 * });
 *
 * sm.transition('team-preview'); // idle -> team-preview
 * sm.transition('battle-start'); // team-preview -> battle-start
 * ```
 */
export class BattleStateMachine extends TypedEventEmitter {
  /** 当前阶段 / Current phase */
  private _current: BattlePhase = 'idle';

  /** 上一个阶段 / Previous phase */
  private _previous: BattlePhase | null = null;

  /** 当前阶段进入时间戳 / Timestamp when current phase was entered */
  private _enteredAt: number = Date.now();

  /** 历史记录 / Transition history */
  private _history: StateChangeEvent[] = [];

  // ----------------------------------------------------------
  // Getters
  // ----------------------------------------------------------

  /**
   * 获取当前阶段
   * Get the current phase.
   */
  get current(): BattlePhase {
    return this._current;
  }

  /**
   * 获取上一个阶段
   * Get the previous phase.
   */
  get previous(): BattlePhase | null {
    return this._previous;
  }

  /**
   * 获取当前阶段的中英文标签
   * Get the current phase's label (zh/en).
   */
  get currentLabel(): PhaseLabel {
    return PHASE_LABELS[this._current];
  }

  /**
   * 获取当前阶段已持续时间（ms）
   * Get elapsed time (ms) since entering the current phase.
   */
  get elapsed(): number {
    return Date.now() - this._enteredAt;
  }

  /**
   * 获取完整的转换历史
   * Get the full transition history.
   */
  get history(): ReadonlyArray<StateChangeEvent> {
    return this._history;
  }

  // ----------------------------------------------------------
  // Transition / 状态转换
  // ----------------------------------------------------------

  /**
   * 判断是否可以转换到目标阶段
   * Check whether a transition to the given phase is valid.
   *
   * @param target - 目标阶段 / Target phase
   * @returns 是否合法 / Whether the transition is valid
   */
  canTransitionTo(target: BattlePhase): boolean {
    return TRANSITION_MAP[this._current]?.includes(target) ?? false;
  }

  /**
   * 获取当前阶段所有可转换的目标阶段
   * Get all phases reachable from the current phase.
   */
  getAvailableTransitions(): BattlePhase[] {
    return [...(TRANSITION_MAP[this._current] ?? [])];
  }

  /**
   * 执行状态转换
   * Perform a state transition.
   *
   * @param target - 目标阶段 / Target phase
   * @returns 转换是否成功 / Whether the transition succeeded
   * @throws 当目标阶段无效时（非合法转换）抛出错误
   *         Throws when the target phase is invalid (illegal transition).
   */
  transition(target: BattlePhase): boolean {
    if (!this.canTransitionTo(target)) {
      console.warn(
        `[BattleStateMachine] 非法转换: ${this._current} -> ${target}。` +
          `允许的目标: [${TRANSITION_MAP[this._current]?.join(', ')}]`,
      );
      return false;
    }

    const from = this._current;
    const now = Date.now();

    // 1. 触发 exit 钩子 / Fire exit hook
    this._emit('exit', {
      phase: from,
      previousPhase: this._previous,
      timestamp: now,
    });

    // 2. 更新状态 / Update state
    this._previous = from;
    this._current = target;
    this._enteredAt = now;

    // 3. 记录历史 / Record history
    const event: StateChangeEvent = { from, to: target, timestamp: now };
    this._history.push(event);

    // 4. 触发 change 事件 / Fire change event
    this._emit('change', event);

    // 5. 触发 enter 钩子 / Fire enter hook
    this._emit('enter', {
      phase: target,
      previousPhase: from,
      timestamp: now,
    });

    return true;
  }

  // ----------------------------------------------------------
  // Helpers / 辅助方法
  // ----------------------------------------------------------

  /**
   * 获取指定阶段的中英文标签
   * Get the label (zh/en) for a given phase.
   *
   * @param phase - 战斗阶段 / Battle phase
   * @returns 中英文标签 / Phase label
   */
  getPhaseLabel(phase: BattlePhase): PhaseLabel {
    return PHASE_LABELS[phase];
  }

  /**
   * 获取指定阶段的所有合法目标转换
   * Get valid target transitions for a given phase.
   *
   * @param phase - 战斗阶段 / Battle phase
   * @returns 可转换的阶段列表 / List of reachable phases
   */
  getTransitionsFor(phase: BattlePhase): BattlePhase[] {
    return [...(TRANSITION_MAP[phase] ?? [])];
  }

  /**
   * 重置状态机到 idle
   * Reset the state machine to idle.
   */
  reset(): void {
    this._emit('exit', {
      phase: this._current,
      previousPhase: this._previous,
      timestamp: Date.now(),
    });

    this._current = 'idle';
    this._previous = null;
    this._enteredAt = Date.now();
    this._history = [];

    this._emit('enter', {
      phase: 'idle',
      previousPhase: null,
      timestamp: this._enteredAt,
    });
  }

  /**
   * 获取当前状态快照
   * Get a snapshot of the current state.
   */
  snapshot(): {
    current: BattlePhase;
    previous: BattlePhase | null;
    elapsed: number;
    historyLength: number;
    label: PhaseLabel;
    availableTransitions: BattlePhase[];
  } {
    return {
      current: this._current,
      previous: this._previous,
      elapsed: this.elapsed,
      historyLength: this._history.length,
      label: this.currentLabel,
      availableTransitions: this.getAvailableTransitions(),
    };
  }
}

export default BattleStateMachine;
