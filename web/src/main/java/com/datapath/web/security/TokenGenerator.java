package com.datapath.web.security;

import com.datapath.web.dto.ApplicationUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Date;

import static com.datapath.web.security.SecurityConstants.*;

public class TokenGenerator {

    private TokenGenerator() {
    }

    public static String generate(ApplicationUser applicationUser) {
        Claims claims = Jwts.claims().setSubject(applicationUser.getEmail());
        claims.put(CREDENTIALS, applicationUser);

        GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(applicationUser.getRole());
        claims.put(AUTHORITY_KEY, grantedAuthority.getAuthority());

        return Jwts.builder()
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .setClaims(claims)
                .compact();
    }
}
