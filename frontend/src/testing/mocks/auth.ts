import { User } from '@modules/auth/models';
export { User };

export class MockUser implements User {
    id = 'TEST_ID';
    login = 'TEST_LOGIN';
    email = 'TEST_EMAIL';
    authState = 'TEST_AUTH_STATE';
}
