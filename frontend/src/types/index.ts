export type UserRole = 'customer' | 'teller' | 'admin';

export type AccountStatus = 'active' | 'inactive' | 'suspended' | 'closed';

export type AccountType = 'checking' | 'savings' | 'credit';

export type TransactionType = 'deposit' | 'withdrawal' | 'transfer' | 'payment' | 'fee';

export type TransactionStatus = 'pending' | 'completed' | 'failed' | 'cancelled';

export interface User {
  id: string;
  username: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  isActive: boolean;
  createdAt: string;
  forcePasswordChange?: boolean;
}

export interface Account {
  id: string;
  accountNumber: string;
  accountName: string;
  accountType: AccountType;
  balance: number;
  status: AccountStatus;
  customerId: string;
  createdAt: string;
  updatedAt: string;
}

export interface Transaction {
  id: string;
  accountId: string;
  type: TransactionType;
  amount: number;
  status: TransactionStatus;
  description: string;
  createdAt: string;
  completedAt?: string;
}

export interface PasswordResetRequest {
  id: string;
  userId: string;
  userEmail: string;
  requestedAt: string;
  status: 'pending' | 'approved' | 'rejected';
}

export interface AccountRequest {
  id: string;
  userId: string;
  username: string;
  accountType: string;
  status: 'pending' | 'approved' | 'rejected';
  requestedAt: string;
  resolvedAt?: string;
}

export interface AccountDeletionRequest {
  id: string;
  userId: string;
  username: string;
  accountId: string;
  accountNumber: string;
  accountType: string;
  status: 'pending' | 'approved' | 'rejected';
  reason: string;
  requestedAt: string;
  resolvedAt?: string;
}

export interface AuthContextType {
  user: User | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
  isLoading: boolean;
}
