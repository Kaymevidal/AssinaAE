# Assinatura Digital

Plataforma para envio de contratos em PDF a um profissional e a um cliente, coleta da assinatura de cada parte (desenhada em um canvas) e geração do PDF final assinado por ambos.

## Backend (Spring Boot)

Requisitos: Java 17, Maven, PostgreSQL (em produção, [Neon](https://neon.tech)).

```bash
mvn spring-boot:run
```

Configuração em [src/main/resources/application.yml](src/main/resources/application.yml), sobrescrevível por variáveis de ambiente:

| Variável | Descrição | Padrão |
|---|---|---|
| `DB_URL` | JDBC URL do Postgres. Na Neon, algo como `jdbc:postgresql://ep-xxx.<região>.aws.neon.tech/assinatura_digital?sslmode=require` | `jdbc:postgresql://localhost:5432/assinatura_digital` |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciais do banco | `postgres` / `postgres` |
| `MAIL_HOST` / `MAIL_PORT` | Servidor SMTP | `smtp.gmail.com` / `587` |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Credenciais SMTP (para Gmail, use uma senha de app) | — |
| `MAIL_FROM` | Remetente dos emails | — |
| `FRONTEND_URL` | Base usada nos links de assinatura enviados por email (em produção, a URL do deploy na Vercel) | `http://localhost:3000` |

As tabelas são criadas/atualizadas automaticamente via `ddl-auto: update`. O banco em si (`assinatura_digital`) precisa existir antes — na Neon, ele é criado pelo painel/CLI da Neon, não pela aplicação.

### Deploy

- **Frontend → Vercel**: build da pasta `src/frontend` (`npm run build`, saída em `dist/`), com `VITE_API_URL` apontando para a URL pública do backend.
- **Backend → onde for hospedado**: definir `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` com a connection string da Neon e `FRONTEND_URL` com a URL da Vercel (usada no CORS e nos links dos emails).

API exposta em `http://localhost:8080/api`:

- `POST /api/contratos` — cria o contrato (multipart: parte `contrato` em JSON + parte `pdf`) e dispara os emails de convite para profissional e cliente.
- `GET /api/contratos/token/{token}` — dados do contrato para a página pública de assinatura.
- `POST /api/contratos/token/{token}/assinar` — registra a assinatura (`{ "assinaturaBase64": "..." }`, PNG em Base64) de quem acessou com aquele token.
- `GET /api/contratos/{id}` — status do contrato.
- `GET /api/contratos/{id}/download` — baixa o PDF assinado (somente após ambas as partes assinarem).

## Frontend (React + Vite)

Requisitos: Node.js 18+.

```bash
cd src/frontend
npm install
npm run dev
```

Roda em `http://localhost:3000`. Para apontar para uma API em outro endereço, defina `VITE_API_URL` (ex.: em `src/frontend/.env`).

Páginas:

- `/` — formulário para criar um novo contrato (título, descrição, dados das duas partes e upload do PDF).
- `/assinar/:token` — página de assinatura acessada pelo link enviado por email; captura a assinatura em um canvas.
- `/download/:id` — status do contrato e link para baixar o PDF assinado quando completo.
