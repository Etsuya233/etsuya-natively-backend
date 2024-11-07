package com.ety.natively;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.people.v1.PeopleServiceScopes;
import com.google.auth.oauth2.GoogleAuthUtils;
import com.google.common.collect.Collections2;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

//@SpringBootTest
public class OAuthTest {

	String accessToken = "";

//	@Test
//	public void testGoogle() throws IOException {
//		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
//				new NetHttpTransport(),
//				GsonFactory.getDefaultInstance(),
//				clientId, clientSecret,
//				List.of(PeopleServiceScopes.USERINFO_PROFILE, PeopleServiceScopes.USERINFO_EMAIL)
//		).setAccessType("offline").build();
//		GoogleAuthorizationCodeRequestUrl requestUrl = flow.newAuthorizationUrl();
//		requestUrl.setRedirectUri("http://localhost:5173/oauth/google");
//		System.out.println(requestUrl);
//	}
//
//	@Test
//	public void testPic() throws Exception {
//		URL url = new URI("https://lh3.googleusercontent.com/a/ACg8ocKPzT-Jca2lC_3z5IL4R2G08vavVpGSFOY-mkoIAERidzXs54gE=s100").toURL();
//		URLConnection connection = url.openConnection();
//		System.out.println(url);
//		String contentType = connection.getContentType();
//		System.out.println(contentType);
//	}
}
