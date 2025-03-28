-- Initial database schema for Degree Attestation System

-- Universities table
CREATE TABLE universities (
                              id BIGSERIAL PRIMARY KEY,
                              registration_id VARCHAR(50) NOT NULL UNIQUE,
                              name VARCHAR(255) NOT NULL,
                              email VARCHAR(255) NOT NULL UNIQUE,
                              address TEXT,
                              stake_amount DECIMAL(12, 2) NOT NULL,
                              status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'PENDING', 'BLACKLISTED')),
                              join_date TIMESTAMP NOT NULL DEFAULT NOW(),
                              blockchain_id VARCHAR(100),
                              created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                              updated_at TIMESTAMP
);

CREATE INDEX idx_universities_status ON universities(status);

-- Degrees table
CREATE TABLE degrees (
                         id BIGSERIAL PRIMARY KEY,
                         degree_id VARCHAR(50) NOT NULL UNIQUE,
                         student_id VARCHAR(50) NOT NULL,
                         student_name VARCHAR(255) NOT NULL,
                         degree_name VARCHAR(255) NOT NULL,
                         university_id BIGINT NOT NULL REFERENCES universities(id),
                         issue_date TIMESTAMP NOT NULL,
                         degree_hash VARCHAR(255) NOT NULL UNIQUE,
                         certificate_url VARCHAR(255),
                         pattern_data BYTEA,
                         status VARCHAR(20) NOT NULL CHECK (status IN ('REGISTERED', 'PROCESSING', 'VERIFIED', 'REVOKED')),
                         blockchain_tx_id VARCHAR(128),
                         created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                         updated_at TIMESTAMP
);

CREATE INDEX idx_degrees_university_id ON degrees(university_id);
CREATE INDEX idx_degrees_student_id ON degrees(student_id);
CREATE INDEX idx_degrees_status ON degrees(status);
CREATE INDEX idx_degrees_degree_hash ON degrees(degree_hash);

-- Verification requests table
CREATE TABLE verification_requests (
                                       id BIGSERIAL PRIMARY KEY,
                                       request_id VARCHAR(50) NOT NULL UNIQUE,
                                       employer_id VARCHAR(50) NOT NULL,
                                       degree_id BIGINT NOT NULL REFERENCES degrees(id),
                                       request_date TIMESTAMP NOT NULL DEFAULT NOW(),
                                       result VARCHAR(20) NOT NULL CHECK (result IN ('AUTHENTIC', 'FAILED', 'PENDING')),
                                       verification_details TEXT,
                                       blockchain_tx_id VARCHAR(128),
                                       completed_at TIMESTAMP,
                                       payment_amount DECIMAL(10, 2) NOT NULL,
                                       payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                       updated_at TIMESTAMP
);

CREATE INDEX idx_verification_requests_degree_id ON verification_requests(degree_id);
CREATE INDEX idx_verification_requests_employer_id ON verification_requests(employer_id);
CREATE INDEX idx_verification_requests_result ON verification_requests(result);
CREATE INDEX idx_verification_requests_payment_status ON verification_requests(payment_status);

-- Payments table
CREATE TABLE payments (
                          id BIGSERIAL PRIMARY KEY,
                          payment_id VARCHAR(50) NOT NULL UNIQUE,
                          verification_id BIGINT REFERENCES verification_requests(id),
                          amount DECIMAL(10, 2) NOT NULL,
                          payment_method VARCHAR(50) NOT NULL,
                          payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                          transaction_reference VARCHAR(128),
                          payment_date TIMESTAMP,
                          university_share DECIMAL(10, 2),
                          created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMP
);

CREATE INDEX idx_payments_verification_id ON payments(verification_id);
CREATE INDEX idx_payments_payment_status ON payments(payment_status);

-- University payouts table
CREATE TABLE university_payouts (
                                    id BIGSERIAL PRIMARY KEY,
                                    payout_id VARCHAR(50) NOT NULL UNIQUE,
                                    university_id BIGINT NOT NULL REFERENCES universities(id),
                                    amount DECIMAL(12, 2) NOT NULL,
                                    payout_method VARCHAR(50) NOT NULL,
                                    payout_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                    transaction_reference VARCHAR(128),
                                    payout_date TIMESTAMP,
                                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                    updated_at TIMESTAMP
);

CREATE INDEX idx_university_payouts_university_id ON university_payouts(university_id);
CREATE INDEX idx_university_payouts_payout_status ON university_payouts(payout_status);

-- Users table
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       full_name VARCHAR(255) NOT NULL,
                       role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'UNIVERSITY', 'EMPLOYER')),
                       university_id BIGINT REFERENCES universities(id),
                       employer_id VARCHAR(50),
                       account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                       last_login TIMESTAMP,
                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMP
);

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_university_id ON users(university_id);
CREATE INDEX idx_users_employer_id ON users(employer_id);

-- Audit log table
CREATE TABLE audit_logs (
                            id BIGSERIAL PRIMARY KEY,
                            user_id BIGINT REFERENCES users(id),
                            action VARCHAR(50) NOT NULL,
                            entity_type VARCHAR(50) NOT NULL,
                            entity_id VARCHAR(50) NOT NULL,
                            details TEXT,
                            ip_address VARCHAR(50),
                            timestamp TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity_type_id ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);

-- Revocations table
CREATE TABLE revocations (
                             id BIGSERIAL PRIMARY KEY,
                             degree_id BIGINT NOT NULL REFERENCES degrees(id),
                             revocation_reason TEXT NOT NULL,
                             revoked_by BIGINT NOT NULL REFERENCES users(id),
                             blockchain_tx_id VARCHAR(128),
                             revocation_date TIMESTAMP NOT NULL DEFAULT NOW(),
                             created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_revocations_degree_id ON revocations(degree_id);

-- Blacklisting table
CREATE TABLE blacklistings (
                               id BIGSERIAL PRIMARY KEY,
                               university_id BIGINT NOT NULL REFERENCES universities(id),
                               blacklisting_reason TEXT NOT NULL,
                               blacklisted_by BIGINT NOT NULL REFERENCES users(id),
                               blockchain_tx_id VARCHAR(128),
                               blacklisting_date TIMESTAMP NOT NULL DEFAULT NOW(),
                               created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_blacklistings_university_id ON blacklistings(university_id);

-- System statistics view
CREATE VIEW system_statistics AS
SELECT
    (SELECT COUNT(*) FROM universities WHERE status = 'ACTIVE') AS active_universities,
    (SELECT COUNT(*) FROM degrees) AS total_degrees,
    (SELECT COUNT(*) FROM verification_requests) AS total_verifications,
    (SELECT COUNT(*) FROM verification_requests WHERE result = 'AUTHENTIC') AS successful_verifications,
    (
        CASE
            WHEN (SELECT COUNT(*) FROM verification_requests) = 0 THEN 0
            ELSE (SELECT COUNT(*) FROM verification_requests WHERE result = 'AUTHENTIC') * 100.0 /
                 (SELECT COUNT(*) FROM verification_requests WHERE result IN ('AUTHENTIC', 'FAILED'))
            END
        ) AS success_rate,
    (SELECT SUM(payment_amount) FROM verification_requests) AS total_payment_amount,
    (SELECT COUNT(*) FROM universities WHERE status = 'BLACKLISTED') AS blacklisted_universities,
    (SELECT COUNT(*) FROM degrees WHERE status = 'REVOKED') AS revoked_degrees,
    NOW() AS calculation_time;