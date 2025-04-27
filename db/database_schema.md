# FixIt Database Schema

This document outlines the database schema for the FixIt mobile application, designed to manage equipment faults in an automotive parts factory.

## Tables

### 1. Users
- `user_id` (INT, PRIMARY KEY, AUTO_INCREMENT) Unique user ID.
- `name` (VARCHAR(100)) User's full name.
- `email` (VARCHAR(100), UNIQUE) User's email.
- `password` (VARCHAR(255)) Encrypted password.
- `role` (ENUM('Operator', 'Technician', 'Manager')) User role.
- `profile_photo` (VARCHAR(255), NULLABLE) Path to the user’s profile photo (optional).
- `phone` (VARCHAR(20), NULLABLE) User’s phone number (optional).

### 2. Equipment
- `equipment_id` (INT, PRIMARY KEY, AUTO_INCREMENT) Unique equipment ID.
- `name` (VARCHAR(100)) Equipment name (e.g., CNC #1).
- `type` (VARCHAR(50)) Equipment type (e.g., Industrial Machine).
- `location` (VARCHAR(100)) Location in the factory (e.g., Line 1).

### 3. Faults
- `fault_id` (INT, PRIMARY KEY, AUTO_INCREMENT) Unique fault ID.
- `equipment_id` (INT, FOREIGN KEY → Equipment.equipment_id) Affected equipment.
- `reported_by` (INT, FOREIGN KEY → Users.user_id) Reporter of the fault.
- `assigned_to` (INT, FOREIGN KEY → Users.user_id, NULLABLE) Assigned technician.
- `description` (TEXT) Fault description.
- `photo` (VARCHAR(255), NULLABLE) Path to fault photo.
- `urgency` (ENUM('Low', 'Medium', 'High')) Fault priority.
- `location` (VARCHAR(100)) Fault location.
- `status` (ENUM('Pending', 'In Progress', 'Resolved')) Fault status.
- `reported_at` (DATETIME) Report timestamp.
- `resolved_at` (DATETIME, NULLABLE) Resolution timestamp.
- `sync_status` (ENUM('Synced', 'Pending')) Sync status for offline mode.

### 4. Messages
- `message_id` (INT, PRIMARY KEY, AUTO_INCREMENT) Unique message ID.
- `fault_id` (INT, FOREIGN KEY → Faults.fault_id) Related fault.
- `sender_id` (INT, FOREIGN KEY → Users.user_id) Message sender.
- `message` (TEXT) Message content.
- `sent_at` (DATETIME) Message timestamp.