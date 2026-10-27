# contexts

React Context providers for global state management in the Municipality Tax Management System.

<!-- AUTO-GENERATED: Purpose -->
## Purpose

The `contexts` directory contains React Context providers that manage global application state. These contexts provide authentication state, user profile data, and toast notifications accessible throughout the component tree without prop drilling. Contexts follow React best practices with proper TypeScript typing and custom hooks for consumption.
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Key Files -->
## Key Files

| File | Description |
|------|-------------|
| `AuthContext.tsx` | Authentication state and user session management |
| `ProfileContext.tsx` | User profile data and preferences |
| `ToastContext.tsx` | Global toast notification system |
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Public API -->
## Public API

### `AuthContext.tsx` - Authentication State

Provides authentication state and methods:

```typescript
import { useAuth } from '@/contexts/AuthContext';

function MyComponent() {
  const { user, isAuthenticated, login, logout, isLoading } = useAuth();
  
  if (!isAuthenticated) {
    return <LoginForm onSubmit={login} />;
  }
  
  return <div>Welcome, {user.name}!</div>;
}
```

**Context Value:**
```typescript
interface AuthContextValue {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: Credentials) => Promise<void>;
  logout: () => void;
  refreshToken: () => Promise<void>;
}
```

### `ProfileContext.tsx` - User Profile

Manages user profile and preferences:

```typescript
import { useProfile } from '@/contexts/ProfileContext';

function ProfileSettings() {
  const { profile, updateProfile, isLoading } = useProfile();
  
  const handleSave = async (data) => {
    await updateProfile(data);
  };
  
  return <ProfileForm profile={profile} onSave={handleSave} />;
}
```

**Context Value:**
```typescript
interface ProfileContextValue {
  profile: UserProfile | null;
  isLoading: boolean;
  updateProfile: (data: Partial<UserProfile>) => Promise<void>;
  refreshProfile: () => Promise<void>;
}
```

### `ToastContext.tsx` - Toast Notifications

Global toast notification system:

```typescript
import { useToast } from '@/contexts/ToastContext';

function SaveButton() {
  const { showToast } = useToast();
  
  const handleSave = async () => {
    try {
      await saveData();
      showToast('Saved successfully!', 'success');
    } catch (error) {
      showToast('Failed to save', 'error');
    }
  };
  
  return <button onClick={handleSave}>Save</button>;
}
```

**Context Value:**
```typescript
interface ToastContextValue {
  showToast: (message: string, type: 'success' | 'error' | 'warning' | 'info') => void;
  hideToast: (id: string) => void;
}
```
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Dependencies -->
## Dependencies

### Internal
- `../src/services/api.ts` - For API calls
- `../src/services/sessionService.ts` - For session management
- `../src/types/` - Type definitions

### External
- `react` - Context API
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Used By -->
## Used By

- All components that need authentication state
- All components that need to show notifications
- Profile-related components
<!-- END AUTO-GENERATED -->

<!-- CUSTOM SECTION: Project-Specific Notes -->
## Notes

Contexts are provided at the root level in `App.tsx` or `index.tsx`.

<!-- END CUSTOM SECTION -->
