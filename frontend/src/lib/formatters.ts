import { Account, AccountType, AccountStatus } from '@/types';

export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(amount);
}

export function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function getStatusColor(status: AccountStatus): string {
  const colors: Record<AccountStatus, string> = {
    active: 'bg-green-100 text-green-800',
    inactive: 'bg-gray-100 text-gray-800',
    suspended: 'bg-yellow-100 text-yellow-800',
    closed: 'bg-red-100 text-red-800',
  };
  return colors[status];
}

export function getAccountTypeLabel(type: AccountType): string {
  const labels: Record<AccountType, string> = {
    checking: 'Checking',
    savings: 'Savings',
    credit: 'Credit Card',
    money_market: 'Money Market',
    cd: 'Certificate of Deposit',
  };
  return labels[type];
}
