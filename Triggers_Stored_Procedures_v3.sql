DROP TRIGGER IF EXISTS trg_create_card;
DROP TRIGGER IF EXISTS trg_prevent_negative_card_balance;
DROP TRIGGER IF EXISTS trg_staff_salary_validation;
DROP TRIGGER IF EXISTS trg_prevent_double_booking;
DROP TRIGGER IF EXISTS trg_contract_date_validation;
DROP TRIGGER IF EXISTS trg_staff_user_exists;

DROP PROCEDURE IF EXISTS PurchaseRide;
DROP PROCEDURE IF EXISTS PurchaseTicket;
DROP PROCEDURE IF EXISTS StartBowlingSession;

-- ── SWITCH DELIMITER TO ALLOW MULTI-LINE PROCEDURAL CODE ───────────
DELIMITER $$

-- TRIGGER 1: AUTOMATED CARD PROVISIONING
-- Purpose: Ensures every single user gets a live wallet card instantly upon account creation.
DELIMITER $$

CREATE TRIGGER trg_create_card
AFTER INSERT ON User
FOR EACH ROW
BEGIN
    INSERT INTO Card (UserID, Balance, LoyaltyPoints, IsActive)
    VALUES (NEW.UserID, 0.00, 0, TRUE);
END$$

-- TRIGGER 2: PREVENT NEGATIVE CARD BALANCE
CREATE TRIGGER trg_prevent_negative_card_balance
BEFORE UPDATE ON Card
FOR EACH ROW
BEGIN
    IF NEW.Balance < 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Card balance cannot be negative';
    END IF;
END$$

-- TRIGGER 3: PREVENT NEGATIVE SALARIES FOR STAFF
CREATE TRIGGER trg_staff_salary_validation
BEFORE UPDATE ON Staff
FOR EACH ROW
BEGIN
    IF NEW.Salary < 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Salary cannot be negative';
    END IF;

    IF NEW.Salary > 10000000 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Salary exceeds system limit';
    END IF;
END$$

-- TRIGGER 4: PREVENT DOUBLE BOOKING (SAFETY TRIGGER)

CREATE TRIGGER trg_prevent_double_booking
BEFORE INSERT ON Ticket
FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1 FROM Ticket
        WHERE ScreeningID = NEW.ScreeningID
        AND SeatID = NEW.SeatID
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Seat already booked';
    END IF;
END$$

-- TRIGGER 5: PREVENT INVALID CONTRACT DATES (SAFETY TRIGGER)

CREATE TRIGGER trg_contract_date_validation
BEFORE INSERT ON Contract
FOR EACH ROW
BEGIN
    IF NEW.EndDate <= NEW.StartDate THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Invalid contract duration';
    END IF;
END $$

--  TRIGGER 6: PREVENT ORPHANED STAFF RECORDS (SAFETY TRIGGER)

CREATE TRIGGER trg_staff_user_exists
BEFORE INSERT ON Staff
FOR EACH ROW
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM User WHERE UserID = NEW.UserID
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'User does not exist';
    END IF;
END$$




-- STORED PROCEDURES AHEAD

-- STORED PROCEDURE 1: PURCHASE RIDE
-- Purpose: Properly ensure that a ride can be purchased by the card provided and then track it on the ledger
CREATE PROCEDURE PurchaseRide(
    IN pCardID INT,
    IN pRideID INT
)
BEGIN
    DECLARE vBalance DECIMAL(10,2);
    DECLARE vPrice DECIMAL(10,2);

    START TRANSACTION;

    SELECT Balance INTO vBalance
    FROM Card
    WHERE CardID = pCardID
    FOR UPDATE;         -- Acquire an exclusive lock

    SELECT RidePrice INTO vPrice
    FROM Ride
    WHERE RideID = pRideID
    AND IsOperational = TRUE;       -- Ride must be operational

    IF vPrice IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Ride unavailable';
    END IF;

    IF vBalance < vPrice THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Insufficient balance';
    END IF;

    UPDATE Card
    SET Balance = Balance - vPrice
    WHERE CardID = pCardID;

    INSERT INTO RideUsage (RideID, CardID)
    VALUES (pRideID, pCardID);

    INSERT INTO CardTransaction (
        CardID, Amount, TransactionType, ReferenceID
    )
    VALUES (
        pCardID, vPrice, 'RIDE', LAST_INSERT_ID()       -- LAST_INSERT_ID() Inserts the latest Ride_UsageID not RideID since the latest insert before this is in Ride_Usage
    );                                                  -- This is also safe due to session isolation (for more info check the internet)

    COMMIT;
END$$

-- STORED PROCEDURE 2: PURCHASE TICKET
-- Purpose: Properly purchase a ticket by checking balance in the card and checking if ticket being generated is valid

CREATE PROCEDURE PurchaseTicket(
    IN pCardID INT,
    IN pScreeningID INT,
    IN pSeatID INT
)
BEGIN
    DECLARE vBalance DECIMAL(10,2);
    DECLARE vPrice DECIMAL(10,2);

    -- To prevent the sql generic error 1062 duplicate entry.
    -- (Added this in case of a simultaneous click which messes with the If Exists line below causing one transaction to process and the other to fail due to a microsecond difference)
    -- It remains active even when inside the TRANSACTION block just waiting for sql error 1062 specifically
    DECLARE EXIT HANDLER FOR 1062
    BEGIN
        ROLLBACK; -- If a duplicate seat insert happens, undo the balance deduction!
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Seat already booked';
    END;

    START TRANSACTION;

    -- In case of matching seat (To prevent over booking which is handled in the db schema but its a safety net in case ui glitches or is slow)
    IF EXISTS (
        SELECT 1 FROM Ticket
        WHERE ScreeningID = pScreeningID
        AND SeatID = pSeatID
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Seat already booked';
    END IF;

    SELECT Balance INTO vBalance
    FROM Card
    WHERE CardID = pCardID
    FOR UPDATE;             -- Get an exclusive lock since we are gonna update balance (will be bad if someone interferes)

    SELECT TicketPrice INTO vPrice
    FROM Screening
    WHERE ScreeningID = pScreeningID;       -- Stores the calculated ticket price from screeningID which is baseMoviePrice + HallFee

    -- Insufficient balance in card case
    IF vBalance < vPrice THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Insufficient balance';
    END IF;

    UPDATE Card
    SET Balance = Balance - vPrice
    WHERE CardID = pCardID;

    INSERT INTO Ticket (
        ScreeningID, SeatID, CardID, AmountPaid
    )
    VALUES (
        pScreeningID, pSeatID, pCardID, vPrice
    );

    INSERT INTO CardTransaction (
        CardID, Amount, TransactionType, ReferenceID
    )
    VALUES (
        pCardID, vPrice, 'CINEMA', LAST_INSERT_ID()     -- This will insert the latest TicketID from Tickets table into ReferenceID of CardTransaction table
    );

    COMMIT;
END$$


-- STORED PROCEDURE 3: START BOWLING SESSION
-- Purpose: Ensure proper bowling session after assesing card balance and if the lane is free and bill at the end

CREATE PROCEDURE StartBowlingSession(
    IN pCardID INT,
    IN pLaneID INT,
    IN pDurationMinutes INT  -- Changed from hours to minutes for maximum flexibility
)
BEGIN
    DECLARE vBalance DECIMAL(10,2);
    DECLARE vRate DECIMAL(10,2);
    DECLARE vAmount DECIMAL(10,2);

    START TRANSACTION;

    -- Secure the card balance
    SELECT Balance INTO vBalance
    FROM Card
    WHERE CardID = pCardID
    FOR UPDATE;

    -- Secure the lane and verify it is operational and open
    SELECT HourlyRate INTO vRate
    FROM BowlingLane  
    WHERE LaneID = pLaneID
    AND IsOperational = TRUE
    AND IsAvailable = TRUE
    FOR UPDATE;  -- Lock the lane row so two people can't claim it simultaneously

    -- If lane is unavailable or doesn't exist
    IF vRate IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Lane unavailable';
    END IF;

    -- Calculate prorated cost: (HourlyRate * Minutes) / 60 
    -- ROUND(..., 2) ensures we don't get messy fractional pennies (e.g., $15.33333)
    SET vAmount = ROUND((vRate * pDurationMinutes) / 60, 2);

    -- Insufficient balance safety check
    IF vBalance < vAmount THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Insufficient balance';
    END IF;

    -- Deduct the calculated prorated amount
    UPDATE Card
    SET Balance = Balance - vAmount
    WHERE CardID = pCardID;

    -- Flip the lane status to occupied
    UPDATE BowlingLane
    SET IsAvailable = FALSE
    WHERE LaneID = pLaneID;

    -- Log the session using MINUTE intervals instead of HOUR
    INSERT INTO BowlingSession (
        LaneID, CardID, StartTime, EndTime, AmountPaid
    )
    VALUES (
        pLaneID,
        pCardID,
        NOW(),
        DATE_ADD(NOW(), INTERVAL pDurationMinutes MINUTE), -- Adds precise minutes to current time
        vAmount
    );

    -- Link financial ledger entry using the generated Bowling Session ID
    INSERT INTO CardTransaction (
        CardID, Amount, TransactionType, ReferenceID
    )
    VALUES (
        pCardID, vAmount, 'BOWLING', LAST_INSERT_ID()   -- LAST_INSERT_ID() Gives SessionID from BowlingSession Table
    );

    COMMIT;
END$$

DELIMITER ;