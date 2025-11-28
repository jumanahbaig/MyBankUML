import { useState, useEffect } from 'react';
import { Routes, Route, useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { api } from '@/services/api';
import { Account } from '@/types';
import DashboardLayout from '@/components/DashboardLayout';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { useToast } from '@/hooks/use-toast';
import { Wallet, ArrowRight, AlertCircle, Plus, Trash2 } from 'lucide-react';
import { formatCurrency, getStatusColor, getAccountTypeLabel } from '@/lib/formatters';
import AccountDetailsPage from './AccountDetailsPage';
import TransactionsPage from './TransactionsPage';

function AccountsList() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const { user } = useAuth();
  const navigate = useNavigate();
  const [isRequesting, setIsRequesting] = useState(false);
  const [selectedAccountType, setSelectedAccountType] = useState('checking');
  const { toast } = useToast();

  const handleRequestAccount = async () => {
    try {
      await api.requestAccount(selectedAccountType);
      toast({
        title: 'Request Sent',
        description: 'Your account request has been sent for approval.',
      });
      setIsRequesting(false);
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Error',
        description: 'Failed to send account request.',
      });
    }
  };

  useEffect(() => {
    let isMounted = true;

    const fetchAccounts = async (showLoading = false) => {
      if (!user) return;

      if (showLoading) setIsLoading(true);
      try {
        const data = await api.getAccountsByCustomerId(user.id);
        if (isMounted) {
          setAccounts(data);
        }
      } catch (error) {
        // Only show toast on initial load error to avoid spamming
        if (showLoading) {
          toast({
            variant: 'destructive',
            title: 'Error',
            description: 'Failed to load accounts',
          });
        }
      } finally {
        if (isMounted && showLoading) {
          setIsLoading(false);
        }
      }
    };

    // Initial fetch
    fetchAccounts(true);

    // Poll every 3 seconds
    const intervalId = setInterval(() => fetchAccounts(false), 3000);

    return () => {
      isMounted = false;
      clearInterval(intervalId);
    };
  }, [user, toast]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading accounts...</p>
        </div>
      </div>
    );
  }

  if (accounts.length === 0) {
    return (
      <Card>
        <CardContent className="pt-6">
          <div className="text-center py-12">
            <AlertCircle className="h-12 w-12 text-gray-400 mx-auto mb-4" />
            <p className="text-gray-600">No accounts found</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  const handleDeleteRequest = async (accountId: string) => {
    const reason = window.prompt("Please provide a reason for deleting this account:");
    if (reason === null) return; // User cancelled

    try {
      await api.requestAccountDeletion(accountId, reason);
      toast({
        title: 'Request Sent',
        description: 'Your account deletion request has been sent for approval.',
      });
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Error',
        description: 'Failed to send deletion request.',
      });
    }
  };

  return (
    <div className="space-y-6">
      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <CardHeader className="pb-3">
            <CardDescription>Total Accounts</CardDescription>
            <CardTitle className="text-3xl">{accounts.length}</CardTitle>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader className="pb-3">
            <CardDescription>Total Balance</CardDescription>
            <CardTitle className="text-3xl">
              {formatCurrency(accounts.reduce((sum, acc) => sum + acc.balance, 0))}
            </CardTitle>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader className="pb-3">
            <CardDescription>Active Accounts</CardDescription>
            <CardTitle className="text-3xl">
              {accounts.filter((acc) => acc.status === 'active').length}
            </CardTitle>
          </CardHeader>
        </Card>
      </div>

      {/* Accounts List */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>My Accounts</CardTitle>
            <CardDescription>View and manage your bank accounts</CardDescription>
          </div>
          <Button onClick={() => setIsRequesting(!isRequesting)}>
            <Plus className="mr-2 h-4 w-4" />
            Request New Account
          </Button>
        </CardHeader>
        <CardContent>
          {isRequesting && (
            <div className="mb-6 p-4 border rounded-lg bg-gray-50">
              <h4 className="font-semibold mb-2">Select Account Type</h4>
              <div className="flex gap-2">
                <select
                  value={selectedAccountType}
                  onChange={(e) => setSelectedAccountType(e.target.value)}
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <option value="savings">Savings Account</option>
                  <option value="credit">Credit Card</option>
                </select>
                <Button onClick={handleRequestAccount}>Submit Request</Button>
                <Button variant="ghost" onClick={() => setIsRequesting(false)}>Cancel</Button>
              </div>
            </div>
          )}
          <div className="space-y-4">
            {accounts.map((account) => (
              <div
                key={account.id}
                className="flex items-center justify-between p-4 border rounded-lg hover:bg-gray-50 transition-colors"
              >
                <div className="flex items-center gap-4">
                  <div className="h-12 w-12 rounded-full bg-primary/10 flex items-center justify-center">
                    <Wallet className="h-6 w-6 text-primary" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900">{account.accountName}</h3>
                    <div className="flex items-center gap-2 mt-1">
                      <p className="text-sm text-gray-500">
                        {getAccountTypeLabel(account.accountType)} • {account.accountNumber}
                      </p>
                      <span
                        className={`text-xs px-2 py-0.5 rounded-full ${getStatusColor(
                          account.status
                        )}`}
                      >
                        {account.status}
                      </span>
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-4">
                  <div className="text-right">
                    <p className="text-lg font-semibold text-gray-900">
                      {formatCurrency(account.balance)}
                    </p>
                    <p className="text-xs text-gray-500">Current Balance</p>
                  </div>
                  {account.accountType.toLowerCase() !== 'checking' && account.accountType !== 'CHECK' && (
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-red-600 hover:text-red-700 hover:bg-red-50"
                      onClick={() => handleDeleteRequest(account.id)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  )}
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => navigate(`/customer/accounts/${account.id}`)}
                  >
                    View Details
                    <ArrowRight className="ml-2 h-4 w-4" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div >
  );
}

export default function CustomerDashboard() {
  return (
    <DashboardLayout title="Dashboard">
      <Routes>
        <Route index element={<AccountsList />} />
        <Route path="accounts/:accountId" element={<AccountDetailsPage />} />
        <Route path="accounts/:accountId/transactions" element={<TransactionsPage />} />
      </Routes>
    </DashboardLayout>
  );
}
