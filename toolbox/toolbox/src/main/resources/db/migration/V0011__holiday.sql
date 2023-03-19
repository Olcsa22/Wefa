CREATE TABLE holiday ( -- Google Calendar-ból áthúzott ünnepnapok és hétvégi munkanapok
	id SERIAL PRIMARY KEY,
	dstart DATE NOT NULL, -- a nap (csak azért ...start mert a Google is így hívja)
	summary VARCHAR(160),
	workday BOOLEAN NOT NULL, -- hétvégi munkanapok esetén true
	UNIQUE (dstart)
);