package com.owncloud.android.lib.common;

public class OwnCloudClientManagerFactory {
	
	public static enum Policy {
		ALWAYS_NEW_CLIENT,
		SINGLE_SESSION_PER_ACCOUNT
	}
	
	public final static Policy DEFAULT_POLICY = Policy.ALWAYS_NEW_CLIENT;

	public static OwnCloudClientManager newDefaultOwnCloudClientManager() {
		return newOwnCloudClientManager(DEFAULT_POLICY);
	}
	
	public static OwnCloudClientManager newOwnCloudClientManager(Policy policy) {
		switch (policy) {
			case ALWAYS_NEW_CLIENT:
				return new SimpleFactoryManager();
				
			case SINGLE_SESSION_PER_ACCOUNT:
				return new SingleSessionManager();
				
			default:
				throw new IllegalArgumentException("Unknown policy");
		}
	}
	
	
}
