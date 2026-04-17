CREATE TABLE result (
    id SERIAL PRIMARY KEY,
    election_id BIGINT,
    candidate_name VARCHAR(255),
    votes INT
);