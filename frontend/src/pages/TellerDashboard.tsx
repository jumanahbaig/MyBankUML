import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '@/services/api';
import { Account, AccountRequest } from '@/types';
import DashboardLayout from '@/components/DashboardLayout';
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
import { Search, ChevronLeft, ChevronRight, UserPlus, Shield } from 'lucide-react';
import { formatCurrency, getStatusColor, getAccountTypeLabel, formatDate } from '@/lib/formatters';

export default function TellerDashboard() {
  const [activeTab, setActiveTab] = useState<'search' | 'requests'>('search');
  const [accountRequests, setAccountRequests] = useState<AccountRequest[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<Account[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalResults, setTotalResults] = useState(0);
  const [selectedAccount, setSelectedAccount] = useState<Account | null>(null);
  const { toast } = useToast();
  const navigate = useNavigate();

  // Load account requests
  useState(() => {
    const loadRequests = async () => {
      try {
        const requests = await api.getAccountRequests();
        setAccountRequests(requests);
      } catch (error) {
        console.error('Failed to load account requests', error);
      }
    };

    loadRequests();
    const interval = setInterval(loadRequests, 5000);
    return () => clearInterval(interval);
  });

  const handleApproveAccountRequest = async (requestId: string) => {
    try {
      await api.approveAccountRequest(requestId);
      toast({
        title: 'Request Approved',
        description: 'Account request has been approved and account created',
      });
      // Refresh requests
      const requests = await api.getAccountRequests();
      setAccountRequests(requests);
    } catch (error: any) {
      toast({
        variant: 'destructive',
        title: 'Error',
        description: error.message || 'Failed to approve account request',
      });
    }
  };

  const handleRejectAccountRequest = async (requestId: string) => {
    try {
      await api.rejectAccountRequest(requestId);
      toast({
        title: 'Request Rejected',
        description: 'Account request has been rejected',
      });
      // Refresh requests
      const requests = await api.getAccountRequests();
      setAccountRequests(requests);
    } catch (error: any) {
      toast({
        variant: 'destructive',
        title: 'Error',
        description: error.message || 'Failed to reject account request',
      });
    }
  };

  const handleSearch = async (page: number = 1) => {
    if (searchQuery.trim().length < 2) {
      toast({
        variant: 'destructive',
        title: 'Validation Error',
        description: 'Please enter at least 2 characters to search',
      });
      return;
    }

    setIsSearching(true);

    try {
      const results = await api.searchAccounts(searchQuery, page, 10);
      setSearchResults(results.accounts);
      setCurrentPage(results.page);
      setTotalPages(results.totalPages);
      setTotalResults(results.total);

      if (results.accounts.length === 0) {
        toast({
          title: 'No Results',
          description: 'No accounts found matching your search',
        });
      }
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Search Failed',
        description: 'An error occurred while searching',
      });
    } finally {
      setIsSearching(false);
    }
  };

  return (
    <DashboardLayout title="Teller Dashboard">
      <div className="space-y-6">
        {/* Tabs */}
        <div className="flex gap-2 border-b">
          <Button
            variant={activeTab === 'search' ? 'default' : 'ghost'}
            onClick={() => setActiveTab('search')}
          >
            <Search className="mr-2 h-4 w-4" />
            Account Search
          </Button>
          <Button
            variant={activeTab === 'requests' ? 'default' : 'ghost'}
            onClick={() => setActiveTab('requests')}
          >
            <Shield className="mr-2 h-4 w-4" />
            Account Requests
            {accountRequests.length > 0 && (
              <span className="ml-2 bg-blue-500 text-white text-xs px-2 py-0.5 rounded-full">
                {accountRequests.length}
              </span>
            )}
          </Button>
        </div>

        {activeTab === 'search' ? (
          <>
            {/* Quick Actions */}
            <Card>
              <CardHeader>
                <CardTitle>Quick Actions</CardTitle>
                <CardDescription>Common teller operations</CardDescription>
              </CardHeader>
              <CardContent>
                <Button onClick={() => navigate('/create-user')}>
                  <UserPlus className="mr-2 h-4 w-4" />
                  Create Customer
                </Button>
              </CardContent>
            </Card>

            {/* Search Form */}
            <Card>
              <CardHeader>
                <CardTitle>Account Search</CardTitle>
                <CardDescription>
                  Search for customer accounts by account number or name
                </CardDescription>
              </CardHeader>
              <CardContent>
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    handleSearch(1);
                  }}
                  className="space-y-4"
                >
                  <div className="space-y-2">
                    <Label htmlFor="search">Search Query</Label>
                    <div className="flex gap-2">
                      <Input
                        id="search"
                        type="text"
                        placeholder="Enter account number or name..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        disabled={isSearching}
                      />
                      <Button type="submit" disabled={isSearching}>
                        {isSearching ? (
                          'Searching...'
                        ) : (
                          <>
                            <Search className="mr-2 h-4 w-4" />
                            Search
                          </>
                        )}
                      </Button>
                    </div>
                    <p className="text-sm text-gray-500">
                      Minimum 2 characters required
                    </p>
                  </div>
                </form>
              </CardContent>
            </Card>

            {/* Search Results */}
            {searchResults.length > 0 && (
              <Card>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <div>
                      <CardTitle>Search Results</CardTitle>
                      <CardDescription>
                        Found {totalResults} account{totalResults !== 1 ? 's' : ''}
                      </CardDescription>
                    </div>
                    {totalPages > 1 && (
                      <div className="flex items-center gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleSearch(currentPage - 1)}
                          disabled={currentPage === 1 || isSearching}
                        >
                          <ChevronLeft className="h-4 w-4" />
                        </Button>
                        <span className="text-sm text-gray-600">
                          Page {currentPage} of {totalPages}
                        </span>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleSearch(currentPage + 1)}
                          disabled={currentPage === totalPages || isSearching}
                        >
                          <ChevronRight className="h-4 w-4" />
                        </Button>
                      </div>
                    )}
                  </div>
                </CardHeader>
                <CardContent>
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Account Name</TableHead>
                        <TableHead>Account Number</TableHead>
                        <TableHead>Type</TableHead>
                        <TableHead className="text-right">Balance</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead>Actions</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {searchResults.map((account) => (
                        <TableRow key={account.id}>
                          <TableCell className="font-medium">{account.accountName}</TableCell>
                          <TableCell>{account.accountNumber}</TableCell>
                          <TableCell>{getAccountTypeLabel(account.accountType)}</TableCell>
                          <TableCell className="text-right font-semibold">
                            {formatCurrency(account.balance)}
                          </TableCell>
                          <TableCell>
                            <span className={`text-xs px-2 py-1 rounded-full ${getStatusColor(account.status)}`}>
                              {account.status}
                            </span>
                          </TableCell>
                          <TableCell>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => setSelectedAccount(account)}
                            >
                              View Details
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </CardContent>
              </Card>
            )}
          </>
        ) : (
          /* Account Requests Tab */
          <Card>
            <CardHeader>
              <CardTitle>Account Requests</CardTitle>
              <CardDescription>Review and approve pending new account requests</CardDescription>
            </CardHeader>
            <CardContent>
              {accountRequests.length === 0 ? (
                <div className="text-center py-12">
                  <Shield className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                  <p className="text-gray-600">No pending account requests</p>
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Username</TableHead>
                      <TableHead>Account Type</TableHead>
                      <TableHead>Requested</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {accountRequests.map((request) => (
                      <TableRow key={request.id}>
                        <TableCell className="font-medium">{request.username}</TableCell>
                        <TableCell className="capitalize">{request.accountType}</TableCell>
                        <TableCell>{formatDate(request.requestedAt)}</TableCell>
                        <TableCell>
                          <span className="text-xs px-2 py-1 rounded-full bg-blue-100 text-blue-800">
                            {request.status}
                          </span>
                        </TableCell>
                        <TableCell>
                          <div className="flex gap-2">
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => handleApproveAccountRequest(request.id)}
                            >
                              Approve
                            </Button>
                            <Button
                              variant="destructive"
                              size="sm"
                              onClick={() => handleRejectAccountRequest(request.id)}
                            >
                              Reject
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        )}

        {/* Account Details Modal */}
        {selectedAccount && (
          <Card className="border-primary">
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle>Account Details (Read-Only)</CardTitle>
                  <CardDescription>
                    Viewing details for {selectedAccount.accountName}
                  </CardDescription>
                </div>
                <Button variant="ghost" size="sm" onClick={() => setSelectedAccount(null)}>
                  Close
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <p className="text-sm text-gray-500">Account Name</p>
                  <p className="text-lg font-semibold">{selectedAccount.accountName}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Account Number</p>
                  <p className="text-lg font-semibold">{selectedAccount.accountNumber}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Account Type</p>
                  <p className="text-lg font-semibold">
                    {getAccountTypeLabel(selectedAccount.accountType)}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Status</p>
                  <p className="text-lg font-semibold capitalize">{selectedAccount.status}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Current Balance</p>
                  <p className="text-lg font-semibold text-primary">
                    {formatCurrency(selectedAccount.balance)}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Created On</p>
                  <p className="text-lg font-semibold">
                    {new Date(selectedAccount.createdAt).toLocaleDateString()}
                  </p>
                </div>
              </div>
              <div className="mt-6 p-4 bg-muted rounded-md">
                <p className="text-sm text-gray-600">
                  <strong>Note:</strong> As a teller, you can only view account information. You
                  cannot modify or perform transactions on customer accounts.
                </p>
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </DashboardLayout>
  );
}
