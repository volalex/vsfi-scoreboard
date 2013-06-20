# Many-to-many table schema

# --- !Ups
CREATE SEQUENCE task_to_team_id_seq;
CREATE TABLE task_to_team (
  team_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  score INTEGER,
  solved_at BIGINT,
  PRIMARY KEY(team_id,task_id),
  FOREIGN KEY(team_id) REFERENCES team(id) ON DELETE CASCADE ON UPDATE RESTRICT,
  FOREIGN KEY(task_id) REFERENCES task(id) ON DELETE CASCADE ON UPDATE RESTRICT
);

# --- !Downs
DROP TABLE task_to_team;
DROP SEQUENCE task_to_team_id_seq;
