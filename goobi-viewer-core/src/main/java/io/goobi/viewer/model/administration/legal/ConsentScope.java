package io.goobi.viewer.model.administration.legal;

import java.io.Serializable;

import javax.persistence.Entity;

import org.apache.commons.lang3.StringUtils;

/**
 * Describes the scope within which a declaration of consent (i.e. clicking the 'accept' button) is valid.
 * Outside of that scope, consent must be requested again.
 * The scope consists of a duration in days during which it is valid, as well as a storage type which can
 * either be 'local' or 'session'. This type determines if a consent is stored in the local or session storage of a browser.
 * If it is stored in session storage, the time to live of that scope is determined by the duration of the session.
 * 
 * @author florian
 *
 */
public class ConsentScope implements Serializable{

    private static final long serialVersionUID = -7933737886888841025L;

    private StorageMode storageMode = StorageMode.LOCAL;
    
    /**
     * The number of days after which the consent must be renewed at the latest
     */
    private int daysToLive = 14;
    
    //empty default constructor
    public ConsentScope() {
        
    }
    
    public ConsentScope(String string) {
        if("session".equalsIgnoreCase(string)) {
            this.storageMode = StorageMode.SESSION;
        } else if(StringUtils.isNotBlank(string) && string.matches("\\d+d")) {
            this.storageMode = StorageMode.LOCAL;
            this.daysToLive = Integer.parseInt(string.substring(0, string.length()-1));
        } else {
            throw new IllegalArgumentException("String '" + string + "' is not a valid consent scope string");
        }
    }
    
    public String toString() {
        if(StorageMode.SESSION.equals(this.storageMode)) {
            return "session";
        } else {
            return Integer.toString(daysToLive) + "d";
        }
    }
    
    /**
     * @return the stoargeMode
     */
    public StorageMode getStorageMode() {
        return storageMode;
    }



    /**
     * @param stoargeMode the stoargeMode to set
     */
    public void setStorageMode(StorageMode stoargeMode) {
        this.storageMode = stoargeMode;
    }



    /**
     * @return the daysToLive
     */
    public int getDaysToLive() {
        return daysToLive;
    }



    /**
     * @param daysToLive the daysToLive to set
     */
    public void setDaysToLive(int daysToLive) {
        this.daysToLive = daysToLive;
    }



    public static enum StorageMode {
        /**
         * Consent is valid for a single browser and stored it its local storage
         */
        LOCAL,
        /**
         * Consent is valid for a single browser session and stored in the browser's session storage
         */
        SESSION;
    }
    
    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj != null && obj.getClass().equals(this.getClass())) {
            return ((ConsentScope)obj).toString().equals(this.toString());
        } else {
            return false;
        }
    }
    
}
