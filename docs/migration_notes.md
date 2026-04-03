Migration notes

- V5..V6 already applied: user table and team.version
- V7__battle_add_player_move_map.sql: adds player_move_map TEXT to battle table so async matches can persist client-provided move mappings.

How to run migrations:

1. Ensure Flyway is configured in application.properties (spring.flyway.locations includes classpath:db/migration)
2. Run the application or `mvn -f pokemon-factory-backend\battleFactory flyway:migrate` to apply migrations.

JWT and runtime notes:
- Set JWT_SECRET env var for persistent tokens in production.
