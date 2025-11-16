import { User, Account, Transaction, PasswordResetRequest } from '@/types';
import {
  mockUsers,
  mockAccounts,
  mockTransactions,
  mockPasswordResetRequests,
  mockCredentials,
} from './mockData';

// Simulate network delay
const delay = (ms: number = 800) => new Promise((resolve) => setTimeout(resolve, ms));

class ApiService {
  // Authentication
  async login(email: string, password: string): Promise<User> {
    await delay();

    const validPassword = mockCredentials[email as keyof typeof mockCredentials];

    if (!validPassword || validPassword !== password) {
      throw new Error('Invalid credentials');
    }

    const user = mockUsers.find((u) => u.email === email);

    if (!user) {
      throw new Error('User not found');
    }

    if (!user.isActive) {
      throw new Error('Account is disabled. Please contact support.');
    }

    return user;
  }

  async requestPasswordReset(email: string): Promise<void> {
    await delay();

    const user = mockUsers.find((u) => u.email === email);

    if (!user) {
      // Don't reveal if user exists
      return;
    }

    // In a real app, this would create a password reset request
    console.log('Password reset requested for:', email);
  }

  // Account operations
  async getAccountsByCustomerId(customerId: string): Promise<Account[]> {
    await delay();
    return mockAccounts.filter((acc) => acc.customerId === customerId);
  }

  async getAccountById(accountId: string): Promise<Account | null> {
    await delay();
    return mockAccounts.find((acc) => acc.id === accountId) || null;
  }

  async searchAccounts(query: string, page: number = 1, limit: number = 10): Promise<{
    accounts: Account[];
    total: number;
    page: number;
    totalPages: number;
  }> {
    await delay(1000);

    let filtered = mockAccounts;

    if (query) {
      const lowerQuery = query.toLowerCase();
      filtered = mockAccounts.filter(
        (acc) =>
          acc.accountNumber.includes(query) ||
          acc.accountName.toLowerCase().includes(lowerQuery)
      );
    }

    const start = (page - 1) * limit;
    const end = start + limit;
    const paginated = filtered.slice(start, end);

    return {
      accounts: paginated,
      total: filtered.length,
      page,
      totalPages: Math.ceil(filtered.length / limit),
    };
  }

  // Transaction operations
  async getTransactionsByAccountId(accountId: string): Promise<Transaction[]> {
    await delay();
    return mockTransactions.filter((txn) => txn.accountId === accountId);
  }

  async createTransaction(
    accountId: string,
    type: Transaction['type'],
    amount: number,
    description: string
  ): Promise<Transaction> {
    await delay(1200);

    const account = mockAccounts.find((acc) => acc.id === accountId);

    if (!account) {
      throw new Error('Account not found');
    }

    // Check balance for withdrawals and payments
    if ((type === 'withdrawal' || type === 'payment') && account.balance < amount) {
      throw new Error('Insufficient funds');
    }

    const newTransaction: Transaction = {
      id: `txn-${Date.now()}`,
      accountId,
      type,
      amount,
      status: 'completed',
      description,
      createdAt: new Date().toISOString(),
      completedAt: new Date().toISOString(),
    };

    // Update account balance (in-memory only for mock)
    if (type === 'deposit') {
      account.balance += amount;
    } else if (type === 'withdrawal' || type === 'payment') {
      account.balance -= amount;
    }

    return newTransaction;
  }

  // User management (Admin operations)
  async getAllUsers(): Promise<User[]> {
    await delay();
    return mockUsers;
  }

  async updateUserRole(userId: string, role: User['role']): Promise<User> {
    await delay();

    const user = mockUsers.find((u) => u.id === userId);

    if (!user) {
      throw new Error('User not found');
    }

    user.role = role;
    return user;
  }

  async toggleUserStatus(userId: string): Promise<User> {
    await delay();

    const user = mockUsers.find((u) => u.id === userId);

    if (!user) {
      throw new Error('User not found');
    }

    user.isActive = !user.isActive;
    return user;
  }

  // Password reset requests (Admin operations)
  async getPasswordResetRequests(): Promise<PasswordResetRequest[]> {
    await delay();
    return mockPasswordResetRequests.filter((req) => req.status === 'pending');
  }

  async approvePasswordResetRequest(requestId: string): Promise<void> {
    await delay();

    const request = mockPasswordResetRequests.find((req) => req.id === requestId);

    if (request) {
      request.status = 'approved';
    }
  }

  async rejectPasswordResetRequest(requestId: string): Promise<void> {
    await delay();

    const request = mockPasswordResetRequests.find((req) => req.id === requestId);

    if (request) {
      request.status = 'rejected';
    }
  }
}

export const api = new ApiService();
