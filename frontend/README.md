# MyBankUML Frontend

## Setup

Make sure Node.js is installed then run:

```bash
npm install
npm run dev
```

App runs at `http://localhost:5173`

## Demo Logins

| Role     | Email                    | Password    |
|----------|--------------------------|-------------|
| Customer | customer@example.com     | password123 |
| Teller   | teller@example.com       | password123 |
| Admin    | admin@example.com        | password123 |


## Next Steps: Backend Integration

### Currently Hardcoded
- All user data, accounts, transactions (see `src/services/mockData.ts`)
- Authentication responses (passwords validated in-memory)
- All CRUD operations return mock data with simulated delays
- No actual persistence - data resets on page refresh

### Required Java Endpoints

The backend should implement these REST endpoints to replace the mock API in `src/services/api.ts`:

**Authentication**
- `POST /api/auth/login` - Email/password → User object + session/token
- `POST /api/auth/logout` - End session
- `POST /api/auth/forgot-password` - Create password reset request

**Accounts**
- `GET /api/accounts?customerId={id}` - Get accounts by customer
- `GET /api/accounts/{id}` - Get single account details
- `GET /api/accounts/search?query={q}&page={p}&limit={l}` - Search accounts (paginated)

**Transactions**
- `GET /api/transactions?accountId={id}` - Get transactions by account
- `POST /api/transactions` - Create transaction (body: accountId, type, amount, description)

**Admin - Users**
- `GET /api/admin/users` - Get all users
- `PUT /api/admin/users/{id}/role` - Update user role (body: role)
- `PUT /api/admin/users/{id}/status` - Toggle user active status

**Admin - Password Resets**
- `GET /api/admin/password-resets` - Get pending reset requests
- `PUT /api/admin/password-resets/{id}/approve` - Approve request
- `PUT /api/admin/password-resets/{id}/reject` - Reject request

### Integration Steps

1. **Update `src/services/api.ts`**: Replace each mock method with `fetch()` calls to Java endpoints
2. **Add base URL**: Create `VITE_API_URL` environment variable (e.g., `http://localhost:8080`)
3. **Handle auth**: Store JWT/session token, attach to requests via headers
4. **Error handling**: Map Java exception responses to user-friendly messages
5. **Remove mock data**: Delete `src/services/mockData.ts` once backend is connected

