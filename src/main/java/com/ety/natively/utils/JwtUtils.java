package com.ety.natively.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类(RSA256)
 * @author Etsuya
 */
@Slf4j
public class JwtUtils {

	private final PrivateKey privateKey;
	private final PublicKey publicKey;
	private JwtParser jwtParser = null;

	public JwtUtils(String privateKeyPath, String publicKeyPath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, URISyntaxException {
		privateKey = loadPrivateKey(privateKeyPath);
		publicKey = loadPublicKey(publicKeyPath);
		if(publicKey != null){
			jwtParser = Jwts.parser()
					.verifyWith(publicKey)
					.build();
		}
	}

	/**
	 * 创建token
	 * @param claims JWT的claims
	 * @param ttl JWT的有效期，单位为秒
	 * @return token
	 */
	public String createToken(Map<String, ?> claims, Long ttl){
		return Jwts.builder()
				.subject("dc")
				.claims(claims)
				.expiration(Date.from(Instant.now().plusSeconds(ttl)))
				.signWith(privateKey)
				.compact();
	}

	/**
	 * 解析token
	 * @param token token
	 * @return claims
	 */
	public Map<String, Object> parseToken(String token) {
		Jws<Claims> claimsJws = jwtParser.parse(token).accept(Jws.CLAIMS);
		return claimsJws.getPayload();
	}

	private PrivateKey loadPrivateKey(String privateKeyPath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, URISyntaxException {
		if(privateKeyPath == null) return null;
		final PrivateKey privateKey;
		URL resource = getClass().getClassLoader().getResource(privateKeyPath);
		assert resource != null;
		String key = new String(Files.readAllBytes(Path.of(resource.toURI())))
				.replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", "")
				.replaceAll("\\s", "");

		byte[] keyBytes = Base64.getDecoder().decode(key);
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		privateKey = kf.generatePrivate(spec);
		return privateKey;
	}

	private PublicKey loadPublicKey(String publicKeyPath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, URISyntaxException {
		if(publicKeyPath == null) return null;
		URL resource = getClass().getClassLoader().getResource(publicKeyPath);
		assert resource != null;
		String key = new String(Files.readAllBytes(Paths.get(resource.toURI())))
				.replace("-----BEGIN PUBLIC KEY-----", "")
				.replace("-----END PUBLIC KEY-----", "")
				.replaceAll("\\s", "");
		byte[] keyBytes = Base64.getDecoder().decode(key);
		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePublic(spec);
	}

}
