/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityManager {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(SecurityManager.class);

    private static final int ATTEMPTS_BEFORE_CAPTCHA = 5;

    private final Map<String, Integer> failedLoginAttemptsUserNameMap = new HashMap<>();
    private final Map<String, Long> lastLoginAttemptUserNameMap = new HashMap<>();
    private final Map<String, Integer> failedLoginAttemptsIpAddressMap = new HashMap<>();
    private final Map<String, Long> lastLoginAttemptIpAddressMap = new HashMap<>();

    /**
     * 
     * @param ipAddress
     * @return
     */
    public boolean isRequireCaptcha(String ipAddress) {
        if (failedLoginAttemptsIpAddressMap.get(ipAddress) == null) {
            return false;
        }

        return failedLoginAttemptsIpAddressMap.get(ipAddress) > ATTEMPTS_BEFORE_CAPTCHA;
    }

    /**
     * 
     * @param userName
     * @return
     */
    public long getDelayForUserName(String userName) {
        if (userName == null) {
            throw new IllegalArgumentException("userName may not be null");
        }

        if (!failedLoginAttemptsUserNameMap.containsKey(userName)) {
            return 0;
        }
        int attempts = failedLoginAttemptsUserNameMap.get(userName);
        long lastAttempt = lastLoginAttemptUserNameMap.get(userName);

        return getDelay(attempts, lastAttempt);
    }

    /**
     * 
     * @param ipAddress
     * @return
     */
    public long getDelayForIpAddress(String ipAddress) {
        if (ipAddress == null) {
            throw new IllegalArgumentException("ipAddress may not be null");
        }

        if (!failedLoginAttemptsIpAddressMap.containsKey(ipAddress)) {
            return 0;
        }
        int attempts = failedLoginAttemptsIpAddressMap.get(ipAddress);
        long lastAttempt = lastLoginAttemptIpAddressMap.get(ipAddress);

        return getDelay(attempts, lastAttempt);
    }

    /**
     * 
     * @param attempts
     * @param lastAttempt
     * @return
     */
    long getDelay(int attempts, long lastAttempt) {
        long delay = attempts * 2 * 1000; // TODO delay progression
        long now = System.currentTimeMillis();
        if (now - lastAttempt >= delay) {
            return 0;
        }

        return delay - (now - lastAttempt);
    }

    /**
     * 
     * @param userName
     */
    public void addFailedLoginAttemptForUserName(String userName) {
        if (userName == null) {
            throw new IllegalArgumentException("userName may not be null");
        }

        Integer count = failedLoginAttemptsUserNameMap.get(userName);
        if (count == null) {
            count = 0;
        }
        failedLoginAttemptsUserNameMap.put(userName, ++count);
        lastLoginAttemptUserNameMap.put(userName, System.currentTimeMillis());
        logger.debug("Failed login attempt for user name '{}', count now {}", NetTools.scrambleEmailAddress(userName), count);
    }

    /**
     * 
     * @param ipAddress
     */
    public void addFailedLoginAttemptForIpAddress(String ipAddress) {
        if (ipAddress == null) {
            throw new IllegalArgumentException("ipAddress may not be null");
        }

        Integer count = failedLoginAttemptsIpAddressMap.get(ipAddress);
        if (count == null) {
            count = 0;
        }
        failedLoginAttemptsIpAddressMap.put(ipAddress, ++count);
        lastLoginAttemptIpAddressMap.put(ipAddress, System.currentTimeMillis());
        logger.debug("Failed login attempt for IP address {}, count now {}", ipAddress, count);
    }

    /**
     * Removes failed login attempt history for given user.
     * 
     * @param user
     */
    public void resetFailedLoginAttemptForUserName(String userName) {
        if (userName == null) {
            throw new IllegalArgumentException("userName may not be null");
        }

        failedLoginAttemptsUserNameMap.remove(userName);
        lastLoginAttemptUserNameMap.remove(userName);
        logger.debug("Reset failed login attempts for user name '{}'", NetTools.scrambleEmailAddress(userName));
    }

    /**
     * Removes failed login attempt history for given IP address.
     * 
     * @param ipAddress
     */
    public void resetFailedLoginAttemptForIpAddress(String ipAddress) {
        if (ipAddress == null) {
            throw new IllegalArgumentException("ipAddress may not be null");
        }

        failedLoginAttemptsIpAddressMap.remove(ipAddress);
        lastLoginAttemptIpAddressMap.remove(ipAddress);
        logger.debug("Reset failed login attempts for IP address {}", ipAddress);
    }

    /**
     * @return the failedLoginAttemptsUserNameMap
     */
    public Map<String, Integer> getFailedLoginAttemptsUserNameMap() {
        return failedLoginAttemptsUserNameMap;
    }

    /**
     * @return the failedLoginAttemptsIpAddressMap
     */
    public Map<String, Integer> getFailedLoginAttemptsIpAddressMap() {
        return failedLoginAttemptsIpAddressMap;
    }
}
