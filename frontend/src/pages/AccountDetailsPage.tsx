import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '@/services/api';
import { Account } from '@/types';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { useToast } from '@/hooks/use-toast';
import { ArrowLeft, Receipt, DollarSign, AlertCircle } from 'lucide-react';
import { formatCurrency, formatDate, getStatusColor, getAccountTypeLabel } from '@/lib/formatters';

export default function AccountDetailsPage() {
  const [account, setAccount] = useState<Account | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const { accountId } = useParams();
  const navigate = useNavigate();
  const { toast } = useToast();

  useEffect(() => {
    const fetchAccount = async () => {
      if (!accountId) return;

      try {
        const data = await api.getAccountById(accountId);
        setAccount(data);
      } catch (error) {
        toast({
          variant: 'destructive',
          title: 'Error',
          description: 'Failed to load account details',
        });
      } finally {
        setIsLoading(false);
      }
    };

    fetchAccount();
  }, [accountId, toast]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading account details...</p>
        </div>
      </div>
    );
  }

  if (!account) {
    return (
      <Card>
        <CardContent className="pt-6">
          <div className="text-center py-12">
            <AlertCircle className="h-12 w-12 text-gray-400 mx-auto mb-4" />
            <p className="text-gray-600">Account not found</p>
            <Button variant="outline" className="mt-4" onClick={() => navigate('/customer')}>
              <ArrowLeft className="mr-2 h-4 w-4" />
              Back to Dashboard
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  const hasInsufficientFunds = account.balance < 100;

  return (
    <div className="space-y-6">
      {/* Back Button */}
      <Button variant="ghost" onClick={() => navigate('/customer')}>
        <ArrowLeft className="mr-2 h-4 w-4" />
        Back to Dashboard
      </Button>

      {/* Account Header */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-2xl">{account.accountName}</CardTitle>
              <CardDescription className="mt-1">
                {getAccountTypeLabel(account.accountType)}
              </CardDescription>
            </div>
            <span className={`text-sm px-3 py-1 rounded-full ${getStatusColor(account.status)}`}>
              {account.status}
            </span>
          </div>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <p className="text-sm text-gray-500">Account Number</p>
              <p className="text-lg font-semibold">{account.accountNumber}</p>
            </div>
            <div>
              <p className="text-sm text-gray-500">Current Balance</p>
              <p className="text-3xl font-bold text-primary">{formatCurrency(account.balance)}</p>
            </div>
            <div>
              <p className="text-sm text-gray-500">Account Type</p>
              <p className="text-lg font-semibold">{getAccountTypeLabel(account.accountType)}</p>
            </div>
            <div>
              <p className="text-sm text-gray-500">Status</p>
              <p className="text-lg font-semibold capitalize">{account.status}</p>
            </div>
            <div>
              <p className="text-sm text-gray-500">Created On</p>
              <p className="text-lg font-semibold">{formatDate(account.createdAt)}</p>
            </div>
            <div>
              <p className="text-sm text-gray-500">Last Updated</p>
              <p className="text-lg font-semibold">{formatDate(account.updatedAt)}</p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Actions */}
      <Card>
        <CardHeader>
          <CardTitle>Account Actions</CardTitle>
          <CardDescription>Manage your account and view transactions</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Button
              variant="outline"
              className="h-auto py-4 flex-col items-start"
              onClick={() => navigate(`/customer/accounts/${account.id}/transactions`)}
            >
              <div className="flex items-center gap-2 mb-2">
                <Receipt className="h-5 w-5 text-primary" />
                <span className="font-semibold">View Transactions</span>
              </div>
              <span className="text-xs text-gray-500">
                View all transactions for this account
              </span>
            </Button>

            <Button
              variant="outline"
              className="h-auto py-4 flex-col items-start"
              onClick={() => navigate(`/customer/accounts/${account.id}/transactions?action=add`)}
            >
              <div className="flex items-center gap-2 mb-2">
                <DollarSign className="h-5 w-5 text-primary" />
                <span className="font-semibold">Add Transaction</span>
              </div>
              <span className="text-xs text-gray-500">
                Deposit or withdraw funds
              </span>
            </Button>

            <Button
              variant="outline"
              className="h-auto py-4 flex-col items-start"
              disabled={hasInsufficientFunds}
              onClick={() => {
                if (hasInsufficientFunds) {
                  toast({
                    variant: 'destructive',
                    title: 'Insufficient Funds',
                    description: 'You need at least $100 to make a payment.',
                  });
                } else {
                  navigate(`/customer/accounts/${account.id}/transactions?action=payment`);
                }
              }}
            >
              <div className="flex items-center gap-2 mb-2">
                <DollarSign className="h-5 w-5 text-primary" />
                <span className="font-semibold">Make Payment</span>
              </div>
              <span className="text-xs text-gray-500">
                {hasInsufficientFunds ? 'Insufficient funds' : 'Pay bills or transfer money'}
              </span>
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
