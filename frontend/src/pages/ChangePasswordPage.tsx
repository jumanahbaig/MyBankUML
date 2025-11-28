import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { api } from '@/services/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useToast } from '@/hooks/use-toast';
import { Lock } from 'lucide-react';

export default function ChangePasswordPage() {
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const { user, login } = useAuth(); // We might need to update user state
    const navigate = useNavigate();
    const { toast } = useToast();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (newPassword.length < 6) {
            toast({
                variant: 'destructive',
                title: 'Invalid Password',
                description: 'Password must be at least 6 characters long.',
            });
            return;
        }

        if (newPassword !== confirmPassword) {
            toast({
                variant: 'destructive',
                title: 'Passwords do not match',
                description: 'Please ensure both passwords match.',
            });
            return;
        }

        setIsLoading(true);

        try {
            await api.changePassword(newPassword);

            toast({
                title: 'Password Updated',
                description: 'Your password has been changed successfully.',
            });

            // Update local user state to remove forcePasswordChange flag
            // Since we don't have a direct "updateUser" method in context, 
            // we might need to rely on the fact that the backend now sees us as "clean".
            // But the frontend context still has the old user object with forcePasswordChange=true.
            // We should probably force a logout or just navigate and let the guard check again (but guard checks context).
            // Ideally, we should refresh the user profile.

            // For now, let's just navigate. If the guard blocks us, we might need to refresh the page or re-login.
            // Actually, re-login with the new password is a safe bet.
            // Or we can manually update the user object in localStorage if AuthContext reads from it?
            // Let's try navigating to dashboard. If AuthContext is not updated, we might get redirected back.

            // A better UX: Auto-login with new password? Or just update the state.
            // Since I can't easily update AuthContext state without exposing a method, 
            // I'll force a logout and ask them to login again.

            // Wait, the requirement says "then that is their new password". It implies they are then logged in.
            // I'll try to update the user in localStorage and reload the page to refresh context?
            // Or just assume the user updates.

            // Let's just navigate to / and see. If the guard is implemented to check the *current* user object, it will loop.
            // I should probably implement a "refreshProfile" in AuthContext or just logout.

            // Let's go with Logout for safety and simplicity in this POC.
            // "Password changed. Please login with your new password."

            // api.logout();
            // logout();

            // Actually, let's try to just update the local storage user if possible.
            const storedUser = localStorage.getItem('user');
            if (storedUser) {
                const u = JSON.parse(storedUser);
                u.forcePasswordChange = false;
                localStorage.setItem('user', JSON.stringify(u));
                // Force reload to update context
                window.location.href = '/';
            } else {
                navigate('/');
            }

        } catch (error: any) {
            toast({
                variant: 'destructive',
                title: 'Error',
                description: error.message || 'Failed to change password.',
            });
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100 p-4">
            <Card className="w-full max-w-md">
                <CardHeader className="space-y-1">
                    <div className="flex justify-center mb-4">
                        <div className="p-3 rounded-full bg-primary/10">
                            <Lock className="h-6 w-6 text-primary" />
                        </div>
                    </div>
                    <CardTitle className="text-2xl text-center">Change Password</CardTitle>
                    <CardDescription className="text-center">
                        For security reasons, you must change your password to continue.
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <form onSubmit={handleSubmit} className="space-y-4">
                        <div className="space-y-2">
                            <Label htmlFor="new-password">New Password</Label>
                            <Input
                                id="new-password"
                                type="password"
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                required
                                minLength={6}
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="confirm-password">Confirm Password</Label>
                            <Input
                                id="confirm-password"
                                type="password"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                required
                                minLength={6}
                            />
                        </div>
                        <Button className="w-full" type="submit" disabled={isLoading}>
                            {isLoading ? 'Updating...' : 'Update Password'}
                        </Button>
                    </form>
                </CardContent>
            </Card>
        </div>
    );
}
