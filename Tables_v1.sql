CREATE DATABASE AmusementParkDB;
USE AmusementParkDB;

-- Drop old tables if they exist to prevent schema collisions
DROP TABLE IF EXISTS Login;
DROP TABLE IF EXISTS Customer;
DROP TABLE IF EXISTS Card;

CREATE TABLE IF NOT EXISTS Login (
    LoginID INT PRIMARY KEY AUTO_INCREMENT,
    Email VARCHAR(100) UNIQUE NOT NULL,
    Password VARCHAR(255) NOT NULL,      --255 because we use Bcrypt to encrypt the passwords in the db
    isAdmin BOOLEAN DEFAULT FALSE,
    CustomerID INT UNIQUE,
    Created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    CONSTRAINT fk_login_customer
        FOREIGN KEY (CustomerID) REFERENCES Customer(CustomerID)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Customer (
    CustomerID INT PRIMARY KEY AUTO_INCREMENT,
    First_Name VARCHAR(50) NOT NULL,
    Last_Name VARCHAR(50) NOT NULL,
    Type VARCHAR(30),
    Date_of_Birth DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS Card (
    CardID INT PRIMARY KEY AUTO_INCREMENT,
    Balance DECIMAL(10,2) DEFAULT 0.00,
    Points INT DEFAULT 0,
    CustomerID INT,
    CONSTRAINT fk_card_customer 
        FOREIGN KEY (CustomerID) REFERENCES Customer(CustomerID) 
        ON DELETE CASCADE
);

CREATE TABLE Staff (
    StaffID INT PRIMARY KEY AUTO_INCREMENT,
    First_Name VARCHAR(50),
    Last_Name VARCHAR(50),
    Title VARCHAR(80),
    Email VARCHAR(100) UNIQUE,
    Phone_Number VARCHAR(20),
    Salary DECIMAL(10,2),
    Reports_to INT,
    CONSTRAINT fk_staff_reports 
        FOREIGN KEY (Reports_to) REFERENCES Staff(StaffID) 
        ON DELETE SET NULL
);


CREATE TABLE Ride (
    RideID INT PRIMARY KEY AUTO_INCREMENT,
    Ride_Name VARCHAR(100),
    Status BOOL,
    OperatorID INT,
    CONSTRAINT fk_ride_operator 
        FOREIGN KEY (OperatorID) REFERENCES Staff(StaffID) 
        ON DELETE RESTRICT
);

CREATE TABLE Card_Payment (
    TransactionID INT PRIMARY KEY AUTO_INCREMENT,
    Amount DECIMAL(10,2),
    Date DATE,
    CardID INT,
    RideID INT,
    CONSTRAINT fk_cp_card 
        FOREIGN KEY (CardID) REFERENCES Card(CardID) 
        ON DELETE RESTRICT,
    CONSTRAINT fk_cp_ride 
        FOREIGN KEY (RideID) REFERENCES Ride(RideID) 
        ON DELETE RESTRICT
);

CREATE TABLE Job_Post (
    Job_PostID INT PRIMARY KEY AUTO_INCREMENT,
    Location_Name VARCHAR(100),
    StaffID INT,
    CONSTRAINT fk_job_staff 
        FOREIGN KEY (StaffID) REFERENCES Staff(StaffID) 
        ON DELETE CASCADE
);

CREATE TABLE Movie (
    MovieID INT PRIMARY KEY AUTO_INCREMENT,
    Title VARCHAR(150),
    Rating VARCHAR(10),
    Duration INT
);

CREATE TABLE Cinema (
    HallID INT PRIMARY KEY AUTO_INCREMENT,
    Capacity INT
);

CREATE TABLE Screening (
    ScreeningID INT PRIMARY KEY AUTO_INCREMENT,
    Screening_Time DATETIME,
    MovieID INT,
    HallID INT,
    CONSTRAINT fk_screening_movie 
        FOREIGN KEY (MovieID) REFERENCES Movie(MovieID) 
        ON DELETE RESTRICT,
    CONSTRAINT fk_screening_cinema 
        FOREIGN KEY (HallID) REFERENCES Cinema(HallID) 
        ON DELETE RESTRICT
);

CREATE TABLE Ticketing (
    TicketID INT PRIMARY KEY AUTO_INCREMENT,
    Amount DECIMAL(10,2),
    CardID INT,
    ScreeningID INT,
    CONSTRAINT fk_ticket_card 
        FOREIGN KEY (CardID) REFERENCES Card(CardID) 
        ON DELETE RESTRICT,
    CONSTRAINT fk_ticket_screening 
        FOREIGN KEY (ScreeningID) REFERENCES Screening(ScreeningID) 
        ON DELETE RESTRICT
);

CREATE TABLE Bowling_Booking (
    BookingID INT PRIMARY KEY AUTO_INCREMENT,
    Lane_Number INT,
    Time DATETIME,
    Amount DECIMAL(10,2),
    CardID INT,
    CONSTRAINT fk_bowling_card 
        FOREIGN KEY (CardID) REFERENCES Card(CardID) 
        ON DELETE RESTRICT
);

CREATE TABLE Food_Owner(
	Food_OwnerID INT PRIMARY KEY AUTO_INCREMENT,
    First_Name VARCHAR(50),
    Last_Name VARCHAR(50),
    Email VARCHAR(100),
    Phone VARCHAR(20)
);


CREATE TABLE Food_Stalls (
    Food_StallID INT PRIMARY KEY AUTO_INCREMENT,
    Name VARCHAR(100),
    Rent DECIMAL(10,2),
    Type VARCHAR(50),
    Establish_Date DATE,
    Opening_Time TIME,
    Closing_Time TIME,
    Food_OwnerID INT,
    CONSTRAINT fk_foodstall_owner 
        FOREIGN KEY (Food_OwnerID) REFERENCES Food_Owner(Food_OwnerID) 
        ON DELETE RESTRICT
);

CREATE TABLE Food_Payment (
    Food_PaymentID INT PRIMARY KEY AUTO_INCREMENT,
    Amount DECIMAL(10,2),
    Payment_Time DATETIME,
    Food_StallID INT,
    CONSTRAINT fk_fp_stall 
        FOREIGN KEY (Food_StallID) REFERENCES Food_Stalls(Food_StallID) 
        ON DELETE RESTRICT
);

