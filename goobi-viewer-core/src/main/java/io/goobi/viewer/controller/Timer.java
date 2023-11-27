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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * <p>
 * Timer class.
 * @deprecated use {@link de.intranda.monitoring.timer.Timer} instead
 * </p>
 */
@Deprecated(since="23.11")
public class Timer {

    private static final Logger logger = LogManager.getLogger(Timer.class);
    private static final NumberFormat format = new DecimalFormat("0.00");
    private static final Timer instance = new Timer();

    private Long startDate = null;
    private Long measuredTime = null;
    private Map<String, Long> startTimeMap = new HashMap<>();
    private Map<String, Long> measuredTimeMap = new HashMap<>();

    private Timer() {
    }

    /**
     * <p>
     * Getter for the field <code>instance</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.controller.Timer} object.
     */
    public static Timer getInstance() {
        return instance;
    }

    /**
     * <p>
     * startTiming.
     * </p>
     *
     * @param key a {@link java.lang.String} object.
     */
    public void startTiming(String key) {
        startTimeMap.put(key, System.nanoTime());
    }

    /**
     * <p>
     * startTiming.
     * </p>
     */
    public void startTiming() {
        startDate = System.nanoTime();
    }

    /**
     * <p>
     * pauseTiming.
     * </p>
     *
     * @param key a {@link java.lang.String} object.
     */
    public void pauseTiming(String key) {
        measure(key, true);
    }

    /**
     * <p>
     * ignoreLastMeasurement.
     * </p>
     *
     * @param key a {@link java.lang.String} object.
     */
    public void ignoreLastMeasurement(String key) {
        startTimeMap.remove(key);
    }

    /**
     * <p>
     * stopTiming.
     * </p>
     *
     * @param key a {@link java.lang.String} object.
     */
    public void stopTiming(String key) {
        try {
            measure(key, false);
        } catch (TimerException e) {

        }
        startTimeMap.remove(key);
    }

    /**
     * <p>
     * stopTiming.
     * </p>
     */
    public void stopTiming() {
        measure();
        startDate = null;
    }

    private void measure(String key, boolean additive) {
        Long startTime = startTimeMap.get(key);
        if (startTime == null) {
            throw new TimerException("Timer with key " + key + " has not been started");
        }
        Long time = System.nanoTime() - startTime;
        Long previousTime = measuredTimeMap.get(key);
        if (additive && previousTime != null) {
            time += previousTime;
        }
        measuredTimeMap.put(key, time);
    }

    /**
     * <p>
     * measure.
     * </p>
     *
     * @throws io.goobi.viewer.controller.Timer$TimerException if any.
     * @throws io.goobi.viewer.controller.Timer.TimerException if any.
     */
    public void measure() throws TimerException {
        if (startDate == null) {
            throw new TimerException("Timer has not been started");
        }
        measuredTime = System.nanoTime() - startDate;
    }

    /**
     * <p>
     * debug.
     * </p>
     *
     * @param message a {@link java.lang.String} object.
     * @param key a {@link java.lang.String} object.
     * @param scale a {@link io.goobi.viewer.controller.Timer.TimeScale} object.
     */
    public void debug(String message, String key, TimeScale scale) {
        String timeString = getTime(key, scale);
        logger.debug(message + timeString);
    }

    /**
     * <p>
     * getTime.
     * </p>
     *
     * @param key a {@link java.lang.String} object.
     * @param scale a {@link io.goobi.viewer.controller.Timer.TimeScale} object.
     * @return a {@link java.lang.String} object.
     */
    public String getTime(String key, TimeScale scale) {
        Long time;
        if (key != null) {
            time = measuredTimeMap.get(key);
        } else {
            time = measuredTime;
        }
        String timeString;
        switch (scale) {
            case NANOS:
                timeString = nanos(time);
                break;
            case MIKROS:
                timeString = mikros(time);
                break;
            case MILLIS:
                timeString = millis(time);
                break;
            case SECONDS:
            default:
                timeString = seconds(time);
        }
        return timeString;
    }

    /**
     * <p>
     * debug.
     * </p>
     *
     * @param message a {@link java.lang.String} object.
     * @param key a {@link java.lang.String} object.
     */
    public void debug(String message, String key) {
        debug(message, key, TimeScale.SECONDS);
    }

    /**
     * <p>
     * debug.
     * </p>
     */
    public void debug() {
        debug("Logged time: ");
    }

    /**
     * <p>
     * debug.
     * </p>
     *
     * @param message a {@link java.lang.String} object.
     */
    public void debug(String message) {
        debug(message, null);
    }

    /**
     * <p>
     * seconds.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String seconds() {
        return seconds(measuredTime);
    }

    /**
     * <p>
     * seconds.
     * </p>
     *
     * @param time a {@link java.lang.Long} object.
     * @return a {@link java.lang.String} object.
     */
    public String seconds(Long time) {
        if (time == null) {
            throw new TimerException("No measure has yet been taken");
        }
        double seconds = time / 1000.0 / 1000.0 / 1000.0;
        return format.format(seconds) + "s";
    }

    /**
     * <p>
     * millis.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String millis() {
        return millis(measuredTime);
    }

    /**
     * <p>
     * millis.
     * </p>
     *
     * @param time a {@link java.lang.Long} object.
     * @return a {@link java.lang.String} object.
     */
    public String millis(Long time) {
        if (time == null) {
            throw new TimerException("No mease has yet been taken");
        }
        double millis = time / 1000.0 / 1000.0;
        return format.format(millis) + "ms";
    }

    /**
     * <p>
     * mikros.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String mikros() {
        return mikros(measuredTime);
    }

    /**
     * <p>
     * mikros.
     * </p>
     *
     * @param time a {@link java.lang.Long} object.
     * @return a {@link java.lang.String} object.
     */
    public String mikros(Long time) {
        if (time == null) {
            throw new TimerException("No mease has yet been taken");
        }
        double mikros = time / 1000.0;
        return format.format(mikros) + "\u00B5s";
    }

    /**
     * <p>
     * nanos.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String nanos() {
        return nanos(measuredTime);
    }

    /**
     * <p>
     * nanos.
     * </p>
     *
     * @param time a {@link java.lang.Long} object.
     * @return a {@link java.lang.String} object.
     */
    public String nanos(Long time) {
        if (time == null) {
            throw new TimerException("No mease has yet been taken");
        }
        return time + "ns";
    }

    public static class TimerException extends RuntimeException {

        private static final long serialVersionUID = 5063112970533214125L;

        public TimerException() {
            super();
        }

        public TimerException(String message) {
            super(message);
        }

        public TimerException(Throwable cause) {
            super(cause);
        }
    }

    public enum TimeScale {
        SECONDS,
        MILLIS,
        MIKROS,
        NANOS;
    }

    /**
     * <p>
     * main.
     * </p>
     *
     * @param args an array of {@link java.lang.String} objects.
     * @throws java.lang.InterruptedException if any.
     */
    public static void main(String[] args) throws InterruptedException {

        for (int i = 0; i < 5; i++) {
            Timer.getInstance().startTiming("test");
            Thread.sleep(200);
            if (i == 2) {
                Timer.getInstance().ignoreLastMeasurement("test");
            } else {
                Timer.getInstance().pauseTiming("test");
            }
            Thread.sleep(300);
        }
        Timer.getInstance().debug("time measured ", "test");

    }

}
