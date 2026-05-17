import type {LoginType} from "./types/api/auth";

export const REGEX = {
    username: /^[0-9a-zA-Z_]+$/,
    phone: /^1[3-9]\d{9}$/,
    email: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
}

export const AUTH_USERNAME_REGEX_MAP:Record<LoginType,RegExp> ={
    password: REGEX.username,
    sms: REGEX.phone,
    email: REGEX.email,
};