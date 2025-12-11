package org.waterwood.waterfunservicecore.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class UserJwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt source) {
        List<String> roles = source.getClaimAsStringList("roles");
        List<SimpleGrantedAuthority> authorities = (roles == null ? List.<String>of() : roles)
                .stream().map(r -> "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .toList();
        Long userId = Long.valueOf(source.getSubject());
        UserContext context = UserContext.of(userId, roles == null ? Set.of() : new HashSet<>(roles) );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(context, null, authorities);
        auth.setDetails(source);
        return auth;
    }
}
