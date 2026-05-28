export interface User {
    id: string;
    login: string;
    email: string;
    authState: string;
    roles?: string[];
}
