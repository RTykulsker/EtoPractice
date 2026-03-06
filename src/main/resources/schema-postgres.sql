-- PostgreSQL does NOT use sqlite_sequence; remove it entirely.

-- USERS table
CREATE TABLE IF NOT EXISTS users (
    useridx      SERIAL PRIMARY KEY,
    callsign     TEXT NOT NULL UNIQUE,
    name         TEXT,
    active       INTEGER DEFAULT 1,
    datejoined   DATE DEFAULT CURRENT_DATE
);

-- EXERCISES table
CREATE TABLE IF NOT EXISTS exercises (
    exerciseidx   SERIAL PRIMARY KEY,
    date          DATE NOT NULL,
    type          TEXT NOT NULL,
    name          TEXT NOT NULL,
    description   TEXT NOT NULL
);

-- Equivalent unique index
CREATE UNIQUE INDEX exercise_datetypename_index
    ON exercises (date DESC, type ASC, name ASC);

-- EVENTS table
CREATE TABLE IF NOT EXISTS events (
    eventidx       SERIAL PRIMARY KEY,
    useridx        INTEGER NOT NULL REFERENCES users(useridx),
    exerciseidx    INTEGER NOT NULL REFERENCES exercises(exerciseidx),
    latitude       DOUBLE PRECISION NOT NULL,
    longitude      DOUBLE PRECISION NOT NULL,
    feedbackcount  INTEGER NOT NULL,
    feedbacktext   TEXT NOT NULL,
    context        TEXT
);

-- Unique constraint on (UserIDX, ExerciseIDX)
CREATE UNIQUE INDEX event_userexercise_index
    ON events (useridx ASC, exerciseidx ASC);