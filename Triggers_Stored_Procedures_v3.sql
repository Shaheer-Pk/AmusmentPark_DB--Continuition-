-- Automated Card Trigger: Seeds a dynamic digital wallet automatically when a Customer profile is created
DELIMITER $$
CREATE TRIGGER after_customer_insert
AFTER INSERT ON Customer
FOR EACH ROW
BEGIN
    INSERT INTO Card (Balance, Points, CustomerID)
    VALUES (0.00, 0, NEW.CustomerID);
END$$
DELIMITER ;