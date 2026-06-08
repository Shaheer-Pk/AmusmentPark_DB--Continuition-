
USE AmusementParkDB;

-- =========================
-- RBAC SEED DATA
-- =========================

INSERT INTO Role (RoleName, Description) VALUES
-- Base roles
('Customer', 'Regular amusement park customer'),
('Vendor', 'Food stall operator'),
('Staff', 'Base employee role'),

-- Staff specializing in rides
('RideOperator', 'Operates rides and manages ride status'),
('RideManager', 'Can create or delete rides and can even operate rides'),

-- Staff specializing in finances
('FinanceManager', 'Handles revenue, salaries and finances'),

-- Staff specializing in cinema operations
('CinemaOperator', 'Handles all operation regarding cinema, tickets, seating and movies'),

-- Staff specializing in bowling operations
('BowlingOperator', 'Handles all operations regarding bowling lanes and sessions'),

-- Staff specializing in managing user cards and blacklisting naughty cards
('CardOperator', 'Handles all operations regarding user cards and its balance/loyalty-points'),

-- The manager of all staff members
('StaffManager', 'Handles employee management'),

-- The manager of all vendors
('VendorManager', 'Manages all contracts and dealings with private-vendors'),   -- This person is a staff member

-- The big boss
('Admin', 'Full system administrator')
;

-- ============================================================
-- PERMISSIONS
-- ============================================================

INSERT INTO Permission (PermissionName, Description) VALUES

-- Profile
('VIEW_PROFILE', 'View own profile'),       
('EDIT_PROFILE', 'Edit own profile'),       -- Cannot change their user_id but can update their name, password, email etc associated to that id

-- Card
('VIEW_CARD', 'View card details'),
('RECHARGE_CARD', 'Recharge card'),
('UPDATE_CARD_LOYALTYPOINTS', 'Update card loyalty points'),
('UPDATE_CARD_STATUS', 'Update card status to tell if its blacklisted or not'),

-- Ride
('VIEW_RIDES', 'View rides'),
('PURCHASE_RIDE', 'Purchase ride'),
('CREATE_RIDE', 'Create ride'),
('UPDATE_RIDE', 'Update ride'),
('DELETE_RIDE', 'Delete ride'),
('ASSIGN_RIDE_OPERATOR', 'View and Edit RideOperators assigned to rides'),

-- Ride Operations
('VIEW_ASSIGNED_RIDES', 'View assigned rides'),
('UPDATE_RIDE_STATUS', 'Mark rides operational or unavailable'),
('VIEW_RIDE_USAGE', 'View ride usage statistics'),

-- Cinema
('VIEW_MOVIES', 'View movies'),
('PURCHASE_TICKET', 'Purchase cinema ticket'),
('MANAGE_MOVIES', 'Manage movies'),
('MANAGE_SCREENINGS', 'Manage screenings'),
('MANAGE_SEATS', 'Manage seats within halls'),
('MANAGE_CINEMAHALLS', 'Manage number of cinema halls and their fee'),

-- Bowling
('VIEW_BOWLING', 'View bowling lanes'),
('BOOK_BOWLING', 'Book bowling lane'),
('MANAGE_BOWLING', 'Manage bowling operations'),    -- Both lane and session 

-- Vendor
('VIEW_VENDOR_CONTRACT', 'View vendor contracts'),
('VIEW_STALL', 'View stall information'),       -- General stall information like name, open and close hours
('MANAGE_STALL', 'Manage stall information'),   -- Meant for use by the vendor so he can handle his private stall information
('VIEW_VENDOR_REVENUE', 'View vendor revenue'),
('MANAGE_VENDOR_CONTRACT', 'Manager vendor contracts'),  -- by (Status ENUM) meant for staff members responsible for contracts (not vendors)

-- Staff Management
('VIEW_STAFF', 'View staff records'),
('CREATE_STAFF', 'Create staff'),
('UPDATE_STAFF', 'Update staff'),
('DELETE_STAFF', 'Delete staff'),

-- Role Management
('ASSIGN_ROLE', 'Assign existing roles'),
('MANAGE_ROLES', 'Create and manage roles'),
('MANAGE_PERMISSIONS', 'Create and manage permissions'),

-- Finance
('VIEW_REVENUE', 'View revenue'),
('VIEW_LEDGER', 'View financial ledger'),
('EDIT_SALARY', 'Edit salaries'),
('PROCESS_REFUND', 'Process refunds'),  -- Just a fancy way of saying manage ledger
('VIEW_CONTRACT_REVENUE', 'View contract revenue'),

-- Reports
('VIEW_REPORTS', 'View reports'),
('EXPORT_REPORTS', 'Export reports'),
('VIEW_ANALYTICS', 'View analytics'),

-- System
('SYSTEM_ADMIN', 'Full system administration'),
('VIEW_SYSTEM_AUDIT', 'View audit information')
;

-- ============================================================
-- ROLE -> PERMISSION MAPPINGS
-- ============================================================

-- CUSTOMER

INSERT INTO RolePermission
SELECT r.RoleID, p.PermissionID
FROM Role r, Permission p
WHERE r.RoleName='Customer'
AND p.PermissionName IN (
'VIEW_PROFILE',
'EDIT_PROFILE',
'VIEW_CARD',
'RECHARGE_CARD',
'VIEW_RIDES',
'PURCHASE_RIDE',
'VIEW_MOVIES',
'PURCHASE_TICKET',
'VIEW_BOWLING',
'BOOK_BOWLING',
'VIEW_STALL'
);

-- VENDOR

INSERT INTO RolePermission
SELECT r.RoleID, p.PermissionID
FROM Role r, Permission p
WHERE r.RoleName='Vendor'
AND p.PermissionName IN (
'VIEW_PROFILE',
'EDIT_PROFILE',
'VIEW_VENDOR_CONTRACT',
'VIEW_STALL',
'MANAGE_STALL',
'VIEW_VENDOR_REVENUE'
);

-- STAFF
-- Basic skeleton for a staff (Every specialized staff at their roots are a staff member including admin)
INSERT INTO RolePermission
SELECT r.RoleID, p.PermissionID
FROM Role r, Permission p
WHERE r.RoleName='Staff'
AND p.PermissionName IN (
'VIEW_PROFILE',
'EDIT_PROFILE'
);

-- RIDE OPERATOR

INSERT INTO RolePermission
SELECT r.RoleID, p.PermissionID
FROM Role r, Permission p
WHERE r.RoleName='RideOperator'
AND p.PermissionName IN (
'VIEW_ASSIGNED_RIDES',
'UPDATE_RIDE_STATUS',
'VIEW_RIDE_USAGE'
);

-- RIDE MANAGER

-- He has all powers as ride-operator except for
-- VIEW_ASSIGNED_RIDES as he is meant to oversee
-- ALL operators
INSERT INTO RolePermission
SELECT r.RoleID, p.PermissionID
FROM Role r, Permission p
WHERE r.RoleName = 'RideManager'
AND p.PermissionName IN (
    'CREATE_RIDE',
    'UPDATE_RIDE',
    'DELETE_RIDE',
    'UPDATE_RIDE_STATUS',
    'VIEW_RIDE_USAGE',
    'ASSIGN_RIDE_OPERATOR'
);

-- CINEMA OPERATOR

INSERT INTO RolePermission
SELECT r.RoleID, p.PermissionID
FROM Role r, Permission p
WHERE r.RoleName='CinemaOperator'
AND p.PermissionName IN (
'VIEW_MOVIES',
'MANAGE_MOVIES',
'MANAGE_SCREENINGS',
'MANAGE_SEATS',
'MANAGE_CINEMAHALLS'
);

-- BOWLING OPERATOR

INSERT INTO RolePermission
SELECT r.RoleID, p.PermissionID
FROM Role r, Permission p
WHERE r.RoleName='BowlingOperator'
AND p.PermissionName IN (
'MANAGE_BOWLING'
);

-- FINANCE MANAGER

INSERT INTO RolePermission
SELECT r.RoleID, p.PermissionID
FROM Role r, Permission p
WHERE r.RoleName='FinanceManager'
AND p.PermissionName IN (
'VIEW_REVENUE',
'VIEW_LEDGER',
'EDIT_SALARY',
'PROCESS_REFUND',
'VIEW_CONTRACT_REVENUE',
'VIEW_REPORTS',
'EXPORT_REPORTS'
);

-- CARD OPERATOR

INSERT INTO RolePermission
SELECT r.RoleID, p.PermissionID
FROM Role r, Permission p
WHERE r.RoleName='CardOperator'
AND p.PermissionName IN (
    'UPDATE_CARD_LOYALTYPOINTS',
    'UPDATE_CARD_STATUS'
);

-- STAFF MANAGER

INSERT INTO RolePermission
SELECT r.RoleID, p.PermissionID
FROM Role r, Permission p
WHERE r.RoleName='StaffManager'
AND p.PermissionName IN (
'VIEW_STAFF',
'CREATE_STAFF',
'UPDATE_STAFF',
'DELETE_STAFF',
'ASSIGN_ROLE'
);

-- ADMIN

INSERT INTO RolePermission
SELECT r.RoleID, p.PermissionID
FROM Role r, Permission p
WHERE r.RoleName='Admin'
AND p.PermissionName NOT IN (
'VIEW_ASSIGNED_RIDES'
);

-- =========================
-- SEEDING INITIAL ADMIN
-- =========================

-- 1. Create the Bootstrap User Profile
INSERT INTO User (UserID, FirstName, LastName, PhoneNumber, DateOfBirth) 
VALUES (1, 'Park', 'Admin', '000-000-0000', '1990-01-01');

-- 2. Create the Auth Entry
-- Password is: 123456789 (its been hashed using JBCrypt)
INSERT INTO Login (Email, Password, UserID) 
VALUES ('admin@amusementpark.com', '$2a$10$2MCNvUwZjV7Rsd23JJMZOem.uZ.clSl.9692OC38IW0U1KNA3uFWC', 1);

-- 3. Assign the 'Admin' Role to this newly generated User ID (Assuming Admin RoleID is 11)
-- Tip: It's safer to use a subquery to grab the exact RoleID dynamically

INSERT INTO UserRole (UserID, RoleID)
SELECT 1, RoleID FROM Role WHERE RoleName = 'Admin'        -- Maps the hardcoded UserID (1) with the dynamically retrieved RoleID
UNION
SELECT 1, RoleID FROM Role WHERE RoleName = 'Staff';        -- The boot-strapped admin is also a staff member 

-- Its better to use UNION as this sends it as one INSERT reducing overhead, sort of like VALUES ((val1), (val2),....) 

INSERT INTO Staff (UserID, Title, Salary) VALUES
(1, 'Administrator', 1000000);

-- =========================
-- STATIC ENTERTAINMENT DATA
-- =========================

INSERT INTO Movie (Title, Genre, DurationMinutes, Rating, BasePrice) VALUES
('Interstellar', 'Sci-Fi', 169, 'PG-13', 500),
('Avengers: Endgame', 'Action', 181, 'PG-13', 600),
('Inside Out', 'Animation', 95, 'U', 300);

INSERT INTO CinemaHall (HallNumber, Capacity, RoomFee, HallType) VALUES
(1, 100, 200, 'Standard'),
(2, 150, 300, 'Premium');

-- Seats for Hall 1
INSERT INTO Seat (HallID, SeatNumber)
SELECT 1, CONCAT('A', n)
FROM (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) x;

-- Seats for Hall 2
INSERT INTO Seat (HallID, SeatNumber)
SELECT 2, CONCAT('B', n)
FROM (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) x;

INSERT INTO Ride (RideName, RidePrice, IsOperational) VALUES
('Roller Coaster', 800, TRUE),
('Haunted House', 500, TRUE),
('Ferris Wheel', 300, TRUE);

INSERT INTO BowlingLane (LaneNumber, HourlyRate, IsOperational, IsAvailable) VALUES
(1, 1000, TRUE, TRUE),
(2, 1200, TRUE, TRUE);

-- =========================
-- FOOD & LEASING DATA
-- =========================

INSERT INTO FoodPlace (Name, LocationDetails, BaseRent) VALUES
('North Food Court', 'Near entrance gate', 50000),
('Lake Side Food Area', 'Beside lake zone', 70000);

