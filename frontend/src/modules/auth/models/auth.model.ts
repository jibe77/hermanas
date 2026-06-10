export interface User {
    id: string;
    login: string;
    email: string;
    authState: string;
    roles?: string[];
    /** Preferred language ("fr" | "en"). Undefined for anonymous visitors. */
    language?: string;
}
