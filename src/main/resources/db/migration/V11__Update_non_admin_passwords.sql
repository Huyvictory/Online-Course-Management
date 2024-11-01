-- V10__Update_non_admin_passwords.sql

-- Create a temporary function to generate BCrypt hash
DELIMITER //

DROP FUNCTION IF EXISTS bcrypt_hash//
CREATE FUNCTION bcrypt_hash(password VARCHAR(255)) RETURNS VARCHAR(255)
    DETERMINISTIC
BEGIN
    -- BCrypt hash for password '123456'
    -- This is equivalent to BCryptPasswordEncoder.encode("123456")
RETURN '$2a$10$wtXh4Q7bsC.6y4D.yKvbl.X9D4TNeSXz70rCz63PVtb9xoyxC0Jly';
END//

DELIMITER ;

-- Update passwords for all non-admin users
UPDATE users u
    LEFT JOIN user_roles ur ON u.id = ur.user_id
    LEFT JOIN roles r ON ur.role_id = r.id
    SET u.password_hash = bcrypt_hash('123456')
WHERE u.email != 'admin@gmail.com'  -- Exclude admin user
  AND (r.name != 'ADMIN' OR r.name IS NULL);  -- Extra safety check for non-admin users

-- Log the number of updated users
SELECT CONCAT('Updated passwords for ', ROW_COUNT(), ' users') as migration_log;

-- Clean up
DROP FUNCTION IF EXISTS bcrypt_hash;