import { AuthService } from './auth.service';
import { LoginService } from './login.service';

// UserService is providedIn: 'root' (single global instance — required so APP_INITIALIZER
// and lazy-loaded modules share the same auth state). It must not appear in this list.
export const services = [AuthService, LoginService];

export * from './auth.service';
export * from './login.service';
export * from './user.service';
