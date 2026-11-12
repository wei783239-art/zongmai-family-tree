CREATE TABLE IF NOT EXISTS sys_user (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(64) NOT NULL UNIQUE,
 password_hash VARCHAR(255) NOT NULL, status VARCHAR(16) DEFAULT 'ACTIVE' NOT NULL,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS family (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(120) NOT NULL, surname VARCHAR(64),
 ancestral_hall VARCHAR(120), hometown VARCHAR(160), description TEXT, ancestor_person_id BIGINT,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS family_user (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, family_id BIGINT NOT NULL REFERENCES family(id),
 user_id BIGINT NOT NULL REFERENCES sys_user(id), role VARCHAR(24) NOT NULL DEFAULT 'MEMBER',
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UNIQUE(family_id,user_id),
 CHECK(role IN ('MEMBER','FAMILY_ADMIN'))
);
CREATE TABLE IF NOT EXISTS person (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, family_id BIGINT NOT NULL REFERENCES family(id),
 name VARCHAR(120) NOT NULL, gender VARCHAR(16), birth_date DATE, death_date DATE,
 generation INT, birth_place VARCHAR(160), biography TEXT, avatar_url VARCHAR(500),
 visibility VARCHAR(16) NOT NULL DEFAULT 'FAMILY',
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(id,family_id), CHECK(birth_date IS NULL OR death_date IS NULL OR birth_date <= death_date)
);
CREATE TABLE IF NOT EXISTS person_relation (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, family_id BIGINT NOT NULL,
 from_person_id BIGINT NOT NULL, to_person_id BIGINT NOT NULL,
 relation_type VARCHAR(32) NOT NULL, status VARCHAR(16) NOT NULL DEFAULT 'CONFIRMED',
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(from_person_id,family_id) REFERENCES person(id,family_id),
 FOREIGN KEY(to_person_id,family_id) REFERENCES person(id,family_id),
 UNIQUE(family_id,from_person_id,to_person_id,relation_type), CHECK(from_person_id <> to_person_id)
);
