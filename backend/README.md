# MyBankUML Backend

## Setup & Run

**IMPORTANT:** Always run commands from the `backend` directory. The backend uses the `bank.db` file in the project root (parent directory), NOT in the backend folder.

From the `backend` directory:

### 1. Install dependencies (first time only)
```cmd
mvn clean install
```

### 2. Run the server
```cmd
mvn exec:java
```

Server runs on **http://localhost:8080**

The database (`bank.db`) is pre-populated and committed in the project root. Maven is configured to use the root directory as the working directory via `<workingDirectory>..</workingDirectory>` in pom.xml.

## Test Credentials

| Role | Username | Password |
|------|----------|----------|
| Admin | admin | password123 |
| Teller | teller | password123 |
| Customer | customer | password123 |
| Customer | bob | password123 |
| Customer | emma | password123 |
| Customer | james | password123 |

## API Endpoints

- POST `/api/auth/login` - Login
- GET `/api/customers/{id}/accounts` - Get customer accounts
- GET `/api/accounts/{id}` - Get account by ID
- GET `/api/accounts/search` - Search accounts
- GET `/api/accounts/{id}/transactions` - Get transactions
- POST `/api/accounts/{id}/transactions` - Create transaction
- GET `/api/users` - Get all users (admin)

## Frontend

Start the frontend in a separate terminal:
```cmd
cd ..\frontend
npm run dev
```

Navigate to http://localhost:5173
