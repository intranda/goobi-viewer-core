/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.intranda.digiverso.presentation.controller;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Timer {

    private static final Logger logger = LoggerFactory.getLogger(Timer.class);
    private static final NumberFormat format = new DecimalFormat("0.00");
    private static final Timer instance = new Timer();

    private Long startDate = null;
    private Long measuredTime = null;
    private Map<String, Long> startTimeMap = new HashMap<>();
    private Map<String, Long> measuredTimeMap = new HashMap<>();

    private Timer() {
    }

    public static Timer getInstance() {
        return instance;
    }

    public void startTiming(String key) {
        startTimeMap.put(key, System.nanoTime());
    }

    public void startTiming() {
        startDate = System.nanoTime();
    }

    public void pauseTiming(String key) {
        measure(key, true);
    }

    public void ignoreLastMeasurement(String key) {
        startTimeMap.remove(key);
    }

    public void stopTiming(String key) {
        try {
            measure(key, false);
        } catch (TimerException e) {

        }
        startTimeMap.remove(key);
    }

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

    public void measure() throws TimerException {
        if (startDate == null) {
            throw new TimerException("Timer has not been started");
        }
        measuredTime = System.nanoTime() - startDate;
    }

    public void debug(String message, String key, TimeScale scale) {
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
        logger.debug(message + timeString);
    }

    public void debug(String message, String key) {
        debug(message, key, TimeScale.SECONDS);
    }

    public void debug() {
        debug("Logged time: ");
    }

    public void debug(String message) {
        debug(message, null);
    }

    public String seconds() {
        return seconds(measuredTime);
    }

    public String seconds(Long time) {
        if (time == null) {
            throw new TimerException("No measure has yet been taken");
        }
        double seconds = time / 1000.0 / 1000.0 / 1000.0;
        return format.format(seconds) + "s";
    }

    public String millis() {
        return millis(measuredTime);
    }

    public String millis(Long time) {
        if (time == null) {
            throw new TimerException("No mease has yet been taken");
        }
        double millis = time / 1000.0 / 1000.0;
        return format.format(millis) + "ms";
    }

    public String mikros() {
        return mikros(measuredTime);
    }

    public String mikros(Long time) {
        if (time == null) {
            throw new TimerException("No mease has yet been taken");
        }
        double mikros = time / 1000.0;
        return format.format(mikros) + "\u00B5s";
    }

    public String nanos() {
        return nanos(measuredTime);
    }

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
