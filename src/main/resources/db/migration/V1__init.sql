CREATE TABLE profissionais (
    id          BIGSERIAL PRIMARY KEY,
    nome        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    senha_hash  VARCHAR(255),
    google_id   VARCHAR(255),
    data_criacao TIMESTAMP NOT NULL,
    CONSTRAINT uk_profissionais_email UNIQUE (email),
    CONSTRAINT uk_profissionais_google_id UNIQUE (google_id)
);

CREATE TABLE contratos (
    id                              BIGSERIAL PRIMARY KEY,
    titulo                          VARCHAR(255) NOT NULL,
    descricao                       VARCHAR(255) NOT NULL,
    profissional_id                 BIGINT NOT NULL REFERENCES profissionais (id),
    email_cliente                   VARCHAR(255) NOT NULL,
    nome_cliente                    VARCHAR(255) NOT NULL,
    pdf_original                    BYTEA NOT NULL,
    pdf_assinado                    BYTEA,
    status_profissional             VARCHAR(20) NOT NULL,
    status_cliente                  VARCHAR(20) NOT NULL,
    token_profissional              VARCHAR(255),
    token_cliente                   VARCHAR(255),
    data_criacao                    TIMESTAMP NOT NULL,
    data_assinatura_profissional    TIMESTAMP,
    data_assinatura_cliente         TIMESTAMP,
    CONSTRAINT uk_contratos_token_profissional UNIQUE (token_profissional),
    CONSTRAINT uk_contratos_token_cliente UNIQUE (token_cliente)
);

CREATE INDEX idx_contratos_profissional_id ON contratos (profissional_id);
