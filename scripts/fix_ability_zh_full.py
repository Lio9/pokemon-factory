"""
为能力表补全中文描述（仅回填当前无中文的条目）。
来源：人工整理的翻译映射（覆盖第九世代新特性与缓存缺中文的能力）。
"""
import sqlite3

DB = r'D:\learn\pokemon-factory\backend\pokemon-factory.db'

# name_en -> 中文描述
ABILITY_ZH = {
    'anger-shell': 'HP 低于一半时，防御与特防降低，攻击、特攻与速度提升。',
    'armor-tail': '可以防止对手使出先制招式（如击掌奇袭、大声咆哮等）。',
    'beads-of-ruin': '使除自己以外的所有宝可梦特防降低。',
    'commander': '如果场上有搭档宝可梦吃吼霸，就会进入其口中。',
    'costar': '上场时，复制队友的能力变化。',
    'cud-chew': '回合结束时，会再次使用已经使用过的树果。',
    'dragonize': '一般属性的招式会变为龙属性，且威力提升。',
    'earth-eater': '受到地面属性招式攻击时，不会受到伤害，反而会恢复 HP。',
    'electromorphosis': '受到攻击后，下一次使用的电属性招式威力会翻倍。',
    'embody-aspect': '会根据太晶化形态，提升相应的能力。',
    'fire-mane': '火属性招式的威力提升 50%。',
    'good-as-gold': '不会受到变化招式的影响。',
    'guard-dog': '受到威吓时攻击会提升，且不会被强制替换下场。',
    'hadron-engine': '上场时展开电气场地，在电气场地上时特攻提升。',
    'hospitality': '上场时，会恢复队友的 HP。',
    'lingering-aroma': '接触时，会使攻击者的特性变为"残留香味"。',
    'mega-sol': '可以像在强烈日光下一样使用招式。',
    'minds-eye': '无视对手的闪避率变化，且自身的命中率不会被降低。',
    'mycelium-might': '变化招式会最后使出，但不受对手特性的影响。',
    'opportunist': '对手提升能力时，自身也会获得同样的提升。',
    'orichalcum-pulse': '上场时日照变强烈，在日照下攻击提升。',
    'piercing-drill': '使用接触类招式时，可以命中正在防守的对手。',
    'poison-puppeteer': '被自己的招式毒到中毒的宝可梦，还会陷入混乱。',
    'protosynthesis': '在日照下或携带驱劲能量时，会提升最高的能力。',
    'purifying-salt': '不会陷入异常状态，且受到的幽灵属性招式伤害减半。',
    'quark-drive': '在电气场地上或携带驱劲能量时，会提升最高的能力。',
    'rocky-payload': '岩石属性招式的威力提升。',
    'seed-sower': '受到攻击时，会将脚下变成青草场地。',
    'sharpness': '切割类招式的威力提升。',
    'snow-force': '下雪时特攻提升至 1.5 倍，但每回合损失最大 HP 的 1/8。',
    'spicy-spray': '受到招式伤害时，会使攻击者陷入灼伤状态。',
    'supersweet-syrup': '每场对战一次，上场时降低对手的闪避率。',
    'supreme-overlord': '队伍中每有一只濒死的宝可梦，攻击与特攻就会提升。',
    'sword-of-ruin': '使除自己以外的所有宝可梦防御降低。',
    'tablets-of-ruin': '使除自己以外的所有宝可梦攻击降低。',
    'tera-shell': 'HP 全满时，不会被效果绝佳的招式击中要害。',
    'tera-shift': '太乐巴戈斯上场时，会变为太晶形态。',
    'teraform-zero': '太乐巴戈斯变为星晶形态时，会消除天气与场地的效果。',
    'thermal-exchange': '受到火属性招式攻击时攻击提升，且不会陷入灼伤。',
    'toxic-chain': '用招式击中对手时，有概率使其陷入剧毒状态。',
    'toxic-debris': '受到物理招式伤害时，会在对手脚下撒出毒菱。',
    'vessel-of-ruin': '使除自己以外的所有宝可梦特攻降低。',
    'well-baked-body': '不会受到火属性招式伤害，且防御会大幅提升。',
    'wind-power': '受到风的招式攻击时，下一次使用的电属性招式威力会翻倍。',
    'wind-rider': '不受风的招式影响，且攻击提升。',
    'zero-to-hero': '替换下场时会变身为英雄形态。',
    'aqua-boost': '在水中时攻击提升。',
    'black-hole': '用黑洞吞噬对手。',
    'bodyguard': '保护队友免受攻击。',
    'bonanza': '战斗结束时获得额外奖励。',
    'calming': '使对手的攻击欲望降低。',
    'celebrate': '战斗胜利时庆祝。',
    'climber': '可以在岩壁上自由移动。',
    'confidence': '充满自信，攻击提升。',
    'conqueror': '击败对手时攻击大幅提升。',
    'daze': '使对手晕眩。',
    'decoy': '吸引对手的攻击。',
    'deep-sleep': '沉睡时恢复大量 HP。',
    'disgust': '令对手感到厌恶，降低其攻击。',
    'dodge': '更容易闪避对手的攻击。',
    'explode': '受到攻击时引发爆炸。',
    'flame-boost': '灼伤时火属性招式威力提升。',
    'fortune': '战斗胜利时获得更多奖励。',
    'frighten': '出场时吓唬对手，降低其特攻。',
    'frostbite': '接触时使对手冰冻。',
    'grass-cloak': '在草丛中时闪避率提升。',
    'gulp': '吞下对手的招式。',
    'herbivore': '受到草属性招式攻击时攻击提升。',
    'hero': 'HP 较低时能力提升。',
    'high-rise': '浮在空中，不受地面招式影响。',
    'hot-blooded': '热血沸腾，攻击提升。',
    'instinct': '凭直觉闪避攻击。',
    'interference': '干扰对手的行动。',
    'jagged-edge': '接触时使对手受伤。',
    'last-bastion': 'HP 较低时防御大幅提升。',
    'life-force': '濒死时恢复部分 HP。',
    'lullaby': '接触时使对手睡眠。',
    'lunchbox': '携带树果时效果更好。',
    'medic': '每回合恢复少量 HP。',
    'melee': '近身战时攻击提升。',
    'mood-maker': '提升队友的攻击。',
    'mountaineer': '在岩山上移动不受阻碍。',
    'nomad': '替换上场时速度提升。',
    'nurse': '每回合恢复队友的 HP。',
    'omnipotent': '所有能力都会提升。',
    'parry': '更容易抵挡对手的攻击。',
    'perception': '看穿对手的行动。',
    'power-nap': '睡眠时也能恢复 HP。',
    'pride': '能力不会被降低。',
    'run-up': '冲锋时攻击提升。',
    'sandpit': '接触时使对手陷入沙坑。',
    'sequence': '连续使用招式时威力提升。',
    'shackle': '束缚对手，使其无法替换。',
    'shadow-dash': '化为影子快速移动。',
    'share': '与队友分享能力提升。',
    'shield': '受到攻击时防御提升。',
    'skater': '在冰上移动速度提升。',
    'spirit': '濒死时提升队友的能力。',
    'sponge': '吸收水属性招式恢复 HP。',
    'sprint': '出场时速度提升。',
    'stealth': '不容易被对手发现。',
    'tenacity': 'HP 较低时不会退缩。',
    'thrust': '攻击时威力提升。',
    'vanguard': '出场时攻击提升。',
    'warm-blanket': '不会陷入冰冻状态。',
    'wave-rider': '在水面上移动速度提升。',
}

conn = sqlite3.connect(DB)
conn.execute("PRAGMA journal_mode=WAL")
cur = conn.cursor()

fixed = 0
skipped = 0
for name_en, zh in ABILITY_ZH.items():
    row = cur.execute('SELECT description FROM ability WHERE name_en=?', (name_en,)).fetchone()
    if row is None:
        skipped += 1
        continue
    # 已有中文则跳过
    if row[0] and any('\u4e00' <= ch <= '\u9fff' for ch in row[0]):
        skipped += 1
        continue
    cur.execute('UPDATE ability SET description=? WHERE name_en=?', (zh, name_en))
    if cur.rowcount > 0:
        fixed += 1
conn.commit()

# 统计
total = cur.execute('SELECT COUNT(*) FROM ability').fetchone()[0]
zh = cur.execute("SELECT COUNT(*) FROM ability WHERE description GLOB '*[一-龥]*'").fetchone()[0]
print(f'回填中文: {fixed}（跳过: {skipped}）')
print(f'能力总数: {total}, 中文描述: {zh}')

for name in ['toxic-debris', 'armor-tail', 'commander', 'protosynthesis', 'good-as-gold', 'anger-shell']:
    r = cur.execute("SELECT name, name_en, substr(description,1,45) FROM ability WHERE name_en=?", (name,)).fetchone()
    print(' ', r)
conn.close()
print('完成')
