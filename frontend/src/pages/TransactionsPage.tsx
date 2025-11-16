import { useState, useEffect } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '@/services/api';
import { Transaction, Account } from '@/types';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { useToast } from '@/hooks/use-toast';
import { ArrowLeft, Plus, ArrowDownUp, Filter } from 'lucide-react';
import { formatCurrency, formatDate } from '@/lib/formatters';

export default function TransactionsPage() {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [account, setAccount] = useState<Account | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [filterType, setFilterType] = useState<string>('all');
  const [sortField, setSortField] = useState<'createdAt' | 'amount'>('createdAt');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  const [showAddTransaction, setShowAddTransaction] = useState(false);
  const [amount, setAmount] = useState('');
  const [type, setType] = useState<Transaction['type']>('deposit');
  const [description, setDescription] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { accountId } = useParams();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { toast } = useToast();

  useEffect(() => {
    const action = searchParams.get('action');
    if (action === 'add' || action === 'payment') {
      setShowAddTransaction(true);
      if (action === 'payment') {
        setType('payment');
      }
    }
  }, [searchParams]);

  useEffect(() => {
    const fetchData = async () => {
      if (!accountId) return;

      try {
        const [transactionsData, accountData] = await Promise.all([
          api.getTransactionsByAccountId(accountId),
          api.getAccountById(accountId),
        ]);

        setTransactions(transactionsData);
        setAccount(accountData);
      } catch (error) {
        toast({
          variant: 'destructive',
          title: 'Error',
          description: 'Failed to load transactions',
        });
      } finally {
        setIsLoading(false);
      }
    };

    fetchData();
  }, [accountId, toast]);

  const getTransactionIcon = (type: Transaction['type']) => {
    return type === 'deposit' ? '↑' : '↓';
  };

  const getTransactionColor = (type: Transaction['type']) => {
    return type === 'deposit' ? 'text-green-600' : 'text-red-600';
  };

  const handleAddTransaction = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!accountId || !amount || !description) {
      toast({
        variant: 'destructive',
        title: 'Validation Error',
        description: 'Please fill in all fields',
      });
      return;
    }

    const amountNum = parseFloat(amount);

    if (isNaN(amountNum) || amountNum <= 0) {
      toast({
        variant: 'destructive',
        title: 'Invalid Amount',
        description: 'Please enter a valid amount',
      });
      return;
    }

    setIsSubmitting(true);

    try {
      const newTransaction = await api.createTransaction(
        accountId,
        type,
        amountNum,
        description
      );

      setTransactions([newTransaction, ...transactions]);
      setShowAddTransaction(false);
      setAmount('');
      setDescription('');
      setType('deposit');

      // Refresh account data to update balance
      const accountData = await api.getAccountById(accountId);
      setAccount(accountData);

      toast({
        title: 'Transaction Successful',
        description: `${type === 'deposit' ? 'Deposited' : type === 'payment' ? 'Paid' : 'Withdrew'} ${formatCurrency(amountNum)}`,
      });
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Transaction Failed',
        description: error instanceof Error ? error.message : 'An error occurred',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const filteredTransactions = transactions
    .filter((txn) => filterType === 'all' || txn.type === filterType)
    .sort((a, b) => {
      const aVal = sortField === 'createdAt' ? new Date(a.createdAt).getTime() : a.amount;
      const bVal = sortField === 'createdAt' ? new Date(b.createdAt).getTime() : b.amount;

      return sortOrder === 'asc' ? aVal - bVal : bVal - aVal;
    });

  const totalAmount = filteredTransactions.reduce((sum, txn) => {
    return sum + (txn.type === 'deposit' ? txn.amount : -txn.amount);
  }, 0);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading transactions...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Back Button */}
      <Button variant="ghost" onClick={() => navigate(`/customer/accounts/${accountId}`)}>
        <ArrowLeft className="mr-2 h-4 w-4" />
        Back to Account Details
      </Button>

      {/* Account Info */}
      {account && (
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle>{account.accountName}</CardTitle>
                <CardDescription>Account #{account.accountNumber}</CardDescription>
              </div>
              <div className="text-right">
                <p className="text-sm text-gray-500">Current Balance</p>
                <p className="text-2xl font-bold text-primary">
                  {formatCurrency(account.balance)}
                </p>
              </div>
            </div>
          </CardHeader>
        </Card>
      )}

      {/* Add Transaction Form */}
      {showAddTransaction && (
        <Card>
          <CardHeader>
            <CardTitle>Add Transaction</CardTitle>
            <CardDescription>Deposit or withdraw funds from this account</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleAddTransaction} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="type">Transaction Type</Label>
                  <select
                    id="type"
                    value={type}
                    onChange={(e) => setType(e.target.value as Transaction['type'])}
                    className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                    disabled={isSubmitting}
                  >
                    <option value="deposit">Deposit</option>
                    <option value="withdrawal">Withdrawal</option>
                    <option value="payment">Payment</option>
                  </select>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="amount">Amount</Label>
                  <Input
                    id="amount"
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="0.00"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    disabled={isSubmitting}
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="description">Description</Label>
                <Input
                  id="description"
                  type="text"
                  placeholder="Enter transaction description"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  disabled={isSubmitting}
                />
              </div>

              <div className="flex gap-2">
                <Button type="submit" disabled={isSubmitting}>
                  {isSubmitting ? 'Processing...' : 'Submit Transaction'}
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => {
                    setShowAddTransaction(false);
                    setAmount('');
                    setDescription('');
                  }}
                  disabled={isSubmitting}
                >
                  Cancel
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      {/* Transactions List */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Transactions</CardTitle>
              <CardDescription>All transactions for this account</CardDescription>
            </div>
            {!showAddTransaction && (
              <Button onClick={() => setShowAddTransaction(true)}>
                <Plus className="mr-2 h-4 w-4" />
                Add Transaction
              </Button>
            )}
          </div>
        </CardHeader>
        <CardContent>
          {/* Filters */}
          <div className="flex flex-wrap gap-4 mb-4">
            <div className="flex items-center gap-2">
              <Filter className="h-4 w-4 text-gray-500" />
              <select
                value={filterType}
                onChange={(e) => setFilterType(e.target.value)}
                className="flex h-9 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm"
              >
                <option value="all">All Types</option>
                <option value="deposit">Deposits</option>
                <option value="withdrawal">Withdrawals</option>
                <option value="payment">Payments</option>
              </select>
            </div>

            <div className="flex items-center gap-2">
              <ArrowDownUp className="h-4 w-4 text-gray-500" />
              <select
                value={`${sortField}-${sortOrder}`}
                onChange={(e) => {
                  const [field, order] = e.target.value.split('-');
                  setSortField(field as 'createdAt' | 'amount');
                  setSortOrder(order as 'asc' | 'desc');
                }}
                className="flex h-9 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm"
              >
                <option value="createdAt-desc">Newest First</option>
                <option value="createdAt-asc">Oldest First</option>
                <option value="amount-desc">Highest Amount</option>
                <option value="amount-asc">Lowest Amount</option>
              </select>
            </div>
          </div>

          {/* Summary */}
          <div className="mb-4 p-4 bg-muted rounded-lg">
            <div className="grid grid-cols-3 gap-4 text-center">
              <div>
                <p className="text-sm text-gray-500">Total Transactions</p>
                <p className="text-xl font-semibold">{filteredTransactions.length}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Total Amount</p>
                <p className={`text-xl font-semibold ${totalAmount >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                  {formatCurrency(Math.abs(totalAmount))}
                </p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Net Change</p>
                <p className={`text-xl font-semibold ${totalAmount >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                  {totalAmount >= 0 ? '+' : '-'}{formatCurrency(Math.abs(totalAmount))}
                </p>
              </div>
            </div>
          </div>

          {/* Table */}
          {filteredTransactions.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-gray-600">No transactions found</p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Date</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead className="text-right">Amount</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredTransactions.map((txn) => (
                  <TableRow key={txn.id}>
                    <TableCell className="font-medium">{formatDate(txn.createdAt)}</TableCell>
                    <TableCell className="capitalize">
                      <span className={getTransactionColor(txn.type)}>
                        {getTransactionIcon(txn.type)} {txn.type}
                      </span>
                    </TableCell>
                    <TableCell>{txn.description}</TableCell>
                    <TableCell className={`text-right font-semibold ${getTransactionColor(txn.type)}`}>
                      {txn.type === 'deposit' ? '+' : '-'}{formatCurrency(txn.amount)}
                    </TableCell>
                    <TableCell>
                      <span className={`text-xs px-2 py-1 rounded-full ${
                        txn.status === 'completed'
                          ? 'bg-green-100 text-green-800'
                          : txn.status === 'pending'
                          ? 'bg-yellow-100 text-yellow-800'
                          : 'bg-red-100 text-red-800'
                      }`}>
                        {txn.status}
                      </span>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
