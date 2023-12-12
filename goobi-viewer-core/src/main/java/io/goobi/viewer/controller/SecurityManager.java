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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class SecurityManager {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(SecurityManager.class);

    private static final int ATTEMPTS_BEFORE_CAPTCHA = 5;

    /** Map containing failed login attempt counters for user names. */
    private final Map<String, Integer> failedLoginAttemptsUserNameMap = new HashMap<>();
    /** Map containing last failed login attempt time stamps for user names. */
    private final Map<String, Long> lastLoginAttemptUserNameMap = new HashMap<>();
    /** Map containing failed login attempt counters for IP addresses. */
    private final Map<String, Integer> failedLoginAttemptsIpAddressMap = new HashMap<>();
    /** Map containing last failed login attempt time stamps for IP addresses. */
    private final Map<String, Long> lastLoginAttemptIpAddressMap = new HashMap<>();

    /**
     * Resets all maps. Used for unit tests.
     */
    public void reset() {
        failedLoginAttemptsUserNameMap.clear();
        lastLoginAttemptUserNameMap.clear();
        failedLoginAttemptsIpAddressMap.clear();
        lastLoginAttemptIpAddressMap.clear();
    }

    /**
     * 
     * @param ipAddress IP address to check
     * @return true if captcha is appropriate; false otherwise
     */
    public boolean isRequireCaptcha(String ipAddress) {
        if (failedLoginAttemptsIpAddressMap.get(ipAddress) == null) {
            return false;
        }

        return failedLoginAttemptsIpAddressMap.get(ipAddress) > ATTEMPTS_BEFORE_CAPTCHA;
    }

    /**
     * 
     * @param userName User name / e-mail address to check
     * @return Current login delay for the given userName
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

        return getDelay(attempts, lastAttempt, System.currentTimeMillis());
    }

    /**
     * 
     * @param ipAddress IP address to check
     * @return Current login delay for the given ipAddress
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

        return getDelay(attempts, lastAttempt, System.currentTimeMillis());
    }

    /**
     * Calculates login delay given the number of failed attempts and the last attempt timestamp.
     * 
     * @param attempts Total number of failed attempts
     * @param lastAttempt Millis of the last attempt
     * @param now Current millis
     * @return Delay in millis
     * @should return zero if attempts zero
     * @should return zero if time between lastAttempt and now larger than delay
     * @should return delay if time between lastAttempt and now smaller than delay
     */
    static long getDelay(int attempts, long lastAttempt, long now) {
        long delay = attempts * 3 * 1000L;
        if (now - lastAttempt >= delay) {
            return 0;
        }

        return delay - (now - lastAttempt);
    }

    /**
     * Adds to the failed attempts counter for the given user name.
     * 
     * @param userName User name / e-mail address
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
     * Adds to the failed attempts counter for the given IP address.
     * 
     * @param ipAddress IP address
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
     * Removes failed login attempt history for given user name.
     * 
     * @param userName User name / e-mail address
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
     * @param ipAddress IP address
     */
    public void resetFailedLoginAttemptForIpAddress(String ipAddress) {
        if (ipAddress == null) {
            throw new IllegalArgumentException("ipAddress may not be null");
        }

        failedLoginAttemptsIpAddressMap.remove(ipAddress);
        lastLoginAttemptIpAddressMap.remove(ipAddress);
        logger.debug("Reset failed login attempts for IP address {}", ipAddress);
    }
}
