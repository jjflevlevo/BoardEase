CREATE TABLE IF NOT EXISTS rooms (
  room_id INTEGER PRIMARY KEY AUTOINCREMENT,
  room_no TEXT NOT NULL UNIQUE,
  capacity INTEGER NOT NULL,
  monthly_rent REAL NOT NULL
);

CREATE TABLE IF NOT EXISTS accounts (
  account_id INTEGER PRIMARY KEY AUTOINCREMENT,
  email TEXT NOT NULL UNIQUE,
  phone TEXT,
  password_hash TEXT NOT NULL,
  role TEXT NOT NULL CHECK (role IN ('TENANT','OWNER'))
);

CREATE TABLE IF NOT EXISTS tenants (
  tenant_id INTEGER PRIMARY KEY AUTOINCREMENT,
  account_id INTEGER NOT NULL REFERENCES accounts(account_id),
  room_id INTEGER REFERENCES rooms(room_id),
  name TEXT NOT NULL,
  address TEXT,
  age INTEGER,
  occupation TEXT,
  emergency_no TEXT
);

CREATE TABLE IF NOT EXISTS meter_readings (
  reading_id INTEGER PRIMARY KEY AUTOINCREMENT,
  room_id INTEGER NOT NULL REFERENCES rooms(room_id),
  type TEXT NOT NULL CHECK (type IN ('WATER','ELECTRIC')),
  reading REAL NOT NULL,
  billing_month TEXT NOT NULL,
  UNIQUE (room_id, type, billing_month)
);

CREATE TABLE IF NOT EXISTS bills (
  bill_id INTEGER PRIMARY KEY AUTOINCREMENT,
  tenant_id INTEGER NOT NULL REFERENCES tenants(tenant_id),
  type TEXT NOT NULL CHECK (type IN ('RENT','WATER','ELECTRIC')),
  billing_month TEXT NOT NULL,
  amount REAL NOT NULL,
  due_date TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'UNPAID' CHECK (status IN ('UNPAID','PARTIAL','PAID')),
  UNIQUE (tenant_id, type, billing_month)
);

CREATE TABLE IF NOT EXISTS payments (
  payment_id INTEGER PRIMARY KEY AUTOINCREMENT,
  bill_id INTEGER NOT NULL REFERENCES bills(bill_id),
  amount REAL NOT NULL,
  method TEXT NOT NULL DEFAULT 'CASH',
  ref_no TEXT,
  proof_path TEXT,
  status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED')),
  paid_at TEXT DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS settings (
  setting_key TEXT PRIMARY KEY,
  setting_value TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS activity_log (
  activity_id INTEGER PRIMARY KEY AUTOINCREMENT,
  type TEXT NOT NULL,
  message TEXT NOT NULL,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);