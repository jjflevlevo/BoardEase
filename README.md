# BoardEase

A boarding house rent and utilities billing system built in Java (Swing + FlatLaf) with an SQLite database. Made as our OOP final term project at UST Legazpi.

BoardEase lets a boarding house owner manage rooms and tenants, record shared electric and water meter readings, split the bills fairly, and track payments. Tenants get their own login to view their bills and pay online.

## Features

**Owner**
- Dashboard with stats and a monthly bar chart
- Manage rooms (add, edit) and tenants (add, view profiles, delete)
- Shared meter computation for electric and water bills
- Billing, payments review, and reports
- Set GCash details for tenants' Pay Online option
- Search, sort, and multi-select in tables

**Tenant**
- Sign up and log in (passwords are hashed)
- View rent, electric, and water bills
- Pay Online via GCash with optional proof image upload

There's also the original console version: run with the argument `console`.

## Tech Stack

- Java 17
- Swing with [FlatLaf](https://www.formdev.com/flatlaf/) for the look and feel
- SQLite via `sqlite-jdbc`
- Maven

## Project Structure

```
src/main/java/boardinghouse/
├── Main.java          entry point
├── model/             Account, Room, Tenant, Bill (Rent/Electric/Water), Payment
├── dao/               database access classes
├── service/           auth, billing, and payment logic
├── gui/               Swing screens and components
├── ui/                console menus
└── util/              DB connection, password hashing, validation
sql/schema.sql         database schema (tables are created on first run)
boardinghouse.db       sample database
```

## Running It

You need JDK 17+ and Maven.

```bash
mvn compile
mvn exec:java -Dexec.mainClass=boardinghouse.Main
```

Or open the folder in VS Code with the Java Extension Pack and run `Main.java`.

Run it from the project root so it can find `sql/schema.sql` and `boardinghouse.db`. If the database file is missing, it gets created automatically.

## Default Owner Login

| Email | Password |
|---|---|
| owner@boardinghouse.com | owner123 |
