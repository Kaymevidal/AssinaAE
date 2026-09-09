# Assinatura Digital

Plataforma para envio de contratos em PDF a um profissional e a um cliente, coleta da assinatura de cada parte (desenhada em um canvas) e geração do PDF final assinado por ambos.

O profissional precisa de conta (email/senha ou login com Google) e acompanha os contratos em um painel. O cliente nunca faz login — recebe o link de assinatura por email, assina, e o profissional é notificado por email quando o contrato é totalmente assinado.

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
| `JWT_SECRET` | Segredo usado para assinar os tokens de login (HS256). **Troque em produção** — ex.: `openssl rand -base64 48` | valor de desenvolvimento, inseguro |
| `JWT_EXPIRACAO_DIAS` | Validade do token de login, em dias | `7` |
| `GOOGLE_CLIENT_ID` | Client ID OAuth do Google usado para validar o login com Google (veja abaixo) | — (login com Google fica desativado sem isso) |

As tabelas são criadas/atualizadas automaticamente via `ddl-auto: update`. O banco em si (`assinatura_digital`, ou o nome que você criar) precisa existir antes — na Neon, ele é criado pelo painel/CLI da Neon, não pela aplicação.

### Login com Google

1. No [Google Cloud Console](https://console.cloud.google.com/apis/credentials), crie uma credencial OAuth 2.0 do tipo **Web application**.
2. Em "Authorized JavaScript origins", adicione `http://localhost:3000` e a URL do deploy na Vercel.
3. Copie o Client ID gerado e defina em **duas** variáveis: `GOOGLE_CLIENT_ID` (backend, valida o token) e `VITE_GOOGLE_CLIENT_ID` (frontend, mostra o botão). Não precisa do client secret — a verificação é feita só com o ID token.

### Deploy

- **Frontend → Vercel**: build da pasta `src/frontend` (`npm run build`, saída em `dist/`), com `VITE_API_URL` apontando para a URL pública do backend e `VITE_GOOGLE_CLIENT_ID` configurado.
- **Backend → onde for hospedado**: definir `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` com a connection string da Neon, `JWT_SECRET`, `GOOGLE_CLIENT_ID` e `FRONTEND_URL` com a URL da Vercel (usada no CORS e nos links dos emails).

### API (`http://localhost:8080/api`)

Autenticação (públicas):
- `POST /api/auth/registrar { nome, email, senha }` — cria a conta do profissional, devolve `{ token, id, nome, email }`.
- `POST /api/auth/login { email, senha }` — devolve o token.
- `POST /api/auth/google { credential }` — troca o ID token do Google Identity Services pelo nosso token.

Contratos (exigem `Authorization: Bearer <token>`, exceto as rotas `/token/**`):
- `POST /api/contratos` — cria o contrato (multipart: parte `contrato` em JSON + parte `pdf`) associado ao profissional autenticado, e dispara os emails de convite.
- `GET /api/contratos/meus` — lista os contratos do profissional autenticado (painel).
- `GET /api/contratos/{id}` / `GET /api/contratos/{id}/download` — detalhe/download; só funciona se o contrato pertencer ao profissional autenticado.
- `GET /api/contratos/token/{token}` — **pública**: dados do contrato para a página de assinatura (cliente ou profissional, sem login).
- `POST /api/contratos/token/{token}/assinar` — **pública**: registra a assinatura (`{ "assinaturaBase64": "..." }`, PNG em Base64) de quem acessou com aquele token.
- `GET /api/contratos/token/{token}/download` — **pública**: baixa o PDF assinado pelo próprio link, sem login (usado pelo cliente).

## Frontend (React + Vite)

Requisitos: Node.js 18+.

```bash
cd src/frontend
npm install
npm run dev
```

Roda em `http://localhost:3000`. Variáveis em `src/frontend/.env` (não versionado): `VITE_API_URL` (endereço da API) e `VITE_GOOGLE_CLIENT_ID` (login com Google).

Páginas do profissional (exigem login):
- `/` — painel com os contratos em andamento e assinados.
- `/novo-contrato` — formulário para criar um novo contrato (título, descrição, dados do cliente e upload do PDF).
- `/contratos/:id` — detalhe de um contrato e download do PDF assinado.
- `/login`, `/registrar` — entrar ou criar conta (email/senha ou Google).

Páginas do cliente (públicas, sem login):
- `/assinar/:token` — página de assinatura acessada pelo link enviado por email; captura a assinatura em um canvas.
- `/download/:token` — status do contrato e link para baixar o PDF assinado quando completo.
