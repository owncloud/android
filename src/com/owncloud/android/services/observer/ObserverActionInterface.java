package com.owncloud.android.services.observer;

public interface ObserverActionInterface {
    
    /**
     * Called by FolderObserver whenever fileName changed.
     * @param fileName which changed
     * @param path 
     */
    public void onFileChanged(String path, String fileName);
}
