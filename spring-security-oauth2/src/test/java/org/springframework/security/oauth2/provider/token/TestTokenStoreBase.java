/*
 * Copyright 2006-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.security.oauth2.provider.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collection;
import java.util.Date;

import org.junit.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.common.DefaultExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.DefaultOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

/**
 * @author Dave Syer
 * 
 */
public abstract class TestTokenStoreBase {

	public abstract TokenStore getTokenStore();

	@Test
	public void testReadingAuthenticationForTokenThatDoesNotExist() {
		assertNull(getTokenStore().readAuthentication("tokenThatDoesNotExist"));
	}

	@Test
	public void testStoreAccessToken() {
		OAuth2Authentication expectedAuthentication = new OAuth2Authentication(new AuthorizationRequest("id", null), new TestAuthentication("test2", false));
		OAuth2AccessToken expectedOAuth2AccessToken = new DefaultOAuth2AccessToken("testToken");
		getTokenStore().storeAccessToken(expectedOAuth2AccessToken, expectedAuthentication);

		OAuth2AccessToken actualOAuth2AccessToken = getTokenStore().readAccessToken("testToken");
		assertEquals(expectedOAuth2AccessToken, actualOAuth2AccessToken);
		assertEquals(expectedAuthentication, getTokenStore().readAuthentication(expectedOAuth2AccessToken));
		getTokenStore().removeAccessToken(expectedOAuth2AccessToken);
		assertNull(getTokenStore().readAccessToken("testToken"));
		assertNull(getTokenStore().readAuthentication(expectedOAuth2AccessToken.getValue()));
	}

	@Test
	public void testRetrieveAccessToken() {
		OAuth2Authentication expectedAuthentication = new OAuth2Authentication(new AuthorizationRequest("id", null), new TestAuthentication("test2", false));
		OAuth2AccessToken expectedOAuth2AccessToken = new DefaultOAuth2AccessToken("testToken");
		getTokenStore().storeAccessToken(expectedOAuth2AccessToken, expectedAuthentication);

		OAuth2AccessToken actualOAuth2AccessToken = getTokenStore().getAccessToken(expectedAuthentication);
		assertEquals(expectedOAuth2AccessToken, actualOAuth2AccessToken);
		assertEquals(expectedAuthentication, getTokenStore().readAuthentication(expectedOAuth2AccessToken));
		getTokenStore().removeAccessToken(expectedOAuth2AccessToken);
		assertNull(getTokenStore().readAccessToken("testToken"));
		assertNull(getTokenStore().readAuthentication(expectedOAuth2AccessToken.getValue()));
		assertNull(getTokenStore().getAccessToken(expectedAuthentication));
	}

	@Test
	public void testFindAccessTokensByUserName() {
		OAuth2Authentication expectedAuthentication = new OAuth2Authentication(new AuthorizationRequest("id", null), new TestAuthentication("test2", false));
		OAuth2AccessToken expectedOAuth2AccessToken = new DefaultOAuth2AccessToken("testToken");
		getTokenStore().storeAccessToken(expectedOAuth2AccessToken, expectedAuthentication);

		Collection<OAuth2AccessToken> actualOAuth2AccessTokens = getTokenStore().findTokensByUserName("test2");
		assertEquals(1, actualOAuth2AccessTokens.size());
	}

	@Test
	public void testFindAccessTokensByClientId() {
		OAuth2Authentication expectedAuthentication = new OAuth2Authentication(new AuthorizationRequest("id", null), new TestAuthentication("test2", false));
		OAuth2AccessToken expectedOAuth2AccessToken = new DefaultOAuth2AccessToken("testToken");
		getTokenStore().storeAccessToken(expectedOAuth2AccessToken, expectedAuthentication);

		Collection<OAuth2AccessToken> actualOAuth2AccessTokens = getTokenStore().findTokensByClientId("id");
		assertEquals(1, actualOAuth2AccessTokens.size());
	}

	@Test
	public void testReadingAccessTokenForTokenThatDoesNotExist() {
		assertNull(getTokenStore().readAccessToken("tokenThatDoesNotExist"));
	}

	@Test
	public void testRefreshTokenIsNotStoredDuringAccessToken() {
		OAuth2Authentication expectedAuthentication = new OAuth2Authentication(new AuthorizationRequest("id", null), new TestAuthentication("test2", false));
		DefaultOAuth2AccessToken expectedOAuth2AccessToken = new DefaultOAuth2AccessToken("testToken");
		expectedOAuth2AccessToken.setRefreshToken(new DefaultOAuth2RefreshToken("refreshToken"));
		getTokenStore().storeAccessToken(expectedOAuth2AccessToken, expectedAuthentication);

		OAuth2AccessToken actualOAuth2AccessToken = getTokenStore().readAccessToken("testToken");
		assertNotNull(actualOAuth2AccessToken.getRefreshToken());
		
		assertNull(getTokenStore().readRefreshToken("refreshToken"));
	}

	@Test
	public void testStoreRefreshToken() {
		DefaultOAuth2RefreshToken expectedExpiringRefreshToken = new DefaultExpiringOAuth2RefreshToken("testToken",
				new Date());
		OAuth2Authentication expectedAuthentication = new OAuth2Authentication(new AuthorizationRequest("id", null), new TestAuthentication("test2", false));
		getTokenStore().storeRefreshToken(expectedExpiringRefreshToken, expectedAuthentication);

		OAuth2RefreshToken actualExpiringRefreshToken = getTokenStore().readRefreshToken("testToken");
		assertEquals(expectedExpiringRefreshToken, actualExpiringRefreshToken);
		assertEquals(expectedAuthentication, getTokenStore().readAuthenticationForRefreshToken(expectedExpiringRefreshToken));
		getTokenStore().removeRefreshToken(expectedExpiringRefreshToken);
		assertNull(getTokenStore().readRefreshToken("testToken"));
		assertNull(getTokenStore().readAuthentication(expectedExpiringRefreshToken.getValue()));
	}

	@Test
	public void testReadingRefreshTokenForTokenThatDoesNotExist() {
		getTokenStore().readRefreshToken("tokenThatDoesNotExist");
	}

	@Test
	public void testGetAccessTokenForDeletedUser() throws Exception {
		OAuth2Authentication expectedAuthentication = new OAuth2Authentication(new AuthorizationRequest("id", null), new TestAuthentication("test", false));
		OAuth2AccessToken expectedOAuth2AccessToken = new DefaultOAuth2AccessToken("testToken");
		getTokenStore().storeAccessToken(expectedOAuth2AccessToken, expectedAuthentication);
		assertEquals(expectedOAuth2AccessToken, getTokenStore().getAccessToken(expectedAuthentication));
		assertEquals(expectedAuthentication, getTokenStore().readAuthentication(expectedOAuth2AccessToken.getValue()));
		OAuth2Authentication anotherAuthentication = new OAuth2Authentication(new AuthorizationRequest("id", null), new TestAuthentication("test", true));
		assertEquals(expectedOAuth2AccessToken, getTokenStore().getAccessToken(anotherAuthentication));
		// The generated key for the authentication is the same as before, but the two auths are not equal. This could
		// happen if there are 2 users in a system with the same username, or (more likely), if a user account was
		// deleted and re-created.
		assertEquals(anotherAuthentication, getTokenStore().readAuthentication(expectedOAuth2AccessToken.getValue()));
	}

	@Test
	public void testRemoveRefreshToken() {
		OAuth2RefreshToken expectedExpiringRefreshToken = new DefaultExpiringOAuth2RefreshToken("testToken",
				new Date());
		OAuth2Authentication expectedAuthentication = new OAuth2Authentication(new AuthorizationRequest("id", null), new TestAuthentication("test2", false));
		getTokenStore().storeRefreshToken(expectedExpiringRefreshToken, expectedAuthentication);
		getTokenStore().removeRefreshToken(expectedExpiringRefreshToken);
		
		assertNull(getTokenStore().readRefreshToken("testToken"));
	}

	protected static class TestAuthentication extends AbstractAuthenticationToken {
		private String principal;

		public TestAuthentication(String name, boolean authenticated) {
			super(null);
			setAuthenticated(authenticated);
			this.principal = name;
		}

		public Object getCredentials() {
			return null;
		}

		public Object getPrincipal() {
			return this.principal;
		}
	}

}
