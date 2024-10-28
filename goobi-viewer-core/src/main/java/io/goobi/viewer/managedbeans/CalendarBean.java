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
package io.goobi.viewer.managedbeans;

import java.io.IOException;
import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.LongStream;

import javax.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;

import com.ibm.icu.text.RuleBasedNumberFormat;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.calendar.CalendarItemCentury;
import io.goobi.viewer.model.calendar.CalendarItemDay;
import io.goobi.viewer.model.calendar.CalendarItemMonth;
import io.goobi.viewer.model.calendar.CalendarItemWeek;
import io.goobi.viewer.model.calendar.CalendarItemYear;
import io.goobi.viewer.model.calendar.CalendarRow;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.solr.SolrConstants;

/**
 * This bean provides data for the calendar and time based search entries.
 */
@Named
@SessionScoped
public class CalendarBean implements Serializable {

    private static final long serialVersionUID = 1095535586988646463L;

    private static final int MAX_ALLOWED_YEAR = LocalDateTime.now().getYear() + 1000;
    private static final int MIN_ALLOWED_YEAR = -10_000;

    private static final Logger logger = LogManager.getLogger(CalendarBean.class);

    @Inject
    private SearchBean searchBean;

    //    private List<CalendarItemYear> allActiveYears;
    private Map<Integer, CalendarItemCentury> allActiveCenturies;
    private CalendarItemYear currentYear;
    private CalendarItemMonth currentMonth;
    private CalendarItemDay currentDay;

    private int yearStart = 1750;
    private int yearEnd = 3000;
    private String collection;

    private int rowIndex;

    private CalendarRow monthRow = new CalendarRow();
    private CalendarRow dayRow = new CalendarRow();

    private String docstructFilterQuery = "";
    private String selectYear;

    private List<CalendarItemMonth> monthList;

    /**
     * <p>
     * Constructor for CalendarBean.
     * </p>
     */
    public CalendarBean() {
        // the emptiness inside
    }

    @PostConstruct
    private void init() {
        // PostConstruct methods may not throw exceptions
        logger.trace("init");
        try {
            getDefaultDates();
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here");
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here");
        }
    }

    /**
     * Required setter for ManagedProperty injection
     *
     * @param searchBean the searchBean to set
     */
    public void setSearchBean(SearchBean searchBean) {
        this.searchBean = searchBean;
    }

    // time line view

    /**
     * The method generates the default values for start date and end date for the calendar view and time view. The Solr query is expensive during the
     * first execution so only execute is if MIN or MAX are actually set.
     *
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private void getDefaultDates() throws PresentationException, IndexUnreachableException {
        String start = DataManager.getInstance().getConfiguration().getStartYearForTimeline();
        String end = DataManager.getInstance().getConfiguration().getEndYearForTimeline();

        if ("MIN".equalsIgnoreCase(start) || "MAX".equalsIgnoreCase(end)) {
            StringBuilder sbSearchString = new StringBuilder(docstructFilterQuery);
            if (collection != null && !collection.isEmpty()) {
                sbSearchString.append(" AND ").append(SolrConstants.DC).append(':').append(collection);
            }
            int[] minMaxYears = SearchHelper.getMinMaxYears(sbSearchString.toString());
            if (start.equalsIgnoreCase("MIN")) {
                yearStart = Math.max(minMaxYears[0], MIN_ALLOWED_YEAR);
            } else {
                yearStart = Integer.parseInt(start);
            }

            if (end.equalsIgnoreCase("MAX")) {
                yearEnd = Math.min(minMaxYears[1], MAX_ALLOWED_YEAR);
            } else {
                yearEnd = Integer.parseInt(end);
            }
        } else {
            yearStart = Integer.parseInt(start);
            yearEnd = Integer.parseInt(end);
        }
    }

    /**
     * The method generates the data for the selected year and month for the time based view from solr index.
     *
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private void populateMonthData() throws PresentationException, IndexUnreachableException {
        monthRow = new CalendarRow();
        StringBuilder sbSearchString = new StringBuilder();
        if (collection != null && !collection.isEmpty()) {
            sbSearchString.append(SolrConstants.CALENDAR_YEAR)
                    .append(':')
                    .append(currentYear.getName())
                    .append(" AND ")
                    .append(SolrConstants.DC)
                    .append(':')
                    .append(collection)
                    .append('*')
                    .append(docstructFilterQuery);
        } else {
            sbSearchString.append(SolrConstants.CALENDAR_YEAR).append(':').append(currentYear.getName()).append(docstructFilterQuery);
        }
        QueryResponse resp =
                SearchHelper.searchCalendar(sbSearchString.toString(), Collections.singletonList(SolrConstants.CALENDAR_MONTH), 0, false);
        FacetField field = resp.getFacetField(SolrConstants.CALENDAR_MONTH);
        List<Count> fieldValues = field.getValues() != null ? field.getValues() : new ArrayList<>();

        Count jan = null;
        Count feb = null;
        Count mar = null;
        Count apr = null;
        Count may = null;
        Count jun = null;
        Count jul = null;
        Count aug = null;
        Count sep = null;
        Count oct = null;
        Count nov = null;
        Count dec = null;

        for (Count current : fieldValues) {
            if (current.getName().equals(currentYear.getName() + "01")) {
                jan = current;
            } else if (current.getName().equals(currentYear.getName() + "02")) {
                feb = current;
            } else if (current.getName().equals(currentYear.getName() + "03")) {
                mar = current;
            } else if (current.getName().equals(currentYear.getName() + "04")) {
                apr = current;
            } else if (current.getName().equals(currentYear.getName() + "05")) {
                may = current;
            } else if (current.getName().equals(currentYear.getName() + "06")) {
                jun = current;
            } else if (current.getName().equals(currentYear.getName() + "07")) {
                jul = current;
            } else if (current.getName().equals(currentYear.getName() + "08")) {
                aug = current;
            } else if (current.getName().equals(currentYear.getName() + "09")) {
                sep = current;
            } else if (current.getName().equals(currentYear.getName() + "10")) {
                oct = current;
            } else if (current.getName().equals(currentYear.getName() + "11")) {
                nov = current;
            } else if (current.getName().equals(currentYear.getName() + "12")) {
                dec = current;
            }
        }

        if (jan != null) {
            CalendarItemMonth january = new CalendarItemMonth(ViewerResourceBundle.getTranslation("january", null), 1, (int) jan.getCount());
            monthRow.addItem(january);
        } else {
            CalendarItemMonth january = new CalendarItemMonth(ViewerResourceBundle.getTranslation("january", null), 1, 0);
            monthRow.addItem(january);
        }
        if (feb != null) {
            CalendarItemMonth february = new CalendarItemMonth(ViewerResourceBundle.getTranslation("february", null), 2, (int) feb.getCount());
            monthRow.addItem(february);
        } else {
            CalendarItemMonth february = new CalendarItemMonth(ViewerResourceBundle.getTranslation("february", null), 2, 0);
            monthRow.addItem(february);
        }

        if (mar != null) {
            CalendarItemMonth march = new CalendarItemMonth(ViewerResourceBundle.getTranslation("march", null), 3, (int) mar.getCount());
            monthRow.addItem(march);
        } else {
            CalendarItemMonth march = new CalendarItemMonth(ViewerResourceBundle.getTranslation("march", null), 3, 0);
            monthRow.addItem(march);
        }
        if (apr != null) {
            CalendarItemMonth april = new CalendarItemMonth(ViewerResourceBundle.getTranslation("april", null), 4, (int) apr.getCount());
            monthRow.addItem(april);
        } else {
            CalendarItemMonth april = new CalendarItemMonth(ViewerResourceBundle.getTranslation("april", null), 4, 0);
            monthRow.addItem(april);
        }
        if (may != null) {
            CalendarItemMonth m = new CalendarItemMonth(ViewerResourceBundle.getTranslation("may", null), 5, (int) may.getCount());
            monthRow.addItem(m);
        } else {
            CalendarItemMonth m = new CalendarItemMonth(ViewerResourceBundle.getTranslation("may", null), 5, 0);
            monthRow.addItem(m);
        }
        if (jun != null) {
            CalendarItemMonth june = new CalendarItemMonth(ViewerResourceBundle.getTranslation("june", null), 6, (int) jun.getCount());
            monthRow.addItem(june);
        } else {
            CalendarItemMonth june = new CalendarItemMonth(ViewerResourceBundle.getTranslation("june", null), 6, 0);
            monthRow.addItem(june);
        }
        if (jul != null) {
            CalendarItemMonth july = new CalendarItemMonth(ViewerResourceBundle.getTranslation("july", null), 7, (int) jul.getCount());
            monthRow.addItem(july);
        } else {
            CalendarItemMonth july = new CalendarItemMonth(ViewerResourceBundle.getTranslation("july", null), 7, 0);
            monthRow.addItem(july);
        }
        if (aug != null) {
            CalendarItemMonth august = new CalendarItemMonth(ViewerResourceBundle.getTranslation("august", null), 8, (int) aug.getCount());
            monthRow.addItem(august);
        } else {
            CalendarItemMonth august = new CalendarItemMonth(ViewerResourceBundle.getTranslation("august", null), 8, 0);
            monthRow.addItem(august);
        }
        if (sep != null) {
            CalendarItemMonth september = new CalendarItemMonth(ViewerResourceBundle.getTranslation("september", null), 9, (int) sep.getCount());
            monthRow.addItem(september);
        } else {
            CalendarItemMonth september = new CalendarItemMonth(ViewerResourceBundle.getTranslation("september", null), 9, 0);
            monthRow.addItem(september);
        }
        if (oct != null) {
            CalendarItemMonth october = new CalendarItemMonth(ViewerResourceBundle.getTranslation("october", null), 10, (int) oct.getCount());
            monthRow.addItem(october);
        } else {
            CalendarItemMonth october = new CalendarItemMonth(ViewerResourceBundle.getTranslation("october", null), 10, 0);
            monthRow.addItem(october);
        }
        if (nov != null) {
            CalendarItemMonth november = new CalendarItemMonth(ViewerResourceBundle.getTranslation("november", null), 11, (int) nov.getCount());
            monthRow.addItem(november);
        } else {
            CalendarItemMonth november = new CalendarItemMonth(ViewerResourceBundle.getTranslation("november", null), 11, 0);
            monthRow.addItem(november);
        }
        if (dec != null) {
            CalendarItemMonth december = new CalendarItemMonth(ViewerResourceBundle.getTranslation("december", null), 12, (int) dec.getCount());
            monthRow.addItem(december);
        } else {
            CalendarItemMonth december = new CalendarItemMonth(ViewerResourceBundle.getTranslation("december", null), 12, 0);
            monthRow.addItem(december);
        }

        monthRow.setSelected(true);
    }

    /**
     * The method generates the data for the selected year, month and day for the time based view from solr index.
     *
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private void populateDayData() throws PresentationException, IndexUnreachableException {
        dayRow = new CalendarRow();
        String value = String.valueOf(currentMonth.getValue());
        if (value.length() == 1) {
            value = getActualYear() + "0" + value;
        } else {
            value = getActualYear() + value;
        }
        StringBuilder sbSearchString = new StringBuilder();
        if (collection != null && !collection.isEmpty()) {
            sbSearchString.append(SolrConstants.CALENDAR_MONTH)
                    .append(':')
                    .append(value)
                    .append(" AND ")
                    .append(SolrConstants.DC)
                    .append(':')
                    .append(collection)
                    .append('*')
                    .append(docstructFilterQuery);
        } else {
            sbSearchString.append(SolrConstants.CALENDAR_MONTH).append(':').append(value).append(docstructFilterQuery);
        }

        QueryResponse resp = SearchHelper.searchCalendar(sbSearchString.toString(), Collections.singletonList(SolrConstants.CALENDAR_DAY), 0, false);
        FacetField field = resp.getFacetField(SolrConstants.CALENDAR_DAY);
        List<Count> fieldValues = field.getValues() != null ? field.getValues() : new ArrayList<>();

        LocalDate date = LocalDate.now().withYear(Integer.parseInt(value)).withMonth(currentMonth.getValue()).withDayOfMonth(1);

        int daysInMonth = date.getMonth().length(date.isLeapYear());
        for (int day = 1; day <= daysInMonth; day++) {
            String facetName = generateSearchStringForDays(day);
            CalendarItemDay dayItem = null;
            for (Count count : fieldValues) {
                if (count.getName().equals(facetName)) {
                    dayItem = new CalendarItemDay(String.valueOf(day), day, (int) count.getCount());
                    break;
                }
            }
            if (dayItem == null) {
                dayItem = new CalendarItemDay(String.valueOf(day), day, 0);
            }
            // date = new LocalDate(Integer.parseInt(value), currentMonth.getValue(), day, calendar);
            date = LocalDate.now().withYear(Integer.parseInt(value)).withMonth(currentMonth.getValue()).withDayOfMonth(day);
            switch (date.getDayOfWeek()) {
                case SUNDAY:
                    dayItem.setDayOfWeek("Sunday");
                    break;
                case MONDAY:
                    dayItem.setDayOfWeek("Monday");
                    break;
                case TUESDAY:
                    dayItem.setDayOfWeek("Tuesday");
                    break;
                case WEDNESDAY:
                    dayItem.setDayOfWeek("Wednesday");
                    break;
                case THURSDAY:
                    dayItem.setDayOfWeek("Thursday");
                    break;
                case FRIDAY:
                    dayItem.setDayOfWeek("Friday");
                    break;
                default:
                    dayItem.setDayOfWeek("Saturday");
                    break;
            }
            dayRow.addItem(dayItem);

        }
        dayRow.setSelected(true);
    }

    /**
     * This method generates a search string for selected year, month and day.
     *
     * @param day
     * @return String with format YYYYMMDD
     */

    private String generateSearchStringForDays(int day) {
        StringBuilder builder = new StringBuilder();
        builder.append(getActualYear());
        if (currentMonth.getValue() < 10) {
            builder.append('0').append(currentMonth.getValue());
        } else {
            builder.append(currentMonth.getValue());
        }
        if (day < 10) {
            builder.append('0').append(day);
        } else {
            builder.append(String.valueOf(day));
        }
        return builder.toString();
    }

    /**
     * <p>
     * Getter for the field <code>currentYear</code>.
     * </p>
     *
     * @return selected year
     */
    public CalendarItemYear getCurrentYear() {
        return currentYear;
    }

    /**
     * Set a new value for year. If the selected year is the same as the old selection, the data will be unselected. Otherwise the new data will be
     * generated.
     *
     * @param currentYear a {@link io.goobi.viewer.model.calendar.CalendarItemYear} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public void setCurrentYear(CalendarItemYear currentYear) throws PresentationException, IndexUnreachableException {
        if (this.currentYear == currentYear && monthRow.isSelected()) {
            monthRow.setSelected(false);
            dayRow.setSelected(false);
            currentDay = null;
            currentMonth = null;
            this.currentYear = null;
        } else {
            this.currentYear = currentYear;
            // populate month row
            populateMonthData();
            currentDay = null;
            currentMonth = null;
            dayRow.setSelected(false);
        }
    }

    /**
     * <p>
     * Getter for the field <code>rowIndex</code>.
     * </p>
     *
     * @return a int.
     */
    public int getRowIndex() {
        return rowIndex;
    }

    /**
     * <p>
     * Setter for the field <code>rowIndex</code>.
     * </p>
     *
     * @param rowIndex a int.
     */
    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    /**
     * <p>
     * Getter for the field <code>monthRow</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.calendar.CalendarRow} object.
     */
    public CalendarRow getMonthRow() {
        return monthRow;
    }

    /**
     * <p>
     * Setter for the field <code>monthRow</code>.
     * </p>
     *
     * @param monthRow a {@link io.goobi.viewer.model.calendar.CalendarRow} object.
     */
    public void setMonthRow(CalendarRow monthRow) {
        this.monthRow = monthRow;
    }

    /**
     * <p>
     * Getter for the field <code>currentMonth</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.calendar.CalendarItemMonth} object.
     */
    public CalendarItemMonth getCurrentMonth() {
        return currentMonth;
    }

    /**
     * Set a new value for month. If the selected month is the same as the old selection, the month and day will be unselected. Otherwise the data for
     * the month gets generated.
     *
     * @param currentMonth a {@link io.goobi.viewer.model.calendar.CalendarItemMonth} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public void setCurrentMonth(CalendarItemMonth currentMonth) throws PresentationException, IndexUnreachableException {
        if (this.currentMonth == currentMonth && dayRow.isSelected()) {
            currentDay = null;
            this.currentMonth = null;
            dayRow.setSelected(false);
        } else {
            this.currentMonth = currentMonth;
            currentDay = null;
            populateDayData();
        }
    }

    /**
     * <p>
     * Getter for the field <code>currentDay</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.calendar.CalendarItemDay} object.
     */
    public CalendarItemDay getCurrentDay() {
        return currentDay;
    }

    /**
     * <p>
     * Setter for the field <code>currentDay</code>.
     * </p>
     *
     * @param currentDay a {@link io.goobi.viewer.model.calendar.CalendarItemDay} object.
     */
    public void setCurrentDay(CalendarItemDay currentDay) {
        if (this.currentDay == currentDay) {
            this.currentDay = null;
        } else {
            this.currentDay = currentDay;
        }
    }

    /**
     * <p>
     * getCurrentDate.
     * </p>
     *
     * @return a {@link java.time.LocalDateTime} object.
     */
    public LocalDateTime getCurrentDate() {
        LocalDateTime ldt = LocalDateTime.now();
        if (currentYear != null) {
            ldt = ldt.withYear(currentYear.getValue());
        }
        if (currentMonth != null) {
            ldt = ldt.withMonth(currentMonth.getValue());
        }
        if (currentDay != null) {
            ldt = ldt.withDayOfMonth(currentDay.getValue());
        }

        return ldt;
    }

    public String getCurrentDateAsString() {
        LocalDateTime ldt = getCurrentDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(BeanUtils.getLocale());
        return ldt.format(formatter);

    }

    /**
     * <p>
     * Getter for the field <code>dayRow</code>.
     * </p>
     *
     * @return the dayRow
     */
    public CalendarRow getDayRow() {
        return dayRow;
    }

    /**
     * <p>
     * Setter for the field <code>dayRow</code>.
     * </p>
     *
     * @param dayRow the dayRow to set
     */
    public void setDayRow(CalendarRow dayRow) {
        this.dayRow = dayRow;
    }

    /**
     * This method generates the search string for the time line based search tab. The search string will be handed over to the search bean to execute
     * the search.
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String searchTimeline() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        StringBuilder builder = new StringBuilder();

        if (collection != null && !collection.isEmpty()) {
            builder.append(SolrConstants.DC).append(':').append(collection).append("* AND ");
        }
        if (currentDay != null) {
            builder.append(SolrConstants.CALENDAR_DAY).append(':');
            builder.append(currentYear.getName());
            if (currentMonth.getValue() < 10) {
                builder.append('0').append(currentMonth.getValue());
            } else {
                builder.append(currentMonth.getValue());
            }
            if (currentDay.getValue() < 10) {
                builder.append('0').append(currentDay.getValue());
            } else {
                builder.append(currentDay.getValue());
            }

        } else if (currentMonth != null) {
            builder.append(SolrConstants.CALENDAR_MONTH).append(':');
            builder.append(currentYear.getName());
            if (currentMonth.getValue() < 10) {
                builder.append('0').append(currentMonth.getValue());
            } else {
                builder.append(currentMonth.getValue());
            }
        } else {
            builder.append(SolrConstants.CALENDAR_YEAR).append(':');
            builder.append(currentYear.getName());
        }

        builder.append(docstructFilterQuery);
        searchBean.resetSearchResults();
        searchBean.setCurrentPage(1);
        searchBean.mirrorAdvancedSearchCurrentHierarchicalFacets();
        searchBean.getFacets().setActiveFacetString("-");
        searchBean.setExactSearchString(builder.toString());
        searchBean.executeSearch();
        return "pretty:newSearch5";
    }

    /**
     * This method resets the current selection for year, month and day; also selectYear and collection.
     */
    public void resetCurrentSelection() {
        currentDay = null;
        currentMonth = null;
        currentYear = null;
        if (monthRow != null) {
            monthRow.setSelected(false);
        }
        if (dayRow != null) {
            dayRow.setSelected(false);
        }
        selectYear = null;
        collection = null;
        try {
            getDefaultDates();
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here");
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here");
        }
    }

    public void resetAllActiveYears() {
        this.allActiveCenturies = null;
    }

    /**
     * This method returns a list of all active years. <br />
     *
     * The method searches for the facet of the field 'YEAR'. If the count of a facet is greater than 0, the year is active.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    //    public List<CalendarItemYear> getAllActiveYears() throws PresentationException, IndexUnreachableException {
    //        if (allActiveYears == null) {
    //            allActiveYears = new ArrayList<>();
    //            List<String> fields = new ArrayList<>();
    //            fields.add(SolrConstants.CALENDAR_YEAR);
    //            fields.add(SolrConstants.CALENDAR_DAY);
    //            StringBuilder sbSearchString = new StringBuilder();
    //            if (collection != null && !collection.isEmpty()) {
    //                sbSearchString.append(SolrConstants.CALENDAR_DAY)
    //                        .append(":* AND ")
    //                        .append(SolrConstants.DC)
    //                        .append(':')
    //                        .append(collection)
    //                        .append('*')
    //                        .append(docstructFilterQuery);
    //            } else {
    //                sbSearchString.append(SolrConstants.CALENDAR_DAY).append(":*").append(docstructFilterQuery);
    //            }
    //            sbSearchString.append(SearchHelper.getAllSuffixes());
    //
    //            logger.trace("getAllActiveYears query: {}", sbSearchString);
    //            QueryResponse resp = SearchHelper.searchCalendar(sbSearchString.toString(), fields, 1, false);
    //
    //            FacetField facetFieldDay = resp.getFacetField(SolrConstants.CALENDAR_DAY);
    //            List<Count> dayCounts = facetFieldDay.getValues() != null ? facetFieldDay.getValues() : new ArrayList<>();
    //            Map<Integer, Integer> yearCountMap = new HashMap<>();
    //            for (Count day : dayCounts) {
    //                int year = Integer.parseInt(day.getName().substring(0, 4));
    //                Integer count = yearCountMap.get(year);
    //                if (count == null) {
    //                    count = 0;
    //                }
    //                yearCountMap.put(year, (int) (count + day.getCount()));
    //            }
    //
    //            List<Integer> years = new ArrayList<>(yearCountMap.keySet());
    //            Collections.sort(years);
    //            for (int year : years) {
    //                CalendarItemYear item = new CalendarItemYear(String.valueOf(year), year, yearCountMap.get(year));
    //                allActiveYears.add(item);
    //            }
    //        }
    //        return allActiveYears;
    //    }

    /**
     * This method returns a list of all active centuries. <br />
     *
     * The method searches for the facet of the field 'CENTURY'. If the count of a facet is greater than 0, the century is active.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<CalendarItemCentury> getAllActiveCenturies() throws PresentationException, IndexUnreachableException {
        if (allActiveCenturies == null) {
            QueryResponse resp = getFacetCounts(SolrConstants.CALENDAR_CENTURY, SolrConstants.CALENDAR_YEAR,
                    SolrConstants.CALENDAR_DAY);

            Map<Integer, Integer> yearCountMap = getCounts(resp, SolrConstants.CALENDAR_YEAR);

            Map<Integer, CalendarItemCentury> centuries = new HashMap<>();

            for (Entry<Integer, Integer> yearEntry : yearCountMap.entrySet()) {
                Integer year = yearEntry.getKey();
                Integer count = yearEntry.getValue();
                Integer century = getCentury(year);
                CalendarItemCentury centuryItem =
                        centuries.computeIfAbsent(century, cent -> new CalendarItemCentury(String.valueOf(century), century, 0));
                centuryItem.addYearHits(year, count);
            }
            centuries.values().forEach(c -> fillEmptyYears(c, LocalDate.now().getYear()));
            this.allActiveCenturies = centuries;
        }
        return allActiveCenturies.values().stream().sorted().toList();
    }

    private void fillEmptyYears(CalendarItemCentury century, Integer maxYear) {
        for (int i = 0; i < 100; i++) {
            Integer year = (century.getValue() - 1) * 100 + i;
            if (year > maxYear) {
                break;
            } else {
                if (century.getYear(year) == null) {
                    century.addYearHits(year, 0);
                }
            }
        }
    }

    public Integer getCentury(Integer year) {
        return year / 100 + (int) Math.signum(year);
    }

    private Map<Integer, Integer> getCounts(QueryResponse resp, String facetField) {
        FacetField facet = resp.getFacetField(facetField);
        List<Count> counts = facet.getValues() != null ? facet.getValues() : new ArrayList<>();
        Map<Integer, Integer> countMap = new HashMap<>();
        for (Count count : counts) {
            countMap.put(Integer.parseInt(count.getName()), (int) count.getCount());
        }
        return countMap;
    }

    public QueryResponse getFacetCounts(String... fields) throws PresentationException, IndexUnreachableException {
        StringBuilder sbSearchString = new StringBuilder();
        sbSearchString.append(
                String.format("+%s:* +%s: [%d TO %d]", SolrConstants.CALENDAR_DAY, SolrConstants.CALENDAR_YEAR, this.yearStart, this.yearEnd));
        if (StringUtils.isNotBlank(this.collection)) {
            sbSearchString.append(" +")
                    .append(SolrConstants.DC)
                    .append(':')
                    .append(collection)
                    .append("*");
        }
        sbSearchString.append(docstructFilterQuery);
        sbSearchString.append(SearchHelper.getAllSuffixes());

        logger.trace("getAllActiveYears query: {}", sbSearchString);
        QueryResponse resp = SearchHelper.searchCalendar(sbSearchString.toString(), Arrays.asList(fields), 1, false);
        return resp;
    }

    /**
     * <p>
     * resetYears.
     * </p>
     *
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public void resetYears() throws PresentationException, IndexUnreachableException {
        logger.trace("resetYears");
        allActiveCenturies = null;
        getAllActiveCenturies();
    }

    // calendar view

    /**
     * Select a year. If the new value differs from the old one, the data for the new value gets generated.
     *
     * @param selectYear the selectYear to set
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws IOException
     */
    public void setSelectYear(String selectYear) throws PresentationException, IndexUnreachableException, IOException {
        logger.trace("setSelectYear: {}", selectYear);
        if (this.selectYear == null || !this.selectYear.equals(selectYear)) {
            this.selectYear = selectYear;
            currentDay = null;
            currentMonth = null;
            monthList = populateMonthsWithDays(selectYear, collection, docstructFilterQuery);
            Integer year = Integer.parseInt(selectYear);
            Integer century = getCentury(year);
            if (this.allActiveCenturies != null) {
                setCurrentYear(this.allActiveCenturies.getOrDefault(century, new CalendarItemCentury(century)).getYear(year));
            }
        }
    }

    /**
     * <p>
     * Getter for the field <code>selectYear</code>.
     * </p>
     *
     * @return the selectYear
     */
    public String getSelectYear() {
        return selectYear;
    }

    /**
     * This method generates the data for each month of the selected year. <br/>
     * It runs a facet search for YEARMONTH and YEARMONTHDAY for the current year. For each day of the year, the method checks if the count of the
     * field YEARMONTHDAY is greater than 0. If this is the case, the day is an active element, otherwise it has no hits.
     *
     * @param selectYear a {@link java.lang.String} object.
     * @param collection a {@link java.lang.String} object.
     * @param filterQuery a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    @SuppressWarnings("fallthrough")
    public static List<CalendarItemMonth> populateMonthsWithDays(String selectYear, String collection, String filterQuery)
            throws PresentationException, IndexUnreachableException {
        List<CalendarItemMonth> monthList = new ArrayList<>();
        QueryResponse resp = null;
        if (StringUtils.isEmpty(selectYear)) {
            return monthList;
        }
        List<String> facetFields = new ArrayList<>();
        facetFields.add(SolrConstants.CALENDAR_DAY);
        facetFields.add(SolrConstants.CALENDAR_MONTH);

        StringBuilder sbSearchString = new StringBuilder();
        if (collection != null && !collection.isEmpty()) {
            sbSearchString.append(SolrConstants.CALENDAR_YEAR)
                    .append(':')
                    .append(ClientUtils.escapeQueryChars(selectYear))
                    .append(" AND ")
                    .append(SolrConstants.DC)
                    .append(':')
                    .append(collection)
                    .append('*')
                    .append(filterQuery);
        } else {
            sbSearchString.append(SolrConstants.CALENDAR_YEAR).append(':').append(ClientUtils.escapeQueryChars(selectYear)).append(filterQuery);
        }

        resp = SearchHelper.searchCalendar(sbSearchString.toString(), facetFields, 0, false);

        List<Count> dayFacets = resp.getFacetField(SolrConstants.CALENDAR_DAY) != null ? resp.getFacetField(SolrConstants.CALENDAR_DAY).getValues()
                : new ArrayList<>();
        List<Count> monthFacets = resp.getFacetField(SolrConstants.CALENDAR_MONTH).getValues() != null
                ? resp.getFacetField(SolrConstants.CALENDAR_MONTH).getValues() : new ArrayList<>();

        CalendarItemMonth jan = new CalendarItemMonth("january", 1, 0);
        monthList.add(jan);
        CalendarItemMonth feb = new CalendarItemMonth("february", 2, 0);
        monthList.add(feb);
        CalendarItemMonth mar = new CalendarItemMonth("march", 3, 0);
        monthList.add(mar);
        CalendarItemMonth apr = new CalendarItemMonth("april", 4, 0);
        monthList.add(apr);
        CalendarItemMonth may = new CalendarItemMonth("may", 5, 0);
        monthList.add(may);
        CalendarItemMonth jun = new CalendarItemMonth("june", 6, 0);
        monthList.add(jun);
        CalendarItemMonth jul = new CalendarItemMonth("july", 7, 0);
        monthList.add(jul);
        CalendarItemMonth aug = new CalendarItemMonth("august", 8, 0);
        monthList.add(aug);
        CalendarItemMonth sep = new CalendarItemMonth("september", 9, 0);
        monthList.add(sep);
        CalendarItemMonth oct = new CalendarItemMonth("october", 10, 0);
        monthList.add(oct);
        CalendarItemMonth nov = new CalendarItemMonth("november", 11, 0);
        monthList.add(nov);
        CalendarItemMonth dec = new CalendarItemMonth("december", 12, 0);
        monthList.add(dec);

        for (Count monthCount : monthFacets) {
            if (monthCount.getName().length() < 6) {
                logger.warn("{} facet name too short: {}", SolrConstants.CALENDAR_MONTH, monthCount.getName());
                continue;
            }
            int monthNumber = Integer.parseInt(monthCount.getName().substring(4));
            //            if (monthCount.getCount() > 0) {
            //                logger.trace("{}: {}", monthCount.getName(), monthCount.getCount()); //NOSONAR Debug
            //            }
            switch (monthNumber) {
                case 1:
                    jan.setHits((int) monthCount.getCount());
                    break;
                case 2:
                    feb.setHits((int) monthCount.getCount());
                    break;
                case 3:
                    mar.setHits((int) monthCount.getCount());
                    break;
                case 4:
                    apr.setHits((int) monthCount.getCount());
                    break;
                case 5:
                    may.setHits((int) monthCount.getCount());
                    break;
                case 6:
                    jun.setHits((int) monthCount.getCount());
                    break;
                case 7:
                    jul.setHits((int) monthCount.getCount());
                    break;
                case 8:
                    aug.setHits((int) monthCount.getCount());
                    break;
                case 9:
                    sep.setHits((int) monthCount.getCount());
                    break;
                case 10:
                    oct.setHits((int) monthCount.getCount());
                    break;
                case 11:
                    nov.setHits((int) monthCount.getCount());
                    break;
                case 12:
                    dec.setHits((int) monthCount.getCount());
                    break;
                default:
                    break;
            }
        }

        for (CalendarItemMonth monthItem : monthList) {
            List<CalendarItemWeek> weeksOfMonth = new ArrayList<>();
            CalendarItemWeek currentWeek = new CalendarItemWeek("", 0, 0);
            weeksOfMonth.add(currentWeek);
            monthItem.setWeeksOfMonth(weeksOfMonth);
            // LocalDate date = new LocalDate(Integer.parseInt(selectYear), monthItem.getValue(), 1, calendar);
            LocalDate date = LocalDate.now().withYear(Integer.parseInt(selectYear)).withMonth(monthItem.getValue()).withDayOfMonth(1);
            int daysInMonth = date.getMonth().length(date.isLeapYear());
            for (int day = 1; day <= daysInMonth; day++) {
                StringBuilder facetBuilder = new StringBuilder();
                facetBuilder.append(selectYear);
                if (monthItem.getValue() < 10) {
                    facetBuilder.append('0').append(monthItem.getValue());
                } else {
                    facetBuilder.append(String.valueOf(monthItem.getValue()));
                }
                if (day < 10) {
                    facetBuilder.append('0').append(day);
                } else {
                    facetBuilder.append(String.valueOf(day));
                }
                String facetName = facetBuilder.toString();

                CalendarItemDay dayItem = null;
                for (Count count : dayFacets) {
                    if (count.getName().equals(facetName)) {
                        dayItem = new CalendarItemDay(String.valueOf(day), day, (int) count.getCount());
                        String query = new StringBuilder().append(SolrConstants.CALENDAR_DAY)
                                .append(':')
                                .append(selectYear)
                                .append(monthItem.getFormattedValue())
                                .append(dayItem.getFormattedValue()) + filterQuery;
                        dayItem.setQuery(query);
                        break;
                    }
                }
                if (dayItem == null) {
                    dayItem = new CalendarItemDay(String.valueOf(day), day, 0);
                }
                //                date = new LocalDate(Integer.parseInt(selectYear), monthItem.getValue(), day, calendar);
                date = LocalDate.now().withYear(Integer.parseInt(selectYear)).withMonth(monthItem.getValue()).withDayOfMonth(day);
                switch (date.getDayOfWeek()) {
                    case MONDAY:
                        dayItem.setDayOfWeek("Monday");
                        if (!currentWeek.getDaysOfWeek().isEmpty()) {
                            currentWeek = new CalendarItemWeek("", 0, 0);
                            weeksOfMonth.add(currentWeek);
                        }
                        break;
                    case TUESDAY:
                        dayItem.setDayOfWeek("Tuesday");
                        break;
                    case WEDNESDAY:
                        dayItem.setDayOfWeek("Wednesday");
                        break;
                    case THURSDAY:
                        dayItem.setDayOfWeek("Thursday");
                        break;
                    case FRIDAY:
                        dayItem.setDayOfWeek("Friday");
                        break;
                    case SATURDAY:
                        dayItem.setDayOfWeek("Saturday");
                        break;
                    case SUNDAY:
                        dayItem.setDayOfWeek("Sunday");
                        break;
                    default:
                        dayItem.setDayOfWeek("unknown");
                        break;
                }
                if (date.getDayOfMonth() == 1) {
                    addEmptyDays(currentWeek, date);
                }
                currentWeek.addDay(dayItem);
            }
        }

        return monthList;
    }

    /**
     * Add as many {@link CalendarItemDay}s to 'currentWeek' as there are days between the start of the month and the previous monday
     * 
     * @param currentWeek
     * @param date
     */
    protected static void addEmptyDays(CalendarItemWeek currentWeek, LocalDate date) {

        LocalDate previousMonday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long daysToAdd = Duration.between(previousMonday.atStartOfDay(), date.atStartOfDay()).toDays();
        LongStream.range(0, daysToAdd).forEach(i -> currentWeek.addDay(new CalendarItemDay("", 0, 0)));

    }

    /**
     * <p>
     * Getter for the field <code>monthList</code>.
     * </p>
     *
     * @return the monthList
     */
    public List<CalendarItemMonth> getMonthList() {
        return monthList;
    }

    /**
     * <p>
     * Getter for the field <code>yearEnd</code>.
     * </p>
     *
     * @return the yearEnd
     */
    public int getYearEnd() {
        return yearEnd;
    }

    /**
     * <p>
     * Getter for the field <code>yearStart</code>.
     * </p>
     *
     * @return the yearStart
     */
    public int getYearStart() {
        return yearStart;
    }

    /**
     * <p>
     * Setter for the field <code>yearEnd</code>.
     * </p>
     *
     * @param yearEnd the yearEnd to set
     */
    public void setYearEnd(int yearEnd) {
        this.yearEnd = yearEnd;
    }

    /**
     * <p>
     * Setter for the field <code>yearStart</code>.
     * </p>
     *
     * @param yearStart the yearStart to set
     */
    public void setYearStart(int yearStart) {
        this.yearStart = yearStart;
    }

    /**
     * <p>
     * Setter for the field <code>collection</code>.
     * </p>
     *
     * @param collection the collection to set
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public void setCollection(String collection) throws PresentationException, IndexUnreachableException {
        if (this.collection == null || !this.collection.equals(collection)) {
            this.collection = collection;
            populateMonthsWithDays(selectYear, collection, docstructFilterQuery);
        }
    }

    /**
     * <p>
     * Getter for the field <code>collection</code>.
     * </p>
     *
     * @return the collection
     */
    public String getCollection() {
        return collection;
    }

    /**
     * <p>
     * searchCalendar.
     * </p>
     *
     * @param month a {@link io.goobi.viewer.model.calendar.CalendarItemMonth} object.
     * @param day a {@link io.goobi.viewer.model.calendar.CalendarItemDay} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String searchCalendar(CalendarItemMonth month, CalendarItemDay day)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        currentMonth = month;
        currentDay = day;
        return searchCalendar();
    }

    /**
     * This method generates the search string for the calendar search tab. The search string will be handed over to the search bean to execute the
     * search.
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String searchCalendar() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (StringUtils.isEmpty(selectYear)) {
            logger.trace("No year selected");
            return "";
        }

        StringBuilder builder = new StringBuilder();

        //        if (collection != null && !collection.isEmpty()) {
        //            builder.append(SolrConstants.DC).append(':').append(collection).append("* AND ");
        //        }
        if (currentDay != null) {
            builder.append(SolrConstants.CALENDAR_DAY).append(':');
            builder.append(selectYear);
            if (currentMonth.getValue() < 10) {
                builder.append('0').append(currentMonth.getValue());
            } else {
                builder.append(currentMonth.getValue());
            }
            if (currentDay.getValue() < 10) {
                builder.append('0').append(currentDay.getValue());
            } else {
                builder.append(currentDay.getValue());
            }

        } else if (currentMonth != null) {
            builder.append(SolrConstants.CALENDAR_MONTH).append(':');
            builder.append(selectYear);
            if (currentMonth.getValue() < 10) {
                builder.append('0').append(currentMonth.getValue());
            } else {
                builder.append(currentMonth.getValue());
            }
        } else {
            builder.append(SolrConstants.CALENDAR_YEAR).append(':');
            builder.append(selectYear);
        }

        // builder.append(docstructFilterQuery);
        searchBean.resetSearchResults();
        searchBean.setCurrentPage(1);
        searchBean.mirrorAdvancedSearchCurrentHierarchicalFacets();
        searchBean.setExactSearchString(builder.toString());
        searchBean.executeSearch();
        return "pretty:searchCalendar4";
    }

    /**
     * This method generates a search string to search for data with a value in YEAR but without a value in YEARMONTHDAY.
     *
     * @param date
     * @return Generated query
     */
    private String getQueryForIncompleteData(String date) {
        StringBuilder searchString = new StringBuilder();
        if (collection != null && !collection.isEmpty()) {
            searchString.append(SolrConstants.DC).append(':').append(collection).append("* AND ");

        }
        searchString.append(SolrConstants.CALENDAR_YEAR).append(':').append(date);
        searchString.append(" AND -").append(SolrConstants.CALENDAR_DAY).append(":*");
        searchString.append(docstructFilterQuery);
        return searchString.toString();
    }

    /**
     * This method returns the count of all data without a value in YEARMONTHDAY for the selected year.
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public int getCountOfIncompleteData() throws PresentationException, IndexUnreachableException {
        String year = getActualYear();
        if (year == null) {
            return 0;
        }
        String query = getQueryForIncompleteData(year);
        QueryResponse resp = SearchHelper.searchCalendar(query, Collections.singletonList(SolrConstants.CALENDAR_YEAR), 0, false);
        FacetField field = resp.getFacetField(SolrConstants.CALENDAR_YEAR);
        List<Count> fieldValues = field.getValues() != null ? field.getValues() : new ArrayList<>();
        for (Count count : fieldValues) {
            if (count.getName().equals(year)) {
                return (int) count.getCount();
            }
        }

        return 0;
    }

    /**
     * <p>
     * getActualYear.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getActualYear() {
        String year = "";
        if (searchBean.getActiveSearchType() == SearchHelper.SEARCH_TYPE_TIMELINE) {
            if (currentYear == null) {
                return null;
            }
            year = currentYear.getName();
        } else {
            if (StringUtils.isEmpty(selectYear)) {
                return null;
            }
            year = selectYear;
        }
        return year;
    }

    /**
     * This method generates the search string for incomplete data. The search string will be handed over to the search bean to execute the search.
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String searchForIncompleteData() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        String year = "";
        if (searchBean.getActiveSearchType() == SearchHelper.SEARCH_TYPE_TIMELINE) {
            if (currentYear == null) {
                return "";
            }
            year = currentYear.getName();
        } else {
            if (StringUtils.isEmpty(selectYear)) {
                return "";
            }
            year = selectYear;
        }

        searchBean.setCurrentPage(1);
        searchBean.mirrorAdvancedSearchCurrentHierarchicalFacets();
        searchBean.setExactSearchString(getQueryForIncompleteData(year));
        searchBean.executeSearch();
        return "pretty:newSearch5";
    }

    public String getCenturyLabel(Integer century, Locale locale) {
        RuleBasedNumberFormat format = new RuleBasedNumberFormat(locale, RuleBasedNumberFormat.ORDINAL);
        return format.format(century);

    }

}
