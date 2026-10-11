# FinanceFlow API 💸

API REST de gestão financeira pessoal: cada usuário tem a sua conta, registra receitas e despesas, organiza por categorias, consulta o saldo e gera relatórios mensais.

Existe também um [app mobile](https://github.com/JoaogVianna/financeflow-mobile) que consome esta API.

## Funcionalidades

- Cadastro e login de usuários, com senha criptografada (BCrypt) e autenticação por token JWT
- Categorias de transações (CRUD)
- Receitas e despesas (CRUD)
- Saldo automático (receitas − despesas), geral ou por período
- Filtros por período e por categoria
- Relatório mensal com totais e resumo por categoria
- Isolamento de dados: cada usuário só enxerga e altera os próprios registros
- Documentação interativa com Swagger UI

## Tecnologias

- Java 21
- Spring Boot 4.1 (Web MVC, Data JPA, Validation)
- Spring Security 7 + JWT (OAuth2 Resource Server, assinatura HS256)
- PostgreSQL 16
- Maven (Maven Wrapper incluso)
- springdoc-openapi 3.1 (Swagger UI)

## Como rodar

**Pré-requisitos:** Java 21 e PostgreSQL.

**1. Crie o banco e o usuário:**

```bash
sudo -u postgres psql -c "CREATE USER financeflow WITH PASSWORD 'financeflow';"
sudo -u postgres psql -c "CREATE DATABASE financeflow OWNER financeflow;"
```

**2. Suba a API:**

```bash
./mvnw spring-boot:run
```

As tabelas são criadas automaticamente na primeira execução.

**3. Abra o Swagger:** http://localhost:8080/swagger-ui.html

Para testar as rotas protegidas: faça o cadastro e o login em `/api/auth`, clique em **Authorize** e cole o token.

### Configuração

| Propriedade | Descrição | Padrão (somente desenvolvimento) |
|---|---|---|
| `JWT_SECRET` (variável de ambiente) | Segredo de assinatura do token, mínimo 32 caracteres | valor de desenvolvimento |
| `app.jwt.expiration-minutes` | Validade do token | `60` |
| `spring.datasource.*` | Conexão com o banco | `localhost:5432/financeflow` |

> Em produção, defina `JWT_SECRET` por variável de ambiente e não use o segredo padrão.

## Endpoints

Todas as rotas, exceto `/api/auth/**` e o Swagger, exigem o cabeçalho `Authorization: Bearer <token>`.

### Autenticação

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/auth/register` | Cria uma conta |
| POST | `/api/auth/login` | Retorna o token JWT |

### Categorias

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/categories` | Cria categoria |
| GET | `/api/categories` | Lista as suas categorias |
| GET | `/api/categories/{id}` | Busca por id |
| PUT | `/api/categories/{id}` | Atualiza |
| DELETE | `/api/categories/{id}` | Remove (só se não tiver transações) |

### Transações

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/transactions` | Registra receita (`INCOME`) ou despesa (`EXPENSE`) |
| GET | `/api/transactions` | Lista, com filtros `startDate`, `endDate` e `categoryId` |
| GET | `/api/transactions/balance` | Saldo, com filtros `startDate` e `endDate` |
| GET | `/api/transactions/{id}` | Busca por id |
| PUT | `/api/transactions/{id}` | Atualiza |
| DELETE | `/api/transactions/{id}` | Remove |

### Relatórios

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/reports/monthly?year=2026&month=10` | Totais do mês e resumo por categoria (sem parâmetros, usa o mês atual) |

## Exemplo de uso

```bash
# cadastro e login
curl -X POST localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d '{"name":"João","email":"joao@email.com","password":"senha12345"}'

TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"joao@email.com","password":"senha12345"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# categoria e receita
curl -X POST localhost:8080/api/categories -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"name":"Salário"}'

curl -X POST localhost:8080/api/transactions -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"description":"Salário de outubro","amount":5000.00,"type":"INCOME","date":"2026-10-05","categoryId":1}'

# relatório do mês
curl -H "Authorization: Bearer $TOKEN" "localhost:8080/api/reports/monthly?year=2026&month=10"
```

Resposta do relatório:

```json
{
  "year": 2026,
  "month": 10,
  "startDate": "2026-10-01",
  "endDate": "2026-10-31",
  "totalIncome": 5000.00,
  "totalExpense": 280.25,
  "balance": 4719.75,
  "categories": [
    { "categoryId": 1, "categoryName": "Alimentação", "income": 0, "expense": 220.25, "balance": -220.25 }
  ]
}
```

Os erros seguem o formato RFC 7807 (`application/problem+json`). Erros de validação trazem o campo `errors` com a mensagem de cada campo.

## Arquitetura

```
src/main/java/com/financeflow/
├── controller/   # Endpoints REST
├── service/      # Regras de negócio
├── repository/   # Acesso a dados (Spring Data JPA + JPQL)
├── entity/       # Entidades JPA
├── dto/          # Contratos de entrada e saída (records)
├── security/     # Utilitários de autenticação
├── exception/    # Exceções de domínio e tratamento global de erros
├── config/       # Segurança (JWT) e OpenAPI
└── FinanceflowApplication.java
```

## Decisões técnicas

- **`BigDecimal` para dinheiro**, nunca `double`, para evitar erro de ponto flutuante.
- **DTOs em records** com Bean Validation: a entidade nunca é exposta pela API.
- **Isolamento por usuário:** o id do usuário vem sempre do token validado, nunca da requisição. Recursos de outro usuário respondem `404`, sem revelar que existem.
- **JWT stateless** com assinatura HS256 via Spring Security (OAuth2 Resource Server); o login responde com a mesma mensagem para e-mail inexistente e senha errada.
- **Agregações no banco** (`sum` e `group by` em JPQL) em vez de somar em memória.
- **Erros padronizados** por um `@RestControllerAdvice` (`ProblemDetail`).
- `open-in-view` desativado, com `@Transactional` explícito onde há carregamento lazy.

## Roadmap

- [x] CRUD de categorias e transações, saldo e filtros
- [x] Cadastro, login e proteção das rotas com JWT
- [x] Dados isolados por usuário
- [x] Relatório mensal por categoria
- [x] Documentação com Swagger
- [ ] Testes automatizados (JUnit + MockMvc)
- [ ] Configuração de CORS para clientes web
- [ ] Migrations com Flyway
- [ ] Credenciais do banco por variável de ambiente
- [ ] Dockerfile e CI com GitHub Actions
- [ ] Refresh token
