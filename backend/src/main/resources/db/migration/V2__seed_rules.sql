-- Sample rule set for R&D trial calculations.
-- DISCLAIMER: this is a project-internal SAMPLE rule set. It is NOT aligned
-- with, and does not claim compliance with, any national or regional
-- labelling regulation.

insert into rule_set (id, code, name, disclaimer) values
(1, 'SAMPLE-RULESET',
 '示例规则集（研发试算用）',
 '本结果由示例规则集计算，仅用于研发阶段配方比较，不宣称符合任何国家或地区现行法规。'
 || ' This label is computed with a SAMPLE rule set for R&D comparison only;'
 || ' it does not claim compliance with any national or regional regulation.');

insert into rule_version (id, rule_set_id, version, note) values
(1, 1, '1.0', '示例规则初版：能量/蛋白质/脂肪/碳水/糖/钠/盐当量，含零阈值与微量阈值。'),
(2, 1, '1.1', '调整示例：能量修约间隔 5 kcal，蛋白质间隔 0.5 g，钠间隔 5 mg，脂肪零阈值 0.1 g，用于演示规则版本对比。');

-- Nutrients. storage_unit is the unit of ingredient values per 100 g.
-- Derived nutrients: salt_eq = sodium x 2.5 / 1000 (mg -> g),
--                    energy_kj = energy_kcal x 4.184 (kcal -> kJ).
insert into nutrient (id, code, name, storage_unit, display_order,
                      derived_from_code, derive_multiplier, derive_divisor) values
(1, 'energy_kcal', '能量',   'kcal', 1, null,         null,   null),
(2, 'energy_kj',   '能量',   'kJ',   2, 'energy_kcal', 4.184, 1),
(3, 'protein',     '蛋白质', 'g',    3, null,         null,   null),
(4, 'fat',         '脂肪',   'g',    4, null,         null,   null),
(5, 'carb',        '碳水化合物', 'g', 5, null,       null,   null),
(6, 'sugar',       '糖',     'g',    6, null,         null,   null),
(7, 'sodium',      '钠',     'mg',   7, null,         null,   null),
(8, 'salt_eq',     '盐当量', 'g',    8, 'sodium',      2.5,   1000);

-- Rule version 1.0
insert into nutrient_rule (id, rule_version_id, nutrient_id, display_unit,
                           rounding_increment, zero_threshold, trace_threshold) values
( 1, 1, 1, 'kcal', 1,    0,     null),
( 2, 1, 2, 'kJ',   1,    0,     null),
( 3, 1, 3, 'g',    0.1,  0.05,  null),
( 4, 1, 4, 'g',    0.1,  0.05,  null),
( 5, 1, 5, 'g',    0.1,  0.05,  null),
( 6, 1, 6, 'g',    0.1,  0.05,  0.5),
( 7, 1, 7, 'mg',   1,    0.5,   5),
( 8, 1, 8, 'g',    0.01, 0.005, null);

-- Rule version 1.1 (comparison demo: coarser increments / different thresholds)
insert into nutrient_rule (id, rule_version_id, nutrient_id, display_unit,
                           rounding_increment, zero_threshold, trace_threshold) values
( 9, 2, 1, 'kcal', 5,    0,     null),
(10, 2, 2, 'kJ',   5,    0,     null),
(11, 2, 3, 'g',    0.5,  0.05,  null),
(12, 2, 4, 'g',    0.1,  0.1,   null),
(13, 2, 5, 'g',    0.1,  0.05,  null),
(14, 2, 6, 'g',    0.1,  0.05,  0.5),
(15, 2, 7, 'mg',   5,    0.5,   10),
(16, 2, 8, 'g',    0.1,  0.005, null);
