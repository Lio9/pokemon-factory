-- Flyway V11: insert default skills (team_shield, protect) into skill_catalog
INSERT INTO skill_catalog(name, default_cooldown) VALUES('team_shield', 2);
INSERT INTO skill_catalog(name, default_cooldown) VALUES('protect', 2);
