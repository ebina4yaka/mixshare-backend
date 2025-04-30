-- Separate password_hash from user table

-- !Ups
-- Create new user_passwords table
CREATE TABLE "user_passwords" (
    user_id       INTEGER PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE CASCADE
);

-- Migrate existing password hashes to new table
INSERT INTO "user_passwords" (user_id, password_hash)
SELECT id, password_hash FROM "user";

-- Remove password_hash column from user table
ALTER TABLE "user" DROP COLUMN password_hash;

-- !Downs
-- Add password_hash column back to user table
ALTER TABLE "user" ADD COLUMN password_hash VARCHAR(255);

-- Copy password hashes back to user table
UPDATE "user" u
SET password_hash = up.password_hash
FROM "user_passwords" up
WHERE u.id = up.user_id;

-- Set NOT NULL constraint on password_hash
ALTER TABLE "user" ALTER COLUMN password_hash SET NOT NULL;

-- Drop user_passwords table
DROP TABLE "user_passwords";