import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '@/services/api';
import { User, PasswordResetRequest, Account, UserRole, AccountRequest, AccountDeletionRequest } from '@/types';
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
import { Users, Key, Search, Shield, ChevronLeft, ChevronRight, UserPlus, Trash2 } from 'lucide-react';
import { formatCurrency, formatDate } from '@/lib/formatters';

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState<'users' | 'passwords' | 'accounts' | 'account-requests' | 'deletion-requests'>('users');
  const [users, setUsers] = useState<User[]>([]);
  const [passwordResetRequests, setPasswordResetRequests] = useState<PasswordResetRequest[]>([]);
  const [accountRequests, setAccountRequests] = useState<AccountRequest[]>([]);
  const [accountDeletionRequests, setAccountDeletionRequests] = useState<AccountDeletionRequest[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<Account[]>([]);
  const [userSearchQuery, setUserSearchQuery] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSearching, setIsSearching] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalResults, setTotalResults] = useState(0);
  const { toast } = useToast();
  const navigate = useNavigate();

  useEffect(() => {
    let isMounted = true;

    loadData(true);

    const intervalId = setInterval(() => {
      if (isMounted) loadData(false);
    }, 3000);

    return () => {
      isMounted = false;
      clearInterval(intervalId);
    };
  }, []);

  const loadData = async (showLoading = false) => {
    if (showLoading) setIsLoading(true);
    try {
      const [usersData, passwordRequestsData, accountRequestsData, accountDeletionRequestsData] = await Promise.all([
        api.getAllUsers(),
        api.getPasswordResetRequests(),
        api.getAccountRequests(),
        api.getAccountDeletionRequests(),
      ]);

      setUsers(usersData);
      setPasswordResetRequests(passwordRequestsData);
      setAccountRequests(accountRequestsData);
      setAccountDeletionRequests(accountDeletionRequestsData);
    } catch (error) {
      // Only show toast on initial load error
      if (showLoading) {
        toast({
          variant: 'destructive',
          title: 'Error',
          description: 'Failed to load admin data',
        });
      }
    } finally {
      if (showLoading) {
        setIsLoading(false);
      }
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

  const handleApproveAccountDeletion = async (requestId: string) => {
    try {
      await api.approveAccountDeletion(requestId);
      toast({
        title: 'Request Approved',
        description: 'Account deletion request has been approved and account deleted',
      });
      // Refresh requests
      const requests = await api.getAccountDeletionRequests();
      setAccountDeletionRequests(requests);
    } catch (error: any) {
      toast({
        variant: 'destructive',
        title: 'Error',
        description: error.message || 'Failed to approve account deletion request',
      });
    }
  };

  const handleRejectAccountDeletion = async (requestId: string) => {
    try {
      await api.rejectAccountDeletion(requestId);
      toast({
        title: 'Request Rejected',
        description: 'Account deletion request has been rejected',
      });
      // Refresh requests
      const requests = await api.getAccountDeletionRequests();
      setAccountDeletionRequests(requests);
    } catch (error: any) {
      toast({
        variant: 'destructive',
        title: 'Error',
        description: error.message || 'Failed to reject account deletion request',
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

  const handleUserSearch = async () => {
    setIsSearching(true);

    try {
      // If query is empty, searchUsers('') will return all users (handled by backend)
      const results = await api.searchUsers(userSearchQuery);
      setUsers(results);
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Search Failed',
        description: 'An error occurred while searching users',
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
          <Button
            variant={activeTab === 'account-requests' ? 'default' : 'ghost'}
            onClick={() => setActiveTab('account-requests')}
          >
            <Shield className="mr-2 h-4 w-4" />
            Account Requests
            {accountRequests.length > 0 && (
              <span className="ml-2 bg-blue-500 text-white text-xs px-2 py-0.5 rounded-full">
                {accountRequests.length}
              </span>
            )}
          </Button>
          <Button
            variant={activeTab === 'deletion-requests' ? 'default' : 'ghost'}
            onClick={() => setActiveTab('deletion-requests')}
          >
            <Trash2 className="mr-2 h-4 w-4" />
            Deletion Requests
            {accountDeletionRequests.length > 0 && (
              <span className="ml-2 bg-red-500 text-white text-xs px-2 py-0.5 rounded-full">
                {accountDeletionRequests.length}
              </span>
            )}
          </Button>
        </div>

        {/* User Management Tab */}
        {activeTab === 'users' && (
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle>User Management</CardTitle>
                  <CardDescription>Manage user roles and account status</CardDescription>
                </div>
                <Button onClick={() => navigate('/create-user')}>
                  <UserPlus className="mr-2 h-4 w-4" />
                  Create User
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <div className="mb-6">
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    handleUserSearch();
                  }}
                  className="flex gap-2"
                >
                  <Input
                    type="text"
                    placeholder="Search by username..."
                    value={userSearchQuery}
                    onChange={(e) => setUserSearchQuery(e.target.value)}
                    disabled={isSearching}
                    className="max-w-sm"
                  />
                  <Button type="submit" disabled={isSearching}>
                    {isSearching ? 'Searching...' : 'Search'}
                  </Button>
                  {userSearchQuery && (
                    <Button
                      type="button"
                      variant="ghost"
                      onClick={() => {
                        setUserSearchQuery('');
                        // Trigger search with empty query to reset
                        api.searchUsers('').then(setUsers);
                      }}
                      disabled={isSearching}
                    >
                      Clear
                    </Button>
                  )}
                </form>
              </div>

              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Name</TableHead>
                    <TableHead>Username</TableHead>
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
                      <TableCell>{user.username}</TableCell>
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
                          className={`text-xs px-2 py-1 rounded-full ${user.isActive
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

        {/* Account Requests Tab */}
        {activeTab === 'account-requests' && (
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

        {/* Account Deletion Requests Tab */}
        {activeTab === 'deletion-requests' && (
          <Card>
            <CardHeader>
              <CardTitle>Account Deletion Requests</CardTitle>
              <CardDescription>Review and approve pending account deletion requests</CardDescription>
            </CardHeader>
            <CardContent>
              {accountDeletionRequests.length === 0 ? (
                <div className="text-center py-12">
                  <Shield className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                  <p className="text-gray-600">No pending account deletion requests</p>
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Username</TableHead>
                      <TableHead>Account Number</TableHead>
                      <TableHead>Reason</TableHead>
                      <TableHead>Requested</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {accountDeletionRequests.map((request) => (
                      <TableRow key={request.id}>
                        <TableCell className="font-medium">{request.username}</TableCell>
                        <TableCell>{request.accountNumber}</TableCell>
                        <TableCell className="max-w-xs truncate" title={request.reason}>
                          {request.reason}
                        </TableCell>
                        <TableCell>{formatDate(request.requestedAt)}</TableCell>
                        <TableCell>
                          <span className="text-xs px-2 py-1 rounded-full bg-red-100 text-red-800">
                            {request.status}
                          </span>
                        </TableCell>
                        <TableCell>
                          <div className="flex gap-2">
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => handleApproveAccountDeletion(request.id)}
                            >
                              Approve
                            </Button>
                            <Button
                              variant="destructive"
                              size="sm"
                              onClick={() => handleRejectAccountDeletion(request.id)}
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
                              className={`text-xs px-2 py-1 rounded-full ${account.status === 'active'
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
    </DashboardLayout >
  );
}
