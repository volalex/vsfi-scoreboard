# Tasks schema

# --- !Ups
CREATE SEQUENCE task_id_seq;
CREATE TABLE task (
  id BIGINT NOT NULL DEFAULT nextval('task_id_seq'),
  name VARCHAR(255),
  task_text TEXT,
  PRIMARY KEY (id)
);

# --- !Downs
DROP TABLE task;
DROP SEQUENCE task_id_seq;
