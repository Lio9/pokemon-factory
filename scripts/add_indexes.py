#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
创建性能优化索引
"""
import sqlite3
import os

def main():
    db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'pokemon-factory.db')
    
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    # 获取所有表名
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
    tables = [row[0] for row in cursor.fetchall()]
    
    print(f"数据库中的表: {len(tables)}个")
    
    # 定义索引创建语句
    indexes = [
        # Pokemon相关
        ('idx_pokemon_species_name', 'CREATE INDEX IF NOT EXISTS idx_pokemon_species_name ON pokemon_species(name)', 'pokemon_species'),
        ('idx_pokemon_species_name_en', 'CREATE INDEX IF NOT EXISTS idx_pokemon_species_name_en ON pokemon_species(name_en)', 'pokemon_species'),
        ('idx_pokemon_species_generation', 'CREATE INDEX IF NOT EXISTS idx_pokemon_species_generation ON pokemon_species(generation_id)', 'pokemon_species'),
        ('idx_pokemon_species_evolution_chain', 'CREATE INDEX IF NOT EXISTS idx_pokemon_species_evolution_chain ON pokemon_species(evolution_chain_id)', 'pokemon_species'),
        
        # PokemonForm相关
        ('idx_pokemon_form_species', 'CREATE INDEX IF NOT EXISTS idx_pokemon_form_species ON pokemon_form(species_id)', 'pokemon_form'),
        ('idx_pokemon_form_is_default', 'CREATE INDEX IF NOT EXISTS idx_pokemon_form_is_default ON pokemon_form(is_default)', 'pokemon_form'),
        ('idx_pokemon_form_name', 'CREATE INDEX IF NOT EXISTS idx_pokemon_form_name ON pokemon_form(form_name)', 'pokemon_form'),
        
        # PokemonFormType相关
        ('idx_pokemon_form_type_form', 'CREATE INDEX IF NOT EXISTS idx_pokemon_form_type_form ON pokemon_form_type(form_id)', 'pokemon_form_type'),
        ('idx_pokemon_form_type_type', 'CREATE INDEX IF NOT EXISTS idx_pokemon_form_type_type ON pokemon_form_type(type_id)', 'pokemon_form_type'),
        
        # PokemonFormAbility相关
        ('idx_pokemon_form_ability_form', 'CREATE INDEX IF NOT EXISTS idx_pokemon_form_ability_form ON pokemon_form_ability(form_id)', 'pokemon_form_ability'),
        ('idx_pokemon_form_ability_ability', 'CREATE INDEX IF NOT EXISTS idx_pokemon_form_ability_ability ON pokemon_form_ability(ability_id)', 'pokemon_form_ability'),
        
        # PokemonFormStat相关
        ('idx_pokemon_form_stat_form', 'CREATE INDEX IF NOT EXISTS idx_pokemon_form_stat_form ON pokemon_form_stat(form_id)', 'pokemon_form_stat'),
        ('idx_pokemon_form_stat_stat', 'CREATE INDEX IF NOT EXISTS idx_pokemon_form_stat_stat ON pokemon_form_stat(stat_id)', 'pokemon_form_stat'),
        
        # PokemonFormMove相关
        ('idx_pokemon_form_move_form', 'CREATE INDEX IF NOT EXISTS idx_pokemon_form_move_form ON pokemon_form_move(form_id)', 'pokemon_form_move'),
        ('idx_pokemon_form_move_move', 'CREATE INDEX IF NOT EXISTS idx_pokemon_form_move_move ON pokemon_form_move(move_id)', 'pokemon_form_move'),
        
        # Move相关
        ('idx_move_name', 'CREATE INDEX IF NOT EXISTS idx_move_name ON move(name)', 'move'),
        ('idx_move_name_en', 'CREATE INDEX IF NOT EXISTS idx_move_name_en ON move(name_en)', 'move'),
        ('idx_move_type', 'CREATE INDEX IF NOT EXISTS idx_move_type ON move(type_id)', 'move'),
        ('idx_move_damage_class', 'CREATE INDEX IF NOT EXISTS idx_move_damage_class ON move(damage_class_id)', 'move'),
        ('idx_move_power', 'CREATE INDEX IF NOT EXISTS idx_move_power ON move(power)', 'move'),
        ('idx_move_priority', 'CREATE INDEX IF NOT EXISTS idx_move_priority ON move(priority)', 'move'),
        
        # Ability相关
        ('idx_ability_name', 'CREATE INDEX IF NOT EXISTS idx_ability_name ON ability(name)', 'ability'),
        ('idx_ability_name_en', 'CREATE INDEX IF NOT EXISTS idx_ability_name_en ON ability(name_en)', 'ability'),
        
        # Item相关
        ('idx_item_name', 'CREATE INDEX IF NOT EXISTS idx_item_name ON items(name)', 'items'),
        ('idx_item_name_en', 'CREATE INDEX IF NOT EXISTS idx_item_name_en ON items(name_en)', 'items'),
        ('idx_item_category', 'CREATE INDEX IF NOT EXISTS idx_item_category ON items(item_category_id)', 'items'),
        
        # Type相关
        ('idx_type_name', 'CREATE INDEX IF NOT EXISTS idx_type_name ON type(name)', 'type'),
        ('idx_type_name_en', 'CREATE INDEX IF NOT EXISTS idx_type_name_en ON type(name_en)', 'type'),
        
        # TypeEfficacy相关
        ('idx_type_efficacy_attacking', 'CREATE INDEX IF NOT EXISTS idx_type_efficacy_attacking ON type_efficacy(attacking_type_id)', 'type_efficacy'),
        ('idx_type_efficacy_defending', 'CREATE INDEX IF NOT EXISTS idx_type_efficacy_defending ON type_efficacy(defending_type_id)', 'type_efficacy'),
        
        # PokemonEggGroup相关
        ('idx_pokemon_egg_group_pokemon', 'CREATE INDEX IF NOT EXISTS idx_pokemon_egg_group_pokemon ON pokemon_egg_group(pokemon_id)', 'pokemon_egg_group'),
        ('idx_pokemon_egg_group_egg', 'CREATE INDEX IF NOT EXISTS idx_pokemon_egg_group_egg ON pokemon_egg_group(egg_group_id)', 'pokemon_egg_group'),
        
        # PokemonEvolution相关
        ('idx_pokemon_evolution_evolved', 'CREATE INDEX IF NOT EXISTS idx_pokemon_evolution_evolved ON pokemon_evolution(evolved_species_id)', 'pokemon_evolution'),
        ('idx_pokemon_evolution_from', 'CREATE INDEX IF NOT EXISTS idx_pokemon_evolution_from ON pokemon_evolution(evolves_from_species_id)', 'pokemon_evolution'),
        
        # AbilityEffect相关
        ('idx_ability_effect_ability', 'CREATE INDEX IF NOT EXISTS idx_ability_effect_ability ON ability_effect(ability_id)', 'ability_effect'),
        ('idx_ability_effect_type', 'CREATE INDEX IF NOT EXISTS idx_ability_effect_type ON ability_effect(effect_type)', 'ability_effect'),
        
        # ItemEffect相关
        ('idx_item_effect_item', 'CREATE INDEX IF NOT EXISTS idx_item_effect_item ON item_effect(item_id)', 'item_effect'),
        ('idx_item_effect_type', 'CREATE INDEX IF NOT EXISTS idx_item_effect_type ON item_effect(effect_type)', 'item_effect'),
        
        # 覆盖索引
        ('idx_move_search', 'CREATE INDEX IF NOT EXISTS idx_move_search ON move(name, name_en, type_id, power)', 'move'),
        ('idx_pokemon_search', 'CREATE INDEX IF NOT EXISTS idx_pokemon_search ON pokemon_species(name, name_en, id)', 'pokemon_species'),
        ('idx_ability_search', 'CREATE INDEX IF NOT EXISTS idx_ability_search ON ability(name, name_en, id)', 'ability'),
        ('idx_item_search', 'CREATE INDEX IF NOT EXISTS idx_item_search ON items(name, name_en, id)', 'items'),
    ]
    
    created = 0
    skipped = 0
    
    for index_name, sql, table_name in indexes:
        if table_name in tables:
            try:
                cursor.execute(sql)
                created += 1
                print(f"✓ 创建索引: {index_name}")
            except Exception as e:
                print(f"✗ 创建索引失败 {index_name}: {e}")
        else:
            skipped += 1
            print(f"- 跳过索引（表不存在）: {index_name} -> {table_name}")
    
    conn.commit()
    conn.close()
    
    print(f"\n索引创建完成！")
    print(f"  创建: {created}个")
    print(f"  跳过: {skipped}个")

if __name__ == '__main__':
    main()