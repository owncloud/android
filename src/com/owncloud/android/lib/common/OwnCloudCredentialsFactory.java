package com.owncloud.android.lib.common;

public class OwnCloudCredentialsFactory {

	private static OwnCloudAnonymousCredentials sAnonymousCredentials;

	public static OwnCloudCredentials newBasicCredentials(String username, String password) {
		return new OwnCloudBasicCredentials(username, password);
	}
	
	public static OwnCloudCredentials newBearerCredentials(String authToken) {
        return new OwnCloudBearerCredentials(authToken);
	}
    
	public static OwnCloudCredentials newSamlSsoCredentials(String sessionCookie) {
		return new OwnCloudSamlSsoCredentials(sessionCookie);
	}

	public static final OwnCloudCredentials getAnonymousCredentials() {
		if (sAnonymousCredentials == null) {
			sAnonymousCredentials = new OwnCloudAnonymousCredentials();
		}
		return sAnonymousCredentials;
	}

	public static final class OwnCloudAnonymousCredentials implements OwnCloudCredentials {
		
		protected OwnCloudAnonymousCredentials() {
		}
		
		@Override
		public void applyTo(OwnCloudClient client) {
			client.getState().clearCredentials();
			client.getState().clearCookies();
		}

		@Override
		public String getAuthToken() {
			return "";
		}

		@Override
		public boolean authTokenExpires() {
			return false;
		}

		@Override
		public String getUsername() {
			// no user name
			return null;
		}
	}

}
