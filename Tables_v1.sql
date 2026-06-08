CREATE DATABASE IF NOT EXISTS AmusementParkDB;
USE AmusementParkDB;

SET FOREIGN_KEY_CHECKS = 0;

/* =========================
   DROP ALL TABLES CLEANLY
========================= */

DROP TABLE IF EXISTS RolePermission;
DROP TABLE IF EXISTS UserRole;
DROP TABLE IF EXISTS Permission;
DROP TABLE IF EXISTS Role;

DROP TABLE IF EXISTS CardTransaction;
DROP TABLE IF EXISTS BowlingSession;
DROP TABLE IF EXISTS BowlingLane;

DROP TABLE IF EXISTS Ticket;
DROP TABLE IF EXISTS Screening;
DROP TABLE IF EXISTS Seat;
DROP TABLE IF EXISTS CinemaHall;
DROP TABLE IF EXISTS Movie;

DROP TABLE IF EXISTS RideUsage;
DROP TABLE IF EXISTS Ride;
DROP TABLE IF EXISTS RideOperatorAssignment;

DROP TABLE IF EXISTS FoodStall;
DROP TABLE IF EXISTS Contract;
DROP TABLE IF EXISTS FoodPlace;

DROP TABLE IF EXISTS Staff;

DROP TABLE IF EXISTS Card;
DROP TABLE IF EXISTS Login;
DROP TABLE IF EXISTS User;

SET FOREIGN_KEY_CHECKS = 1;

-- =========================================================
-- 1. IDENTITY & AUTHENTICATION LAYER
-- =========================================================

CREATE TABLE User (
    UserID INT PRIMARY KEY AUTO_INCREMENT,
    FirstName VARCHAR(50) NOT NULL,
    LastName VARCHAR(50) NOT NULL,
    PhoneNumber VARCHAR(20) NOT NULL,
    DateOfBirth DATE NOT NULL
);

CREATE TABLE Login (
    LoginID INT PRIMARY KEY AUTO_INCREMENT,
    Email VARCHAR(100) UNIQUE NOT NULL,
    Password VARCHAR(255) NOT NULL,     -- To handle JBCrypt hashing size is 255
    UserID INT UNIQUE NOT NULL,
    CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (UserID)
        REFERENCES User(UserID)
        ON DELETE CASCADE
);

CREATE TABLE Card (
    CardID INT PRIMARY KEY AUTO_INCREMENT,
    UserID INT NOT NULL UNIQUE,
    Balance DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    LoyaltyPoints INT DEFAULT 0,
    IsActive BOOLEAN DEFAULT TRUE,

    FOREIGN KEY (UserID)
        REFERENCES User(UserID)
        ON DELETE CASCADE
);

-- =========================================================
-- 2. RBAC SYSTEM
-- =========================================================

CREATE TABLE Role (
    RoleID INT PRIMARY KEY AUTO_INCREMENT,
    RoleName VARCHAR(50) UNIQUE NOT NULL,
    Description TEXT
);

CREATE TABLE Permission (
    PermissionID INT PRIMARY KEY AUTO_INCREMENT,
    PermissionName VARCHAR(100) UNIQUE NOT NULL,
    Description TEXT
);

CREATE TABLE UserRole (
    UserID INT NOT NULL,
    RoleID INT NOT NULL,

    PRIMARY KEY (UserID, RoleID),

    FOREIGN KEY (UserID)
        REFERENCES User(UserID)
        ON DELETE CASCADE,

    FOREIGN KEY (RoleID)
        REFERENCES Role(RoleID)
        ON DELETE CASCADE
);

CREATE TABLE RolePermission (
    RoleID INT NOT NULL,
    PermissionID INT NOT NULL,

    PRIMARY KEY (RoleID, PermissionID),

    FOREIGN KEY (RoleID)
        REFERENCES Role(RoleID)
        ON DELETE CASCADE,

    FOREIGN KEY (PermissionID)
        REFERENCES Permission(PermissionID)
        ON DELETE CASCADE
);

-- =========================================================
-- 3. STAFF MODULE (business entity, NOT auth entity)
-- =========================================================

CREATE TABLE Staff (
    StaffID INT PRIMARY KEY AUTO_INCREMENT,
    UserID INT UNIQUE NOT NULL,
    Title VARCHAR(80) NOT NULL,
    Salary DECIMAL(10,2) NOT NULL,
    ReportsTo INT DEFAULT NULL,

    FOREIGN KEY (UserID)
        REFERENCES User(UserID)
        ON DELETE CASCADE,

    FOREIGN KEY (ReportsTo)
        REFERENCES Staff(StaffID)
        ON DELETE SET NULL
);

-- =========================================================
-- 4. FOOD & COMMERCIAL LEASING
-- =========================================================

CREATE TABLE FoodPlace (
    PlaceID INT PRIMARY KEY AUTO_INCREMENT,
    Name VARCHAR(100) NOT NULL,
    LocationDetails TEXT,
    BaseRent DECIMAL(10,2) NOT NULL
);

CREATE TABLE Contract (
    ContractID INT PRIMARY KEY AUTO_INCREMENT,
    StartDate DATE DEFAULT (CURRENT_DATE),
    EndDate DATE NOT NULL,
    ActualRent DECIMAL(10,2) NOT NULL,
    Status ENUM('Active','Expired','Pending') DEFAULT 'Pending',

    VendorID INT NOT NULL,
    PlaceID INT NOT NULL,

    FOREIGN KEY (VendorID)
        REFERENCES User(UserID)
        ON DELETE RESTRICT,

    FOREIGN KEY (PlaceID)
        REFERENCES FoodPlace(PlaceID)
        ON DELETE RESTRICT
);

CREATE TABLE FoodStall (
    StallID INT PRIMARY KEY AUTO_INCREMENT,
    Name VARCHAR(100) NOT NULL,
    Type VARCHAR(50) NOT NULL,
    OpeningTime TIME,
    ClosingTime TIME,
    ContractID INT NOT NULL,

    FOREIGN KEY (ContractID)
        REFERENCES Contract(ContractID)
        ON DELETE RESTRICT
);

-- =========================================================
-- 5. RIDE SYSTEM
-- =========================================================

CREATE TABLE Ride (
    RideID INT PRIMARY KEY AUTO_INCREMENT,
    RideName VARCHAR(100) NOT NULL,
    RidePrice DECIMAL(10,2) NOT NULL,
    IsOperational BOOLEAN DEFAULT TRUE
);

-- A junction table to rides and their assigned staff (RideOperators)
CREATE TABLE RideOperatorAssignment (
    StaffID  INT NOT NULL,
    RideID   INT NOT NULL,
    PRIMARY KEY (StaffID, RideID),
    FOREIGN KEY (StaffID) REFERENCES Staff(StaffID) ON DELETE CASCADE,
    FOREIGN KEY (RideID)  REFERENCES Ride(RideID)   ON DELETE CASCADE
);

CREATE TABLE RideUsage (
    RideUsageID INT PRIMARY KEY AUTO_INCREMENT,
    RideID INT NOT NULL,
    CardID INT NOT NULL,
    UsedAt DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (RideID)
        REFERENCES Ride(RideID),

    FOREIGN KEY (CardID)
        REFERENCES Card(CardID)
);

-- =========================================================
-- 6. CINEMA SYSTEM
-- =========================================================

CREATE TABLE Movie (
    MovieID INT PRIMARY KEY AUTO_INCREMENT,
    Title VARCHAR(150) NOT NULL,
    Genre VARCHAR(50),
    DurationMinutes INT NOT NULL,
    Rating VARCHAR(20),
    BasePrice DECIMAL(10,2) NOT NULL
);

CREATE TABLE CinemaHall (
    HallID INT PRIMARY KEY AUTO_INCREMENT,
    HallNumber INT UNIQUE NOT NULL,
    Capacity INT NOT NULL,
    RoomFee DECIMAL(10,2) NOT NULL,
    HallType VARCHAR(30)
);

CREATE TABLE Seat (
    SeatID INT PRIMARY KEY AUTO_INCREMENT,
    HallID INT NOT NULL,
    SeatNumber VARCHAR(10) NOT NULL,

    FOREIGN KEY (HallID)
        REFERENCES CinemaHall(HallID)
        ON DELETE CASCADE,

    UNIQUE(HallID, SeatNumber)
);

CREATE TABLE Screening (
    ScreeningID INT PRIMARY KEY AUTO_INCREMENT,
    MovieID INT NOT NULL,
    HallID INT NOT NULL,
    StartTime DATETIME NOT NULL,
    TicketPrice DECIMAL(10,2) NOT NULL,

    FOREIGN KEY (MovieID)
        REFERENCES Movie(MovieID),

    FOREIGN KEY (HallID)
        REFERENCES CinemaHall(HallID)
);

CREATE TABLE Ticket (
    TicketID INT PRIMARY KEY AUTO_INCREMENT,
    ScreeningID INT NOT NULL,
    SeatID INT NOT NULL,
    CardID INT NOT NULL,
    PurchasedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    AmountPaid DECIMAL(10,2) NOT NULL,

    FOREIGN KEY (ScreeningID)
        REFERENCES Screening(ScreeningID),

    FOREIGN KEY (SeatID)
        REFERENCES Seat(SeatID),

    FOREIGN KEY (CardID)
        REFERENCES Card(CardID),

    UNIQUE(ScreeningID, SeatID)
);

-- =========================================================
-- 7. BOWLING SYSTEM
-- =========================================================

CREATE TABLE BowlingLane (
    LaneID INT PRIMARY KEY AUTO_INCREMENT,
    LaneNumber INT UNIQUE NOT NULL,
    HourlyRate DECIMAL(10,2) NOT NULL,
    IsOperational BOOLEAN DEFAULT TRUE,
    IsAvailable BOOLEAN DEFAULT TRUE
);

CREATE TABLE BowlingSession (
    SessionID INT PRIMARY KEY AUTO_INCREMENT,
    LaneID INT NOT NULL,
    CardID INT NOT NULL,
    StartTime DATETIME NOT NULL,
    EndTime DATETIME NOT NULL,
    AmountPaid DECIMAL(10,2) NOT NULL,

    FOREIGN KEY (LaneID)
        REFERENCES BowlingLane(LaneID),

    FOREIGN KEY (CardID)
        REFERENCES Card(CardID)
);

-- =========================================================
-- 8. FINANCIAL LEDGER
-- =========================================================

CREATE TABLE CardTransaction (
    TransactionID INT PRIMARY KEY AUTO_INCREMENT,
    CardID INT NOT NULL,
    Amount DECIMAL(10,2) NOT NULL,

    TransactionType ENUM('RECHARGE','RIDE','CINEMA','BOWLING') NOT NULL,
    ReferenceID INT NULL,

    CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (CardID)
        REFERENCES Card(CardID)
        ON DELETE RESTRICT
);