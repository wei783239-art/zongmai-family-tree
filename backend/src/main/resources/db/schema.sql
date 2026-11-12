CREATE DATABASE IF NOT EXISTS family_tree
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE family_tree;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    CONSTRAINT chk_sys_user_status CHECK (status IN ('ACTIVE', 'DISABLED'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS family (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    surname VARCHAR(64) NULL,
    ancestral_hall VARCHAR(120) NULL,
    hometown VARCHAR(160) NULL,
    description TEXT NULL,
    motto VARCHAR(2000) NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    ancestor_person_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_family_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS family_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(24) NOT NULL DEFAULT 'MEMBER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_family_user (family_id, user_id),
    CONSTRAINT fk_family_user_family FOREIGN KEY (family_id) REFERENCES family (id),
    CONSTRAINT fk_family_user_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT chk_family_user_role CHECK (role IN ('MEMBER', 'FAMILY_ADMIN'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS person (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    gender VARCHAR(16) NULL,
    birth_date DATE NULL,
    death_date DATE NULL,
    generation INT NULL,
    birth_place VARCHAR(160) NULL,
    biography TEXT NULL,
    nickname VARCHAR(80) NULL,
    hometown VARCHAR(160) NULL,
    avatar_url VARCHAR(500) NULL,
    visibility VARCHAR(16) NOT NULL DEFAULT 'FAMILY',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_person_id_family (id, family_id),
    KEY idx_person_family_name (family_id, name),
    CONSTRAINT fk_person_family FOREIGN KEY (family_id) REFERENCES family (id),
    CONSTRAINT chk_person_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'UNKNOWN')),
    CONSTRAINT chk_person_visibility CHECK (visibility IN ('PUBLIC', 'FAMILY', 'PRIVATE')),
    CONSTRAINT chk_person_dates CHECK (birth_date IS NULL OR death_date IS NULL OR birth_date <= death_date)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS person_relation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    from_person_id BIGINT NOT NULL,
    to_person_id BIGINT NOT NULL,
    relation_type VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'CONFIRMED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_person_relation (family_id, from_person_id, to_person_id, relation_type),
    KEY idx_relation_to_person (family_id, to_person_id, relation_type),
    CONSTRAINT fk_relation_from_person FOREIGN KEY (from_person_id, family_id) REFERENCES person (id, family_id),
    CONSTRAINT fk_relation_to_person FOREIGN KEY (to_person_id, family_id) REFERENCES person (id, family_id),
    CONSTRAINT chk_relation_not_self CHECK (from_person_id <> to_person_id),
    CONSTRAINT chk_relation_type CHECK (relation_type IN ('PARENT_CHILD', 'ADOPTIVE_PARENT_CHILD', 'SPOUSE')),
    CONSTRAINT chk_relation_status CHECK (status IN ('PENDING', 'CONFIRMED', 'REJECTED'))
) ENGINE=InnoDB;

