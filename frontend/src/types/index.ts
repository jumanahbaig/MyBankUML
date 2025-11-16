export type UserRole = 'customer' | 'teller' | 'admin';

export type AccountStatus = 'active' | 'inactive' | 'suspended' | 'closed';

export type AccountType = 'checking' | 'savings' | 'money_market' | 'cd';

export type TransactionType = 'deposit' | 'withdrawal' | 'transfer' | 'payment' | 'fee';

export type TransactionStatus = 'pending' | 'completed' | 'failed' | 'cancelled';

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  isActive: boolean;
  createdAt: string;
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

export interface AuthContextType {
  user: User | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
  isLoading: boolean;
}
