import { useState, useEffect } from 'react';
import { api } from '@/services/api';
import { User, PasswordResetRequest, Account, UserRole } from '@/types';
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
import { Users, Key, Search, Shield, ChevronLeft, ChevronRight } from 'lucide-react';
import { formatCurrency, formatDate } from '@/lib/formatters';

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState<'users' | 'passwords' | 'accounts'>('users');
  const [users, setUsers] = useState<User[]>([]);
  const [passwordResetRequests, setPasswordResetRequests] = useState<PasswordResetRequest[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<Account[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSearching, setIsSearching] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalResults, setTotalResults] = useState(0);
  const { toast } = useToast();

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setIsLoading(true);
    try {
      const [usersData, passwordRequestsData] = await Promise.all([
        api.getAllUsers(),
        api.getPasswordResetRequests(),
      ]);

      setUsers(usersData);
      setPasswordResetRequests(passwordRequestsData);
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Error',
        description: 'Failed to load admin data',
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleUpdateUserRole = async (userId: string, newRole: UserRole) => {
    try {
      await api.updateUserRole(userId, newRole);

      setUsers(users.map((u) => (u.id === userId ? { ...u, role: newRole } : u)));

      toast({
        title: 'Role Updated',
        description: 'User role has been updated successfully',
      });
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Error',
        description: 'Failed to update user role',
      });
    }
  };

  const handleToggleUserStatus = async (userId: string) => {
    try {
      await api.toggleUserStatus(userId);

      setUsers(users.map((u) => (u.id === userId ? { ...u, isActive: !u.isActive } : u)));

      toast({
        title: 'Status Updated',
        description: 'User status has been updated successfully',
      });
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Error',
        description: 'Failed to update user status',
      });
    }
  };

  const handleApprovePasswordReset = async (requestId: string) => {
    try {
      await api.approvePasswordResetRequest(requestId);

      setPasswordResetRequests(passwordResetRequests.filter((r) => r.id !== requestId));

      toast({
        title: 'Request Approved',
        description: 'Password reset request has been approved',
      });
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Error',
        description: 'Failed to approve password reset request',
      });
    }
  };

  const handleRejectPasswordReset = async (requestId: string) => {
    try {
      await api.rejectPasswordResetRequest(requestId);

      setPasswordResetRequests(passwordResetRequests.filter((r) => r.id !== requestId));

      toast({
        title: 'Request Rejected',
        description: 'Password reset request has been rejected',
      });
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Error',
        description: 'Failed to reject password reset request',
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

  if (isLoading) {
    return (
      <DashboardLayout title="Admin Dashboard">
        <div className="flex items-center justify-center min-h-[400px]">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto"></div>
            <p className="mt-4 text-gray-600">Loading...</p>
          </div>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout title="Admin Dashboard">
      <div className="space-y-6">
        {/* Stats */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Card>
            <CardHeader className="pb-3">
              <CardDescription>Total Users</CardDescription>
              <CardTitle className="text-3xl">{users.length}</CardTitle>
            </CardHeader>
          </Card>
          <Card>
            <CardHeader className="pb-3">
              <CardDescription>Active Users</CardDescription>
              <CardTitle className="text-3xl">
                {users.filter((u) => u.isActive).length}
              </CardTitle>
            </CardHeader>
          </Card>
          <Card>
            <CardHeader className="pb-3">
              <CardDescription>Pending Password Resets</CardDescription>
              <CardTitle className="text-3xl">{passwordResetRequests.length}</CardTitle>
            </CardHeader>
          </Card>
        </div>

        {/* Tabs */}
        <div className="flex gap-2 border-b">
          <Button
            variant={activeTab === 'users' ? 'default' : 'ghost'}
            onClick={() => setActiveTab('users')}
          >
            <Users className="mr-2 h-4 w-4" />
            User Management
          </Button>
          <Button
            variant={activeTab === 'passwords' ? 'default' : 'ghost'}
            onClick={() => setActiveTab('passwords')}
          >
            <Key className="mr-2 h-4 w-4" />
            Password Resets
            {passwordResetRequests.length > 0 && (
              <span className="ml-2 bg-red-500 text-white text-xs px-2 py-0.5 rounded-full">
                {passwordResetRequests.length}
              </span>
            )}
          </Button>
          <Button
            variant={activeTab === 'accounts' ? 'default' : 'ghost'}
            onClick={() => setActiveTab('accounts')}
          >
            <Search className="mr-2 h-4 w-4" />
            Account Search
          </Button>
        </div>

        {/* User Management Tab */}
        {activeTab === 'users' && (
          <Card>
            <CardHeader>
              <CardTitle>User Management</CardTitle>
              <CardDescription>Manage user roles and account status</CardDescription>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Name</TableHead>
                    <TableHead>Email</TableHead>
                    <TableHead>Role</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Created</TableHead>
                    <TableHead>Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {users.map((user) => (
                    <TableRow key={user.id}>
                      <TableCell className="font-medium">
                        {user.firstName} {user.lastName}
                      </TableCell>
                      <TableCell>{user.email}</TableCell>
                      <TableCell>
                        <select
                          value={user.role}
                          onChange={(e) =>
                            handleUpdateUserRole(user.id, e.target.value as UserRole)
                          }
                          className="flex h-8 rounded-md border border-input bg-transparent px-2 py-1 text-sm"
                        >
                          <option value="customer">Customer</option>
                          <option value="teller">Teller</option>
                          <option value="admin">Admin</option>
                        </select>
                      </TableCell>
                      <TableCell>
                        <span
                          className={`text-xs px-2 py-1 rounded-full ${
                            user.isActive
                              ? 'bg-green-100 text-green-800'
                              : 'bg-red-100 text-red-800'
                          }`}
                        >
                          {user.isActive ? 'Active' : 'Inactive'}
                        </span>
                      </TableCell>
                      <TableCell>{formatDate(user.createdAt)}</TableCell>
                      <TableCell>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleToggleUserStatus(user.id)}
                        >
                          {user.isActive ? 'Disable' : 'Enable'}
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        )}

        {/* Password Reset Requests Tab */}
        {activeTab === 'passwords' && (
          <Card>
            <CardHeader>
              <CardTitle>Password Reset Requests</CardTitle>
              <CardDescription>Review and approve pending password reset requests</CardDescription>
            </CardHeader>
            <CardContent>
              {passwordResetRequests.length === 0 ? (
                <div className="text-center py-12">
                  <Shield className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                  <p className="text-gray-600">No pending password reset requests</p>
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>User Email</TableHead>
                      <TableHead>Requested</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {passwordResetRequests.map((request) => (
                      <TableRow key={request.id}>
                        <TableCell className="font-medium">{request.userEmail}</TableCell>
                        <TableCell>{formatDate(request.requestedAt)}</TableCell>
                        <TableCell>
                          <span className="text-xs px-2 py-1 rounded-full bg-yellow-100 text-yellow-800">
                            {request.status}
                          </span>
                        </TableCell>
                        <TableCell>
                          <div className="flex gap-2">
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => handleApprovePasswordReset(request.id)}
                            >
                              Approve
                            </Button>
                            <Button
                              variant="destructive"
                              size="sm"
                              onClick={() => handleRejectPasswordReset(request.id)}
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

        {/* Account Search Tab */}
        {activeTab === 'accounts' && (
          <>
            <Card>
              <CardHeader>
                <CardTitle>Account Search</CardTitle>
                <CardDescription>Search for customer accounts</CardDescription>
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
                        {isSearching ? 'Searching...' : 'Search'}
                      </Button>
                    </div>
                  </div>
                </form>
              </CardContent>
            </Card>

            {searchResults.length > 0 && (
              <Card>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <div>
                      <CardTitle>Search Results</CardTitle>
                      <CardDescription>Found {totalResults} accounts</CardDescription>
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
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {searchResults.map((account) => (
                        <TableRow key={account.id}>
                          <TableCell className="font-medium">{account.accountName}</TableCell>
                          <TableCell>{account.accountNumber}</TableCell>
                          <TableCell className="capitalize">{account.accountType}</TableCell>
                          <TableCell className="text-right font-semibold">
                            {formatCurrency(account.balance)}
                          </TableCell>
                          <TableCell>
                            <span
                              className={`text-xs px-2 py-1 rounded-full ${
                                account.status === 'active'
                                  ? 'bg-green-100 text-green-800'
                                  : 'bg-gray-100 text-gray-800'
                              }`}
                            >
                              {account.status}
                            </span>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </CardContent>
              </Card>
            )}
          </>
        )}
      </div>
    </DashboardLayout>
  );
}
