import { useState, FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useToast } from '@/hooks/use-toast';
import { Lock } from 'lucide-react';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({});

  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const { toast } = useToast();

  // Redirect if already authenticated
  if (isAuthenticated) {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    if (user.role === 'customer') navigate('/customer');
    else if (user.role === 'teller') navigate('/teller');
    else if (user.role === 'admin') navigate('/admin');
  }

  const validateForm = (): boolean => {
    const newErrors: { email?: string; password?: string } = {};

    if (!email) {
      newErrors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(email)) {
      newErrors.email = 'Email is invalid';
    }

    if (!password) {
      newErrors.password = 'Password is required';
    } else if (password.length < 6) {
      newErrors.password = 'Password must be at least 6 characters';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setIsLoading(true);
    setErrors({});

    try {
      await login(email, password);

      toast({
        title: 'Login successful',
        description: 'Welcome back!',
      });

      // Get user from localStorage to determine role
      const user = JSON.parse(localStorage.getItem('user') || '{}');

      // Redirect based on role
      if (user.role === 'customer') {
        navigate('/customer');
      } else if (user.role === 'teller') {
        navigate('/teller');
      } else if (user.role === 'admin') {
        navigate('/admin');
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'An error occurred';

      toast({
        variant: 'destructive',
        title: 'Login failed',
        description: errorMessage,
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-50 to-secondary-50 p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-1 text-center">
          <div className="flex justify-center mb-4">
            <div className="h-12 w-12 rounded-full bg-primary/10 flex items-center justify-center">
              <Lock className="h-6 w-6 text-primary" />
            </div>
          </div>
          <CardTitle className="text-2xl font-bold">MyBankUML</CardTitle>
          <CardDescription>Enter your credentials to access your account</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                placeholder="name@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                disabled={isLoading}
                className={errors.email ? 'border-destructive' : ''}
              />
              {errors.email && (
                <p className="text-sm text-destructive">{errors.email}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={isLoading}
                className={errors.password ? 'border-destructive' : ''}
              />
              {errors.password && (
                <p className="text-sm text-destructive">{errors.password}</p>
              )}
            </div>

            <Button type="submit" className="w-full" disabled={isLoading}>
              {isLoading ? 'Signing in...' : 'Sign In'}
            </Button>

            <div className="text-center">
              <Link
                to="/forgot-password"
                className="text-sm text-primary hover:underline"
              >
                Forgot your password?
              </Link>
            </div>
          </form>

          <div className="mt-6 p-4 bg-muted rounded-md">
            <p className="text-xs text-muted-foreground font-semibold mb-2">Demo Credentials:</p>
            <div className="text-xs text-muted-foreground space-y-1">
              <p>Customer: customer@example.com / password123</p>
              <p>Teller: teller@example.com / password123</p>
              <p>Admin: admin@example.com / password123</p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
