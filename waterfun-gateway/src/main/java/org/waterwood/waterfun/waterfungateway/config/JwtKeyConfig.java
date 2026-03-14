package org.waterwood.waterfun.waterfungateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class JwtKeyConfig {
    @Value("${jwt.public-key}")
    private Resource publicKeyContent;
    @Bean
    public PublicKey getPublicKey() throws Exception {
        String publicKey = extraKeyContent(publicKeyContent);
        byte[] keyBytes = Base64.getDecoder().decode(publicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    private String extraKeyContent(Resource originalPemKeyContent) throws IOException {
        String content = new String(originalPemKeyContent.getInputStream().readAllBytes());
        if(content.startsWith("-----BEGIN")){
            return content.replaceAll("-----BEGIN (.*)-----", "")
                    .replaceAll("-----END (.*)-----", "")
                    .replaceAll("\\s+", "");
        }
        return content;
    }
}
