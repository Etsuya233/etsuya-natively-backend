package com.ety.natively;

import com.ety.natively.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

@SpringBootTest
public class JwtTest {

	@Autowired
	private JwtUtils jwtUtils;

	@Test
	public void testJwt(){
		String token = jwtUtils.createToken(Map.of("userId", Long.valueOf(1852303734730272770L), "version", 0), 157680000L);
		System.out.println(token);
	}

}
