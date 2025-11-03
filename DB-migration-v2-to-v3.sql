-- Database Migration from Version 2 to Version 3
-- Date: November 3, 2025
-- Purpose: Add user role tracking fields to items table

-- Add new columns for tracking user roles
ALTER TABLE items ADD COLUMN lost_user_id INTEGER;
ALTER TABLE items ADD COLUMN found_user_id INTEGER;
ALTER TABLE items ADD COLUMN returned_user_id INTEGER;

-- Description of new fields:
-- lost_user_id: ID of user who lost the item (owner)
-- found_user_id: ID of user who found the item
-- returned_user_id: ID of user who received the item back after handover

-- Example data after migration:
-- Item created as "lost" by User A:
--   user_id = A, lost_user_id = A, found_user_id = NULL, returned_user_id = NULL
--
-- User B finds the item and updates status to "found":
--   user_id = A, lost_user_id = A, found_user_id = B, returned_user_id = NULL
--
-- User A confirms handover via QR code:
--   user_id = A, lost_user_id = A, found_user_id = B, returned_user_id = A, status = "returned"

-- Notes:
-- - These fields are automatically populated by the API when creating/updating items
-- - If status = "lost" -> lost_user_id is set to current user
-- - If status = "found" -> found_user_id is set to current user
-- - If handover confirmed -> returned_user_id is set to receiver
