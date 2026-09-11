# AssinaAE

Plataforma para envio de contratos em PDF a um profissional e a um cliente, coleta da assinatura de cada parte e geração do PDF final assinado por ambos.

O profissional tem conta (email/senha ou login com Google) e acompanha os contratos em um painel. O cliente nunca faz login — recebe o link de assinatura por email, assina, e cada parte é notificada por email quando o contrato é totalmente assinado.

## Arquitetura

- **Backend**: Spring Boot (Java 17), Postgres (hospedado na [Neon](https://neon.tech)), migrações de schema via Flyway ([src/main/resources/db/migration](src/main/resources/db/migration)).
- **Frontend**: React + Vite, publicado na Vercel.
- **Autenticação**: JWT (HS256) para o profissional, com login por email/senha ou Google (ID token do Google Identity Services, validado no backend só com o Client ID — não usa client secret).
- **Email**: enviado pela API HTTP do SendGrid ([EmailService](src/main/java/com/kayomeira/assinatura/service/EmailService.java)) em vez de SMTP, porque a Railway (onde o backend roda) bloqueia as portas SMTP de saída.
- **PDF**: manipulado com Apache PDFBox no backend (embutir a assinatura, gerar o PDF final). No frontend, a pré-visualização da página para posicionar a assinatura usa pdf.js.

## Fluxo de assinatura

1. O profissional cria o contrato (título, descrição, dados do cliente e upload do PDF). O backend gera um token único de assinatura para o profissional e outro para o cliente, e envia o link de cada um por email.
2. Quem acessa o link (`/assinar/:token`) desenha a própria assinatura em um canvas e depois a posiciona sobre a pré-visualização real do PDF — pode escolher a página, arrastar e redimensionar antes de confirmar. Posição e tamanho são guardados como frações da página (independentes de resolução de tela), convertidas para coordenadas do PDF no backend ([AssinaturaPDFService](src/main/java/com/kayomeira/assinatura/service/AssinaturaPDFService.java)).
3. Cada assinatura é embutida no PDF assim que confirmada (não em lote no final), acumulando sobre o PDF já parcialmente assinado pela outra parte.
4. Quando ambas as partes assinam, o PDF final é enviado por email para as duas e fica disponível para download pelo próprio link (`/download/:token`) ou pelo painel do profissional.
5. Qualquer uma das partes pode recusar o contrato antes de assinar; isso impede a outra parte de assinar depois.

## Configuração (variáveis de ambiente)

Definidas em [src/main/resources/application.yml](src/main/resources/application.yml):

| Variável | Descrição | Padrão |
|---|---|---|
| `DB_URL` | JDBC URL do Postgres (Neon) | `jdbc:postgresql://localhost:5432/assinatura_digital` |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciais do banco | `postgres` / `postgres` |
| `MAIL_FROM` | Remetente dos emails | `seu-email@exemplo.com` |
| `SENDGRID_API_KEY` | Chave da API HTTP do SendGrid | — (envio de email fica desativado sem isso) |
| `FRONTEND_URL` | Base usada nos links de assinatura enviados por email; também a origem liberada no CORS | `http://localhost:3000` |
| `JWT_SECRET` | Segredo usado para assinar os tokens de login (HS256) | valor de desenvolvimento, inseguro |
| `JWT_EXPIRACAO_DIAS` | Validade do token de login, em dias | `7` |
| `GOOGLE_CLIENT_ID` | Client ID OAuth do Google, usado para validar o login com Google | — (login com Google fica desativado sem isso) |

Frontend ([vercel.json](vercel.json), build-time): `VITE_API_URL` (URL pública do backend) e `VITE_GOOGLE_CLIENT_ID` (mesmo Client ID do backend, usado para renderizar o botão do Google).

## API (`/api`)

Autenticação (públicas):
- `POST /api/auth/registrar { nome, email, senha }` — cria a conta do profissional, devolve `{ token, id, nome, email }`.
- `POST /api/auth/login { email, senha }` — devolve o token.
- `POST /api/auth/google { credential }` — troca o ID token do Google Identity Services pelo nosso token.
- `GET /api/auth/verificar-email/{token}`, `POST /api/auth/reenviar-verificacao`, `POST /api/auth/esqueci-senha`, `POST /api/auth/redefinir-senha` — verificação de email e recuperação de senha.

Contratos (exigem `Authorization: Bearer <token>`, exceto as rotas `/token/**`):
- `POST /api/contratos` — cria o contrato (multipart: parte `contrato` em JSON + parte `pdf`) associado ao profissional autenticado, e dispara os emails de convite.
- `GET /api/contratos/meus` — lista os contratos do profissional autenticado (painel).
- `GET /api/contratos/{id}` / `GET /api/contratos/{id}/download` — detalhe/download; só funciona se o contrato pertencer ao profissional autenticado.
- `GET /api/contratos/token/{token}` — **pública**: dados do contrato para a página de assinatura (cliente ou profissional, sem login).
- `GET /api/contratos/token/{token}/pdf-preview` — **pública**: PDF no estado atual (original ou parcialmente assinado), usado para posicionar a assinatura antes de confirmar.
- `POST /api/contratos/token/{token}/assinar { assinaturaBase64, pagina, x, y, largura, altura }` — **pública**: registra a assinatura (PNG em Base64) na página e posição indicadas (frações 0–1) de quem acessou com aquele token.
- `GET /api/contratos/token/{token}/download` — **pública**: baixa o PDF assinado pelo próprio link, sem login.
- `POST /api/contratos/token/{token}/rejeitar` — **pública**: recusa o contrato em nome de quem acessou com aquele token.

## Frontend (rotas)

Páginas do profissional (exigem login):
- `/` — painel com os contratos em andamento e assinados.
- `/novo-contrato` — formulário para criar um novo contrato (título, descrição, dados do cliente e upload do PDF).
- `/contratos/:id` — detalhe de um contrato e download do PDF assinado.
- `/login`, `/registrar` — entrar ou criar conta (email/senha ou Google).

Páginas do cliente (públicas, sem login):
- `/assinar/:token` — página de assinatura acessada pelo link enviado por email; desenhar a assinatura e posicioná-la no PDF.
- `/download/:token` — status do contrato e link para baixar o PDF assinado quando completo.
