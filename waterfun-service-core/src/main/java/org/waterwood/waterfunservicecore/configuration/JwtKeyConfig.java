package org.waterwood.waterfunservicecore.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class JwtKeyConfig {
    @Value("${jwt.private-key}")
    private Resource privateKeyContent;
    @Value("${jwt.public-key}")
    private Resource publicKeyContent;

    @Bean
    public PrivateKey getSigningKey() throws Exception {
        String privateKey = extraKeyContent(privateKeyContent);
        byte[] keyBytes = Base64.getDecoder().decode(privateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    @Bean
    public PublicKey getVerificationKey() throws Exception {
        String publicKey = extraKeyContent(publicKeyContent);
        byte[] keyBytes = Base64.getDecoder().decode(publicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    private String extraKeyContent(Resource originalPemKeyContent) throws IOException {
        String filename = originalPemKeyContent.getFilename();
        String content = new String(originalPemKeyContent.getInputStream().readAllBytes());
        if(content.startsWith("-----BEGIN")){
            return content.replaceAll("-----BEGIN (.*)-----", "")
                    .replaceAll("-----END (.*)-----", "")
                    .replaceAll("\\s+", "");
        }
        return content;
    }
}
