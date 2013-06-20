# Teams schema

# --- !Ups
CREATE SEQUENCE team_id_seq;
CREATE TABLE team (
  id BIGINT NOT NULL DEFAULT nextval('team_id_seq'),
  name VARCHAR(255),
  dns_ip VARCHAR(16),
  PRIMARY KEY(id)
);

# --- !Downs

DROP TABLE team;
DROP SEQUENCE team_id_seq;
