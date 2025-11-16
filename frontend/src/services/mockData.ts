import { User, Account, Transaction, PasswordResetRequest } from '@/types';

// Mock Users
export const mockUsers: User[] = [
  {
    id: 'user-1',
    email: 'customer@example.com',
    firstName: 'John',
    lastName: 'Doe',
    role: 'customer',
    isActive: true,
    createdAt: '2024-01-15T10:00:00Z',
  },
  {
    id: 'user-2',
    email: 'teller@example.com',
    firstName: 'Jane',
    lastName: 'Smith',
    role: 'teller',
    isActive: true,
    createdAt: '2024-01-10T10:00:00Z',
  },
  {
    id: 'user-3',
    email: 'admin@example.com',
    firstName: 'Admin',
    lastName: 'User',
    role: 'admin',
    isActive: true,
    createdAt: '2024-01-01T10:00:00Z',
  },
  {
    id: 'user-4',
    email: 'customer2@example.com',
    firstName: 'Alice',
    lastName: 'Johnson',
    role: 'customer',
    isActive: true,
    createdAt: '2024-02-01T10:00:00Z',
  },
  {
    id: 'user-5',
    email: 'inactive@example.com',
    firstName: 'Bob',
    lastName: 'Williams',
    role: 'customer',
    isActive: false,
    createdAt: '2023-12-01T10:00:00Z',
  },
];

// Mock Accounts
export const mockAccounts: Account[] = [
  {
    id: 'acc-1',
    accountNumber: '1234567890',
    accountName: 'Primary Checking',
    accountType: 'checking',
    balance: 5420.50,
    status: 'active',
    customerId: 'user-1',
    createdAt: '2024-01-15T10:00:00Z',
    updatedAt: '2024-03-10T14:30:00Z',
  },
  {
    id: 'acc-2',
    accountNumber: '1234567891',
    accountName: 'Savings Account',
    accountType: 'savings',
    balance: 15000.00,
    status: 'active',
    customerId: 'user-1',
    createdAt: '2024-01-15T10:00:00Z',
    updatedAt: '2024-03-09T09:15:00Z',
  },
  {
    id: 'acc-3',
    accountNumber: '2234567890',
    accountName: 'Business Checking',
    accountType: 'checking',
    balance: 25000.75,
    status: 'active',
    customerId: 'user-4',
    createdAt: '2024-02-01T10:00:00Z',
    updatedAt: '2024-03-11T11:20:00Z',
  },
  {
    id: 'acc-4',
    accountNumber: '1234567892',
    accountName: 'Emergency Fund',
    accountType: 'money_market',
    balance: 50000.00,
    status: 'active',
    customerId: 'user-1',
    createdAt: '2024-01-20T10:00:00Z',
    updatedAt: '2024-03-05T16:45:00Z',
  },
];

// Mock Transactions
export const mockTransactions: Transaction[] = [
  {
    id: 'txn-1',
    accountId: 'acc-1',
    type: 'deposit',
    amount: 1000.00,
    status: 'completed',
    description: 'Payroll Deposit',
    createdAt: '2024-03-10T08:00:00Z',
    completedAt: '2024-03-10T08:00:01Z',
  },
  {
    id: 'txn-2',
    accountId: 'acc-1',
    type: 'withdrawal',
    amount: 150.00,
    status: 'completed',
    description: 'ATM Withdrawal',
    createdAt: '2024-03-09T14:30:00Z',
    completedAt: '2024-03-09T14:30:01Z',
  },
  {
    id: 'txn-3',
    accountId: 'acc-1',
    type: 'payment',
    amount: 85.50,
    status: 'completed',
    description: 'Electric Bill Payment',
    createdAt: '2024-03-08T10:15:00Z',
    completedAt: '2024-03-08T10:15:02Z',
  },
  {
    id: 'txn-4',
    accountId: 'acc-2',
    type: 'deposit',
    amount: 500.00,
    status: 'completed',
    description: 'Transfer from Checking',
    createdAt: '2024-03-07T09:00:00Z',
    completedAt: '2024-03-07T09:00:01Z',
  },
  {
    id: 'txn-5',
    accountId: 'acc-1',
    type: 'payment',
    amount: 1200.00,
    status: 'completed',
    description: 'Rent Payment',
    createdAt: '2024-03-01T07:00:00Z',
    completedAt: '2024-03-01T07:00:01Z',
  },
];

// Mock Password Reset Requests
export const mockPasswordResetRequests: PasswordResetRequest[] = [
  {
    id: 'reset-1',
    userId: 'user-4',
    userEmail: 'customer2@example.com',
    requestedAt: '2024-03-11T15:30:00Z',
    status: 'pending',
  },
  {
    id: 'reset-2',
    userId: 'user-1',
    userEmail: 'customer@example.com',
    requestedAt: '2024-03-10T12:00:00Z',
    status: 'approved',
  },
];

// Mock login credentials (for development only)
export const mockCredentials = {
  'customer@example.com': 'password123',
  'teller@example.com': 'password123',
  'admin@example.com': 'password123',
  'customer2@example.com': 'password123',
};
