package com.datapath.web.security;

public class SecurityConstants {
    public static final String SECRET = "SecretKeyToGenJWTs";
    public static final long EXPIRATION_TIME = 864_000_000; // 10 days
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String AUTHORITY_KEY = "role";
    public static final String CREDENTIALS = "credentials";
    public static final String HEADER_STRING = "Authorization";
    public static final String SIGN_UP_URL = "/api/v0.1/monitoring/users";
}
