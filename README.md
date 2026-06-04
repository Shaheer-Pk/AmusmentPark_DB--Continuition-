
# 🎡 Amusement Park Management System

## Overview
This system is a JavaFX + MySQL-based amusement park management platform with:
- RBAC-based security
- Card-based payment system
- Entertainment modules (rides, cinema, bowling)
- Vendor leasing system
- Central financial ledger

---

## 🧠 Architecture Principles

### 1. Separation of Concerns
- User/Login → Authentication
- Role/Permission → Authorization
- Domain tables → Business logic
- CardTransaction → Financial truth

---

### 2. RBAC Model
Access is controlled via:

User → UserRole → Role → RolePermission → Permission

No boolean flags are used for authorization.

---

## 🗄️ Data Categories

### 🔹 Static Data (Preloaded via seed_data.sql)
These are inserted manually before system startup:
If you want to expand the database you can add your own roles and assign permissions accordingly

- Roles
- Permissions
- Movies
- Cinema Halls
- Seats
- Rides
- Bowling Lanes
- Food Places

---

### 🔸 Dynamic Data (Runtime Generated)

- Users
- Login entries
- Card balances
- Tickets
- Ride usage logs
- Bowling sessions
- Contracts
- Food stalls
- Card transactions

---

## 💰 Financial System

### CardTransaction
Single source of truth for all money movement:

Triggered by:
- Ride usage
- Ticket purchase
- Bowling session
- Card recharge

Never manually inserted.

---

## 🔐 RBAC Rules

### Roles:
- Admin
- Staff
- Vendor
- Customer

### Permissions:
Examples:
- VIEW_REVENUE
- EDIT_STAFF
- MANAGE_RIDES
- BOOK_TICKET

Roles are mapped to permissions dynamically.

---

## ⚠️ System Rules

- No boolean-based authorization
- No direct role checks in UI
- No manual financial ledger edits
- No duplication of static data at runtime

---

## 🚀 Initialization Flow

1. Run Tables_v1.sql
2. Run seed_data_v2.sql
3. Run Triggered_Stored_Procedures_v3.sql
4. Start JavaFX application
5. Users register and interact dynamically

---

## 🎯 Design Goal

A scalable, RBAC-driven amusement park system with:
- clean separation of concerns
- permission-based UI control
- consistent financial tracking
