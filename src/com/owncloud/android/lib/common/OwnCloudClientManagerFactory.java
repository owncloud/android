package com.owncloud.android.lib.common;

public class OwnCloudClientManagerFactory {
	
	public static enum Policy {
		ALWAYS_NEW_CLIENT,
		SINGLE_SESSION_PER_ACCOUNT
	}
	
	private static Policy sDefaultPolicy = Policy.ALWAYS_NEW_CLIENT;
	
	private static OwnCloudClientManager sDefaultSingleton;

	public static OwnCloudClientManager newDefaultOwnCloudClientManager() {
		return newOwnCloudClientManager(sDefaultPolicy);
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
	
    public static OwnCloudClientManager getDefaultSingleton() {
    	if (sDefaultSingleton == null) {
    		sDefaultSingleton = newDefaultOwnCloudClientManager();
    	}
    	return sDefaultSingleton;
    }
    
    public static Policy getDefaultPolicy() {
    	return sDefaultPolicy;
    }

    public static void setDefaultPolicy(Policy policy) {
    	if (policy == null) {
    		throw new IllegalArgumentException("Default policy cannot be NULL");
    	}
    	if (defaultSingletonMustBeUpdated(policy)) {
    		sDefaultSingleton = null;
    	}
    	sDefaultPolicy = policy;
    }
    
	private static boolean defaultSingletonMustBeUpdated(Policy policy) {
		if (sDefaultSingleton == null) {
			return false;
		}
		if (policy == Policy.ALWAYS_NEW_CLIENT && 
				!(sDefaultSingleton instanceof SimpleFactoryManager)) {
			return true;
		}
		if (policy == Policy.SINGLE_SESSION_PER_ACCOUNT && 
				!(sDefaultSingleton instanceof SingleSessionManager)) {
			return true;
		}
		return false;
	}

}
