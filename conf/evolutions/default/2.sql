-- Recipe schema

-- !Ups
CREATE TABLE recipe
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(255)             NOT NULL,
    description text,
    user_id     INTEGER,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE CASCADE
);

CREATE INDEX recipe_name_idx ON recipe (name);

CREATE TABLE flavor
(
    id         SERIAL PRIMARY KEY,
    name       VARCHAR(255)             NOT NULL,
    recipe_id  INTEGER                  NOT NULL,
    quantity   INTEGER                           DEFAULT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recipe_id) REFERENCES recipe (id) ON DELETE CASCADE
);

CREATE INDEX flavor_name_idx ON flavor (name);

-- !Downs
DROP INDEX flavor_name_idx;
DROP INDEX recipe_name_idx;
DROP TABLE flavor;
DROP TABLE recipe;
