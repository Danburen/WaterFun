export interface User {
    email: string;
    username: string;
    avatar: string;
    user_id: string;
    role: UserRole;
    is_authenticated: boolean;
}

export type UserRole = 'USER' | 'ADMIN' | 'MANAGER' | 'OPERATOR' | 'TOURIST';