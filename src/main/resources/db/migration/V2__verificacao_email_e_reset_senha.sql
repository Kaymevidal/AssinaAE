ALTER TABLE profissionais
    ADD COLUMN email_verificado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN token_verificacao_email VARCHAR(255),
    ADD COLUMN token_reset_senha VARCHAR(255),
    ADD COLUMN token_reset_expiracao TIMESTAMP;

ALTER TABLE profissionais
    ADD CONSTRAINT uk_profissionais_token_verificacao_email UNIQUE (token_verificacao_email),
    ADD CONSTRAINT uk_profissionais_token_reset_senha UNIQUE (token_reset_senha);
