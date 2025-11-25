import { User, Account, Transaction, PasswordResetRequest } from '@/types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const getAuthToken = (): string | null => {
  return localStorage.getItem('token');
};

const getAuthHeaders = (): HeadersInit => {
  const token = getAuthToken();
  return {
    'Content-Type': 'application/json',
    ...(token && { Authorization: `Bearer ${token}` }),
  };
};

const handleResponse = async (response: Response) => {
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'An error occurred' }));
    throw new Error(error.message || 'Request failed');
  }
  return response.json();
};

const mapAccountType = (backendType: string): Account['accountType'] => {
  const typeMap: Record<string, Account['accountType']> = {
    'check': 'checking',
    'saving': 'savings',
    'card': 'checking',
  };
  return typeMap[backendType.toLowerCase()] || 'checking';
};

class ApiService {
  // Authentication
  async login(username: string, password: string): Promise<{ user: User; token: string }> {
    const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });

    const data = await handleResponse(response);

    return {
      user: {
        id: String(data.user.id),
        username: data.user.username,
        firstName: data.user.firstName,
        lastName: data.user.lastName,
        role: data.user.role,
        isActive: data.user.isActive,
        createdAt: data.user.createdAt,
      },
      token: data.token,
    };
  }

  async requestPasswordReset(username: string): Promise<void> {
    await fetch(`${API_BASE_URL}/api/auth/password-reset/request`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username }),
    });
  }

  // Account operations
  async getAccountsByCustomerId(customerId: string): Promise<Account[]> {
    const response = await fetch(`${API_BASE_URL}/api/customers/${customerId}/accounts`, {
      headers: getAuthHeaders(),
    });

    const accounts = await handleResponse(response);

    return accounts.map((acc: any) => ({
      id: String(acc.id),
      accountNumber: acc.accountNumber,
      accountName: acc.customerName,
      accountType: mapAccountType(acc.accountType),
      balance: acc.balance,
      status: 'active' as const,
      customerId: String(acc.customerId),
      createdAt: acc.createdAt,
      updatedAt: acc.createdAt,
    }));
  }

  async getAccountById(accountId: string): Promise<Account | null> {
    try {
      const response = await fetch(`${API_BASE_URL}/api/accounts/${accountId}`, {
        headers: getAuthHeaders(),
      });

      const acc = await handleResponse(response);

      return {
        id: String(acc.id),
        accountNumber: acc.accountNumber,
        accountName: acc.customerName,
        accountType: mapAccountType(acc.accountType),
        balance: acc.balance,
        status: 'active' as const,
        customerId: String(acc.customerId),
        createdAt: acc.createdAt,
        updatedAt: acc.createdAt,
      };
    } catch {
      return null;
    }
  }

  async searchAccounts(query: string, page: number = 1, limit: number = 10): Promise<{
    accounts: Account[];
    total: number;
    page: number;
    totalPages: number;
  }> {
    const params = new URLSearchParams({
      query: query || '',
      page: String(page),
      limit: String(limit),
    });

    const response = await fetch(`${API_BASE_URL}/api/accounts/search?${params}`, {
      headers: getAuthHeaders(),
    });

    const data = await handleResponse(response);

    return {
      accounts: data.accounts.map((acc: any) => ({
        id: String(acc.id),
        accountNumber: acc.accountNumber,
        accountName: acc.customerName,
        accountType: mapAccountType(acc.accountType),
        balance: acc.balance,
        status: 'active' as const,
        customerId: String(acc.customerId),
        createdAt: acc.createdAt,
        updatedAt: acc.createdAt,
      })),
      total: data.totalAccounts,
      page: data.currentPage,
      totalPages: data.totalPages,
    };
  }

  // Transaction operations
  async getTransactionsByAccountId(accountId: string): Promise<Transaction[]> {
    const response = await fetch(`${API_BASE_URL}/api/accounts/${accountId}/transactions`, {
      headers: getAuthHeaders(),
    });

    const transactions = await handleResponse(response);

    return transactions.map((txn: any) => ({
      id: String(txn.id),
      accountId: accountId,
      type: this.mapBackendTransactionType(txn.type),
      amount: txn.amount,
      status: 'completed' as const,
      description: txn.description || '',
      createdAt: txn.createdAt,
      completedAt: txn.createdAt,
    }));
  }

  async createTransaction(
    accountId: string,
    type: Transaction['type'],
    amount: number,
    description: string
  ): Promise<Transaction> {
    const response = await fetch(`${API_BASE_URL}/api/accounts/${accountId}/transactions`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ type, amount, description }),
    });

    const txn = await handleResponse(response);

    return {
      id: String(txn.id),
      accountId: accountId,
      type: this.mapBackendTransactionType(txn.type),
      amount: txn.amount,
      status: 'completed' as const,
      description: txn.description || '',
      createdAt: txn.createdAt,
      completedAt: txn.createdAt,
    };
  }

  // User management (Admin operations)
  async getAllUsers(): Promise<User[]> {
    const response = await fetch(`${API_BASE_URL}/api/users`, {
      headers: getAuthHeaders(),
    });

    const users = await handleResponse(response);

    return users.map((user: any) => ({
      id: String(user.id),
      username: user.username,
      firstName: user.firstName,
      lastName: user.lastName,
      role: user.role,
      isActive: user.isActive,
      createdAt: user.createdAt,
    }));
  }

  async updateUserRole(userId: string, role: User['role']): Promise<User> {
    const response = await fetch(`${API_BASE_URL}/api/users/${userId}/role`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify({ role }),
    });

    const user = await handleResponse(response);

    return {
      id: String(user.id),
      username: user.username,
      firstName: user.firstName,
      lastName: user.lastName,
      role: user.role,
      isActive: user.isActive,
      createdAt: user.createdAt,
    };
  }

  async toggleUserStatus(userId: string): Promise<User> {
    const response = await fetch(`${API_BASE_URL}/api/users/${userId}/status`, {
      method: 'PUT',
      headers: getAuthHeaders(),
    });

    const user = await handleResponse(response);

    return {
      id: String(user.id),
      username: user.username,
      firstName: user.firstName,
      lastName: user.lastName,
      role: user.role,
      isActive: user.isActive,
      createdAt: user.createdAt,
    };
  }

  // Password reset requests (Admin operations)
  async getPasswordResetRequests(): Promise<PasswordResetRequest[]> {
    return [];
  }

  async approvePasswordResetRequest(requestId: string): Promise<void> {
    console.log('Approving password reset request:', requestId);
  }

  async rejectPasswordResetRequest(requestId: string): Promise<void> {
    console.log('Rejecting password reset request:', requestId);
  }

  private mapBackendTransactionType(backendType: string): Transaction['type'] {
    const typeMap: Record<string, Transaction['type']> = {
      'credit': 'deposit',
      'debit': 'withdrawal',
    };
    return typeMap[backendType.toLowerCase()] || 'deposit';
  }
}

export const api = new ApiService();
