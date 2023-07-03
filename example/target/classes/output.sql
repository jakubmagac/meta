create table if not exists CompanyProject(
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	ProjectName TEXT NOT NULL UNIQUE
);
create table if not exists Department(
	pk INTEGER PRIMARY KEY AUTOINCREMENT,
	name TEXT,
	code TEXT
);
create table if not exists Person(
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	surname TEXT,
	name TEXT,
	age INTEGER,
	department INTEGER, 
	FOREIGN KEY (department) REFERENCES Department(pk)
);
