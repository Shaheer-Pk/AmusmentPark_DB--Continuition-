-- No need for any hard-coded inserts related to Login, Customer And Card, as they are all automated and handled with the new signup system

INSERT INTO Cinema (Capacity) VALUES
(120),
(200),
(150),
(250);


INSERT INTO Movie (Title, Rating, Duration) VALUES
('Guardians of the Galaxy', 'PG-13', 121),
('The Dark Knight',         'PG-13', 152),
('Inception',               'PG-13', 148),
('Interstellar',            'PG',    169),
('Avengers: Endgame',       'PG-13', 181),
('Spider-Man: No Way Home', 'PG-13', 148),
('Top Gun: Maverick',       'PG-13', 131),
('Doctor Strange',          'PG-13', 115),
('Black Panther',           'PG-13', 134),
('The Lion King',           'PG',    118),
('Dune',                    'PG-13', 155),
('The Conjuring',           'R',     112);


INSERT INTO Staff (First_Name, Last_Name, Title, Email, Phone_Number, Salary, Reports_to) VALUES
-- Management
('Muhammad',  'Saad',  'General Manager',    'm.saad@park.com',   '0300-1234567', 150000, NULL),
('Shahzaib', 'Nazir', 'Operations Manager', 'm.shahzaib@park.com', '0301-2345678', 125000, 1),
('Rabia',  'Nawaz',    'Cinema Manager',     'rabia.nawaz@park.com',     '0303-4567890', 70000, 2),
('Noman',  'Aslam',    'HR Officer',         'noman.aslam@park.com',     '0311-2345679', 55000, 1),
-- Janitors
('Khalid', 'Hussain', 'Male Janitor', 'khalid.hussain@park.com', '0309-1111111', 30000, 4),
('Nargis', 'Bibi',    'Female Janitor', 'nargis.bibi@park.com',    '0309-2222222', 30000, 4),
-- Ride Operators
('Hamza',   'Iqbal',   'Ride Operator', 'hamza.iqbal@park.com',   '0300-0000001', 40000, 2),
('Ali',     'Raza',    'Ride Operator', 'ali.raza@park.com',      '0300-0000002', 40000, 2),
('Usman',   'Tariq',   'Ride Operator', 'usman.tariq@park.com',   '0300-0000003', 40000, 2),
('Bilal',   'Ahmed',   'Ride Operator', 'bilal.ahmed@park.com',   '0300-0000004', 40000, 2),
('Fahad',   'Ali',     'Ride Operator', 'fahad.ali@park.com',     '0300-0000005', 30000, 2),
('Zain',    'Khan',    'Ride Operator', 'zain.khan@park.com',     '0300-0000006', 40000, 2),
('Omar',    'Saeed',   'Ride Operator', 'omar.saeed@park.com',    '0300-0000007', 40000, 2),
('Umer',    'Iqbal',   'Ride Operator', 'saad.iqbal@park.com',    '0300-0000008', 40000, 2),
('Daniyal', 'Shah',    'Ride Operator', 'daniyal.shah@park.com',  '0300-0000009', 40000, 2),
('Rizwan',  'Ali',     'Ride Operator', 'rizwan.ali@park.com',    '0300-0000010', 40000, 2),
('Hassan',  'Raza',    'Ride Operator', 'hassan.raza@park.com',   '0300-0000011', 40000, 2),
('Ahmad',   'Noor',    'Ride Operator', 'ahmad.noor@park.com',    '0300-0000012', 40000, 2);

INSERT INTO Job_Post (Location_Name, StaffID) VALUES
('Operations Centre', 2),
('Cinema Block',      3),
('HR Office',         4),
('Male Washrooms',    5),
('Female Washrooms',  6);

INSERT INTO Ride (Ride_Name, Status, OperatorID) VALUES
('Roller Coaster',   1, 7),
('Ferris Wheel',     1, 8),
('Bumper Cars',      1, 9),
('Drop Tower',       0, 10),
('River Rapids',     1, 11),
('Carousel',         1, 12),
('Haunted House',    1, 13),
('Go-Karts',         0, 14),
('Swing Ride',       1, 15),
('Mini Train',       1, 16),
('Water Slides',     1, 17),
('Zip Line',         0, 18);

INSERT INTO Screening (Screening_Time, MovieID, HallID) VALUES
('2024-03-01 10:00:00', 1, 1),
('2024-03-02 10:00:00', 5, 1),
('2024-03-03 10:00:00', 9, 1),
('2024-03-01 13:00:00', 2, 2),
('2024-03-02 13:00:00', 6, 2),
('2024-03-03 13:00:00', 10, 2),
('2024-03-01 16:00:00', 3, 3),
('2024-03-02 16:00:00', 7, 3),
('2024-03-03 16:00:00', 11, 3),
('2024-03-01 19:00:00', 4, 4),
('2024-03-02 19:00:00', 8, 4),
('2024-03-03 19:00:00', 12, 4);

INSERT INTO Food_Owner (First_Name, Last_Name, Email, Phone) VALUES
('Zubair', 'Khan', 'zubair.khan@email.com', '03214567890'),
('Sarah', 'Ahmed', 'sarah.piz@email.com', '03224567891'),
('Bilal', 'Dar', 'bilal.desi@email.com', '03234567892'),
('Mona', 'Ijaz', 'mona.ice@email.com', '03244567893'),
('Junaid', 'Ali', 'junaid.j@email.com', '03254567894'),
('Hassan', 'Raza', 'hassan.b@email.com', '03264567895'),
('Waqas', 'Malik', 'waqas.s@email.com', '03274567896'),
('Esha', 'Noor', 'esha.w@email.com', '03284567897'),
('Kamran', 'Shah', 'kamran.n@email.com', '03294567898'),
('Asma', 'Bibi', 'asma.c@email.com', '03314567899'),
('Taimoor', 'Baig', 'taimoor.g@email.com', '03324567800'),
('Zainab', 'Saeed', 'zainab.s@email.com', '03334567801');

INSERT INTO Food_Stalls (Name, Rent, Type, Establish_Date, Opening_Time, Closing_Time, Food_OwnerID) VALUES
('Khan Fast Food', 15000.00, 'Fast Food', '2024-01-01', '09:00:00', '22:00:00', 1),
('Pizza Palace', 18000.00, 'Pizza', '2024-01-05', '10:00:00', '23:00:00', 2),
('Desi Bites', 12000.00, 'Desi', '2024-01-10', '08:00:00', '21:00:00', 3),
('Ice Cream World', 8000.00, 'Dessert', '2024-01-15', '10:00:00', '22:00:00', 4),
('Juice Bar', 7000.00, 'Beverages', '2024-01-20', '09:00:00', '21:00:00', 5),
('Burger Hub', 14000.00, 'Fast Food', '2024-01-25', '10:00:00', '22:00:00', 6),
('Shawarma Corner', 11000.00, 'Fast Food', '2024-02-01', '11:00:00', '23:00:00', 7),
('Waffle House', 9000.00, 'Dessert', '2024-02-05', '09:30:00', '21:30:00', 8),
('Noodle Street', 13000.00, 'Chinese', '2024-02-10', '11:00:00', '22:00:00', 9),
('Chai Dhaba', 6000.00, 'Beverages', '2024-02-15', '08:00:00', '20:00:00', 10),
('Grill Station', 16000.00, 'BBQ', '2024-02-20', '12:00:00', '23:00:00', 11),
('Snack Zone', 5000.00, 'Snacks', '2024-02-25', '09:00:00', '22:00:00', 12);


INSERT INTO Food_Payment (Amount, Payment_Time, Food_StallID) VALUES
(2500.00, '2024-03-01 12:30:00', 1),
(3200.00, '2024-03-01 19:00:00', 2),
(1800.00, '2024-03-02 13:00:00', 3),
(1200.00, '2024-03-02 15:30:00', 4),
(900.00, '2024-03-03 11:00:00', 5),
(2100.00, '2024-03-03 18:00:00', 6),
(1700.00, '2024-03-04 20:00:00', 7),
(1400.00, '2024-03-04 14:30:00', 8),
(2300.00, '2024-03-05 19:30:00', 9),
(800.00, '2024-03-05 10:00:00', 10),
(3500.00, '2024-03-06 21:00:00', 11),
(600.00, '2024-03-06 16:00:00', 12);