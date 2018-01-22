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
package de.intranda.digiverso.presentation.managedbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeField;
import org.joda.time.LocalDate;
import org.joda.time.MutableDateTime;
import org.joda.time.chrono.GregorianChronology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.calendar.CalendarItemDay;
import de.intranda.digiverso.presentation.model.calendar.CalendarItemMonth;
import de.intranda.digiverso.presentation.model.calendar.CalendarItemWeek;
import de.intranda.digiverso.presentation.model.calendar.CalendarItemYear;
import de.intranda.digiverso.presentation.model.calendar.CalendarRow;
import de.intranda.digiverso.presentation.model.search.SearchHelper;

/**
 * This bean provides data for the calendar and time based search entries.
 */
@Named
@SessionScoped
public class CalendarBean implements Serializable {

    private static final long serialVersionUID = 1095535586988646463L;

    private static final Logger logger = LoggerFactory.getLogger(CalendarBean.class);

    @Inject
    private SearchBean searchBean;

    private List<CalendarRow> rowList = new ArrayList<>();
    private List<CalendarItemYear> allActiveYears;
    private CalendarItemYear currentYear;
    private CalendarItemMonth currentMonth;
    private CalendarItemDay currentDay;

    private int yearStart = 1750;
    private int yearEnd = 3000;
    private String collection;

    private static final int NUMBER_OF_ITEMS_IN_ROW = 10;
    private int rowIndex;

    private CalendarRow monthRow = new CalendarRow();
    private CalendarRow dayRow = new CalendarRow();

    private String docstructFilterQuery = "";
    private String selectYear;

    private List<CalendarItemMonth> monthList;

    public CalendarBean() {
        // the emptiness inside
    }

    @PostConstruct
    private void init() {
        // PostConstruct methods may not throw exceptions
        logger.trace("init");
        //        StringBuilder sbQuery = new StringBuilder();
        //        try {
        //            List<String> list = DataManager.getInstance().getConfiguration().getCalendarDocStructTypes();
        //            if (!list.isEmpty()) {
        //                sbQuery.append(" AND (");
        //                for (String s : list) {
        //                    if (StringUtils.isNotBlank(s)) {
        //                        sbQuery.append(SolrConstants.DOCSTRCT).append(':').append(ClientUtils.escapeQueryChars(s.trim())).append(" OR ");
        //                    }
        //                }
        //                sbQuery.delete(sbQuery.length() - 4, sbQuery.length());
        //                sbQuery.append(')');
        //            }
        //        } finally {
        //            docstructFilterQuery = sbQuery.toString();
        //            logger.trace("docstructFilterQuery: {}", docstructFilterQuery);
        //        }
        try {
            getDefaultDates();
            populateYearData();
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
                yearStart = minMaxYears[0];
            } else {
                yearStart = Integer.parseInt(start);
            }

            if (end.equalsIgnoreCase("MAX")) {
                yearEnd = minMaxYears[1];
            } else {
                yearEnd = Integer.parseInt(end);
            }
        } else {
            yearStart = Integer.parseInt(start);
            yearEnd = Integer.parseInt(end);
        }
    }

    /**
     * The method generates the data for the selected year for the time based view from solr index.
     *
     * @throws IndexUnreachableException
     * @throws PresentationException
     */

    public void populateYearData() throws IndexUnreachableException, PresentationException {
        rowList = new ArrayList<>();
        StringBuilder sbSearchString = new StringBuilder();
        if (collection != null && !collection.isEmpty()) {
            sbSearchString.append(SolrConstants._CALENDAR_YEAR).append(":[").append(yearStart).append(" TO ").append(yearEnd).append("] AND ").append(
                    SolrConstants.DC).append(':').append(collection).append('*').append(docstructFilterQuery);
        } else {
            sbSearchString.append(SolrConstants._CALENDAR_YEAR).append(":[").append(yearStart).append(" TO ").append(yearEnd).append(']').append(
                    docstructFilterQuery);
        }
        sbSearchString.append(SearchHelper.getAllSuffixes(DataManager.getInstance().getConfiguration().isSubthemeAddFilterQuery()));
        //            logger.debug("searchString: " + searchString);
        QueryResponse resp = SearchHelper.searchCalendar(sbSearchString.toString(), Collections.singletonList(SolrConstants._CALENDAR_YEAR), 0,
                false);
        //            logger.debug("search end");

        FacetField field = resp.getFacetField(SolrConstants._CALENDAR_YEAR);
        List<Count> fieldValues = field.getValues() != null ? field.getValues() : new ArrayList<>();
        CalendarRow row = new CalendarRow();

        if (fieldValues == null) {
            //                logger.error("Unable to retrieve calendar field values");
            return;
        }

        for (int currentYear = yearStart; currentYear <= yearEnd; currentYear++) {
            if (row.getItemList().size() % NUMBER_OF_ITEMS_IN_ROW == 0) {
                row = new CalendarRow();
                rowList.add(row);
            }
            boolean match = false;
            String name = String.valueOf(currentYear);
            for (Count count : fieldValues) {
                if (count.getName().equals(name)) {
                    CalendarItemYear year = new CalendarItemYear(count.getName(), Integer.parseInt(count.getName()), (int) count.getCount());
                    row.addItem(year);
                    match = true;
                    break;
                }
            }
            if (!match) {
                CalendarItemYear year = new CalendarItemYear(name, currentYear, 0);
                row.addItem(year);
            }
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
            sbSearchString.append(SolrConstants._CALENDAR_YEAR).append(':').append(currentYear.getName()).append(" AND ").append(SolrConstants.DC)
                    .append(':').append(collection).append('*').append(docstructFilterQuery);
        } else {
            sbSearchString.append(SolrConstants._CALENDAR_YEAR).append(':').append(currentYear.getName()).append(docstructFilterQuery);
        }
        QueryResponse resp = SearchHelper.searchCalendar(sbSearchString.toString(), Collections.singletonList(SolrConstants._CALENDAR_MONTH), 0,
                false);
        FacetField field = resp.getFacetField(SolrConstants._CALENDAR_MONTH);
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
            CalendarItemMonth january = new CalendarItemMonth(Helper.getTranslation("january", null), 1, (int) jan.getCount());
            monthRow.addItem(january);
        } else {
            CalendarItemMonth january = new CalendarItemMonth(Helper.getTranslation("january", null), 1, 0);
            monthRow.addItem(january);
        }
        if (feb != null) {
            CalendarItemMonth february = new CalendarItemMonth(Helper.getTranslation("february", null), 2, (int) feb.getCount());
            monthRow.addItem(february);
        } else {
            CalendarItemMonth february = new CalendarItemMonth(Helper.getTranslation("february", null), 2, 0);
            monthRow.addItem(february);
        }

        if (mar != null) {
            CalendarItemMonth march = new CalendarItemMonth(Helper.getTranslation("march", null), 3, (int) mar.getCount());
            monthRow.addItem(march);
        } else {
            CalendarItemMonth march = new CalendarItemMonth(Helper.getTranslation("march", null), 3, 0);
            monthRow.addItem(march);
        }
        if (apr != null) {
            CalendarItemMonth april = new CalendarItemMonth(Helper.getTranslation("april", null), 4, (int) apr.getCount());
            monthRow.addItem(april);
        } else {
            CalendarItemMonth april = new CalendarItemMonth(Helper.getTranslation("april", null), 4, 0);
            monthRow.addItem(april);
        }
        if (may != null) {
            CalendarItemMonth m = new CalendarItemMonth(Helper.getTranslation("may", null), 5, (int) may.getCount());
            monthRow.addItem(m);
        } else {
            CalendarItemMonth m = new CalendarItemMonth(Helper.getTranslation("may", null), 5, 0);
            monthRow.addItem(m);
        }
        if (jun != null) {
            CalendarItemMonth june = new CalendarItemMonth(Helper.getTranslation("june", null), 6, (int) jun.getCount());
            monthRow.addItem(june);
        } else {
            CalendarItemMonth june = new CalendarItemMonth(Helper.getTranslation("june", null), 6, 0);
            monthRow.addItem(june);
        }
        if (jul != null) {
            CalendarItemMonth july = new CalendarItemMonth(Helper.getTranslation("july", null), 7, (int) jul.getCount());
            monthRow.addItem(july);
        } else {
            CalendarItemMonth july = new CalendarItemMonth(Helper.getTranslation("july", null), 7, 0);
            monthRow.addItem(july);
        }
        if (aug != null) {
            CalendarItemMonth august = new CalendarItemMonth(Helper.getTranslation("august", null), 8, (int) aug.getCount());
            monthRow.addItem(august);
        } else {
            CalendarItemMonth august = new CalendarItemMonth(Helper.getTranslation("august", null), 8, 0);
            monthRow.addItem(august);
        }
        if (sep != null) {
            CalendarItemMonth september = new CalendarItemMonth(Helper.getTranslation("september", null), 9, (int) sep.getCount());
            monthRow.addItem(september);
        } else {
            CalendarItemMonth september = new CalendarItemMonth(Helper.getTranslation("september", null), 9, 0);
            monthRow.addItem(september);
        }
        if (oct != null) {
            CalendarItemMonth october = new CalendarItemMonth(Helper.getTranslation("october", null), 10, (int) oct.getCount());
            monthRow.addItem(october);
        } else {
            CalendarItemMonth october = new CalendarItemMonth(Helper.getTranslation("october", null), 10, 0);
            monthRow.addItem(october);
        }
        if (nov != null) {
            CalendarItemMonth november = new CalendarItemMonth(Helper.getTranslation("november", null), 11, (int) nov.getCount());
            monthRow.addItem(november);
        } else {
            CalendarItemMonth november = new CalendarItemMonth(Helper.getTranslation("november", null), 11, 0);
            monthRow.addItem(november);
        }
        if (dec != null) {
            CalendarItemMonth december = new CalendarItemMonth(Helper.getTranslation("december", null), 12, (int) dec.getCount());
            monthRow.addItem(december);
        } else {
            CalendarItemMonth december = new CalendarItemMonth(Helper.getTranslation("december", null), 12, 0);
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
            sbSearchString.append(SolrConstants._CALENDAR_MONTH).append(':').append(value).append(" AND ").append(SolrConstants.DC).append(':')
                    .append(collection).append('*').append(docstructFilterQuery);
        } else {
            sbSearchString.append(SolrConstants._CALENDAR_MONTH).append(':').append(value).append(docstructFilterQuery);
        }

        QueryResponse resp = SearchHelper.searchCalendar(sbSearchString.toString(), Collections.singletonList(SolrConstants._CALENDAR_DAY), 0, false);
        FacetField field = resp.getFacetField(SolrConstants._CALENDAR_DAY);
        List<Count> fieldValues = field.getValues() != null ? field.getValues() : new ArrayList<>();

        GregorianChronology calendar = GregorianChronology.getInstance();
        DateTimeField datefield = calendar.dayOfMonth();

        LocalDate date = new LocalDate(Integer.parseInt(value), currentMonth.getValue(), 1, calendar);

        int number = datefield.getMaximumValue(date);
        for (int day = 1; day <= number; day++) {
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
            date = new LocalDate(Integer.parseInt(value), currentMonth.getValue(), day, calendar);
            switch (date.getDayOfWeek()) {
                case 1:
                    dayItem.setDayOfWeek("Sunday");
                    break;
                case 2:
                    dayItem.setDayOfWeek("Monday");
                    break;
                case 3:
                    dayItem.setDayOfWeek("Tuesday");
                    break;
                case 4:
                    dayItem.setDayOfWeek("Wednsday");
                    break;
                case 5:
                    dayItem.setDayOfWeek("Thursday");
                    break;
                case 6:
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
        String facetName = builder.toString();
        return facetName;
    }

    /**
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
     * @param currentYear
     * @throws IndexUnreachableException
     * @throws PresentationException
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
     * @return the rowList
     */
    public List<CalendarRow> getRowList() {
        return rowList;
    }

    /**
     * @param rowList the rowList to set
     */
    public void setRowList(List<CalendarRow> rowList) {
        this.rowList = rowList;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    public CalendarRow getMonthRow() {
        return monthRow;
    }

    public void setMonthRow(CalendarRow monthRow) {
        this.monthRow = monthRow;
    }

    public CalendarItemMonth getCurrentMonth() {
        return currentMonth;
    }

    /**
     * Set a new value for month. If the selected month is the same as the old selection, the month and day will be unselected. Otherwise the data for
     * the month gets generated.
     *
     * @param currentMonth
     * @throws IndexUnreachableException
     * @throws PresentationException
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

    public CalendarItemDay getCurrentDay() {
        return currentDay;
    }

    public void setCurrentDay(CalendarItemDay currentDay) {
        if (this.currentDay == currentDay) {
            this.currentDay = null;
        } else {
            this.currentDay = currentDay;
        }
    }

    /**
     * 
     * @return
     */
    public Date getCurrentDate() {
        MutableDateTime date = new MutableDateTime();
        if (currentYear != null) {
            date.setYear(currentYear.getValue());
        }
        if (currentMonth != null) {
            date.setMonthOfYear(currentMonth.getValue());
        }
        if (currentDay != null) {
            date.setDayOfMonth(currentDay.getValue());
        }

        return date.toDate();
    }

    /**
     * @return the dayRow
     */
    public CalendarRow getDayRow() {
        return dayRow;
    }

    /**
     * @param dayRow the dayRow to set
     */
    public void setDayRow(CalendarRow dayRow) {
        this.dayRow = dayRow;
    }

    /**
     * This method generates the search string for the time line based search tab. The search string will be handed over to the search bean to execute
     * the search.
     *
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */

    public String searchTimeline() throws PresentationException, IndexUnreachableException, DAOException {
        StringBuilder builder = new StringBuilder();

        if (collection != null && !collection.isEmpty()) {
            builder.append(SolrConstants.DC).append(':').append(collection).append("* AND ");
        }
        if (currentDay != null) {
            builder.append(SolrConstants._CALENDAR_DAY).append(':');
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
            builder.append(SolrConstants._CALENDAR_MONTH).append(':');
            builder.append(currentYear.getName());
            if (currentMonth.getValue() < 10) {
                builder.append('0').append(currentMonth.getValue());
            } else {
                builder.append(currentMonth.getValue());
            }
        } else {
            builder.append(SolrConstants._CALENDAR_YEAR).append(':');
            builder.append(currentYear.getName());
        }

        builder.append(docstructFilterQuery);
        searchBean.resetSearchResults();
        searchBean.setCurrentPage(1);
        searchBean.getFacets().setCurrentHierarchicalFacetString("-");
        searchBean.mirrorAdvancedSearchCurrentHierarchicalFacets();
        searchBean.getFacets().setCurrentFacetString("-");
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
        try {
            populateYearData();
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here");
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here");
        }
    }

    /**
     * This method returns a list of all active years. <br />
     *
     * The method searches for the facet of the field 'YEAR'. If the count of a facet is greater than 0, the year is active.
     *
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */

    public List<CalendarItemYear> getAllActiveYears() throws PresentationException, IndexUnreachableException {
        if (allActiveYears == null) {
            allActiveYears = new ArrayList<>();
            List<String> fields = new ArrayList<>();
            fields.add(SolrConstants._CALENDAR_YEAR);
            fields.add(SolrConstants._CALENDAR_DAY);
            StringBuilder sbSearchString = new StringBuilder();
            //            sbSearchString.append("{!join from=PI_TOPSTRUCT to=PI}");
            if (collection != null && !collection.isEmpty()) {
                sbSearchString.append(SolrConstants._CALENDAR_DAY).append(":* AND ").append(SolrConstants.DC).append(':').append(collection).append(
                        '*').append(docstructFilterQuery);
            } else {
                sbSearchString.append(SolrConstants._CALENDAR_DAY).append(":*").append(docstructFilterQuery);
            }
            sbSearchString.append(SearchHelper.getAllSuffixes(DataManager.getInstance().getConfiguration().isSubthemeAddFilterQuery()));

            logger.trace("getAllActiveYears query: {}", sbSearchString.toString());
            QueryResponse resp = SearchHelper.searchCalendar(sbSearchString.toString(), fields, 1, false);

            FacetField facetFieldDay = resp.getFacetField(SolrConstants._CALENDAR_DAY);
            List<Count> dayCounts = facetFieldDay.getValues() != null ? facetFieldDay.getValues() : new ArrayList<>();
            Map<Integer, Integer> yearCountMap = new HashMap<>();
            for (Count day : dayCounts) {
                int year = Integer.valueOf(day.getName().substring(0, 4));
                Integer count = yearCountMap.get(year);
                if (count == null) {
                    count = 0;
                }
                yearCountMap.put(year, (int) (count + day.getCount()));
            }

            List<Integer> years = new ArrayList<>(yearCountMap.keySet());
            Collections.sort(years);
            for (int year : years) {
                CalendarItemYear item = new CalendarItemYear(String.valueOf(year), year, yearCountMap.get(year));
                allActiveYears.add(item);
            }
        }

        return allActiveYears;
    }

    /**
     * 
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public void resetYears() throws PresentationException, IndexUnreachableException {
        logger.trace("resetYears");
        allActiveYears = null;
        getAllActiveYears();
        populateYearData();
    }

    // calendar view

    /**
     * Select a year. If the new value differs from the old one, the data for the new value gets generated.
     *
     * @param selectYear the selectYear to set
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public void setSelectYear(String selectYear) throws PresentationException, IndexUnreachableException {
        logger.trace("setSelectYear: {}", selectYear);
        if (this.selectYear == null || !this.selectYear.equals(selectYear)) {
            this.selectYear = selectYear;
            currentDay = null;
            currentMonth = null;
            monthList = populateMonthsWithDays(selectYear, collection, docstructFilterQuery);
            // Set currentYear so that the number of hits is available for comparison
            for (CalendarItemYear year : getAllActiveYears()) {
                if (year.getName().equals(selectYear)) {
                    setCurrentYear(year);
                }
            }
        }
    }

    /**
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
     * @throws IndexUnreachableException
     * @throws PresentationException
     *
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
        facetFields.add(SolrConstants._CALENDAR_DAY);
        facetFields.add(SolrConstants._CALENDAR_MONTH);

        StringBuilder sbSearchString = new StringBuilder();
        if (collection != null && !collection.isEmpty()) {
            sbSearchString.append(SolrConstants._CALENDAR_YEAR).append(':').append(ClientUtils.escapeQueryChars(selectYear)).append(" AND ").append(
                    SolrConstants.DC).append(':').append(collection).append('*').append(filterQuery);
        } else {
            sbSearchString.append(SolrConstants._CALENDAR_YEAR).append(':').append(ClientUtils.escapeQueryChars(selectYear)).append(filterQuery);
        }

        resp = SearchHelper.searchCalendar(sbSearchString.toString(), facetFields, 0, false);

        List<Count> dayFacets = resp.getFacetField(SolrConstants._CALENDAR_DAY) != null ? resp.getFacetField(SolrConstants._CALENDAR_DAY).getValues()
                : new ArrayList<>();
        List<Count> monthFacets = resp.getFacetField(SolrConstants._CALENDAR_MONTH).getValues() != null ? resp.getFacetField(
                SolrConstants._CALENDAR_MONTH).getValues() : new ArrayList<>();

        GregorianChronology calendar = GregorianChronology.getInstance();
        DateTimeField datefield = calendar.dayOfMonth();

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
            if (monthCount.getName().equals(selectYear + "01")) {
                jan.setHits((int) monthCount.getCount());
            } else if (monthCount.getName().equals(selectYear + "02")) {
                feb.setHits((int) monthCount.getCount());
            } else if (monthCount.getName().equals(selectYear + "03")) {
                mar.setHits((int) monthCount.getCount());
            } else if (monthCount.getName().equals(selectYear + "04")) {
                apr.setHits((int) monthCount.getCount());
            } else if (monthCount.getName().equals(selectYear + "05")) {
                may.setHits((int) monthCount.getCount());
            } else if (monthCount.getName().equals(selectYear + "06")) {
                jun.setHits((int) monthCount.getCount());
            } else if (monthCount.getName().equals(selectYear + "07")) {
                jul.setHits((int) monthCount.getCount());
            } else if (monthCount.getName().equals(selectYear + "08")) {
                aug.setHits((int) monthCount.getCount());
            } else if (monthCount.getName().equals(selectYear + "09")) {
                sep.setHits((int) monthCount.getCount());
            } else if (monthCount.getName().equals(selectYear + "10")) {
                oct.setHits((int) monthCount.getCount());
            } else if (monthCount.getName().equals(selectYear + "11")) {
                nov.setHits((int) monthCount.getCount());
            } else if (monthCount.getName().equals(selectYear + "12")) {
                dec.setHits((int) monthCount.getCount());
            }
        }

        for (CalendarItemMonth monthItem : monthList) {
            List<CalendarItemWeek> weeksOfMonth = new ArrayList<>();
            CalendarItemWeek currentWeek = new CalendarItemWeek("", 0, 0);
            weeksOfMonth.add(currentWeek);
            monthItem.setWeeksOfMonth(weeksOfMonth);
            LocalDate date = new LocalDate(Integer.parseInt(selectYear), monthItem.getValue(), 1, calendar);
            int number = datefield.getMaximumValue(date);
            for (int day = 1; day <= number; day++) {
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
                        String query = new StringBuilder().append(SolrConstants._CALENDAR_DAY).append(':').append(selectYear).append(monthItem
                                .getFormattedValue()).append(dayItem.getFormattedValue()) + filterQuery;
                        dayItem.setQuery(query);
                        break;
                    }
                }
                if (dayItem == null) {
                    dayItem = new CalendarItemDay(String.valueOf(day), day, 0);
                }
                date = new LocalDate(Integer.parseInt(selectYear), monthItem.getValue(), day, calendar);
                switch (date.getDayOfWeek()) {
                    case DateTimeConstants.MONDAY:
                        dayItem.setDayOfWeek("Monday");
                        if (!currentWeek.getDaysOfWeek().isEmpty()) {
                            currentWeek = new CalendarItemWeek("", 0, 0);
                            weeksOfMonth.add(currentWeek);
                        }
                        break;
                    case DateTimeConstants.TUESDAY:
                        dayItem.setDayOfWeek("Tuesday");
                        break;
                    case DateTimeConstants.WEDNESDAY:
                        dayItem.setDayOfWeek("Wednesday");
                        break;
                    case DateTimeConstants.THURSDAY:
                        dayItem.setDayOfWeek("Thursday");
                        break;
                    case DateTimeConstants.FRIDAY:
                        dayItem.setDayOfWeek("Friday");
                        break;
                    case DateTimeConstants.SATURDAY:
                        dayItem.setDayOfWeek("Saturday");
                        break;
                    case DateTimeConstants.SUNDAY:
                        dayItem.setDayOfWeek("Sunday");
                        break;
                    default:
                        dayItem.setDayOfWeek("unknown");
                        break;
                }

                if (date.getDayOfMonth() == 1 && date.getDayOfWeek() != DateTimeConstants.MONDAY) {
                    // fill first week with empty day items
                    switch (date.getDayOfWeek()) {
                        case 7:
                            // Sunday
                            CalendarItemDay sun = new CalendarItemDay("", 0, 0);
                            currentWeek.addDay(sun);
                        case 6:
                            // Saturday
                            CalendarItemDay sat = new CalendarItemDay("", 0, 0);
                            currentWeek.addDay(sat);
                        case 5:
                            // Friday
                            CalendarItemDay fri = new CalendarItemDay("", 0, 0);
                            currentWeek.addDay(fri);
                        case 4:
                            // Thursday
                            CalendarItemDay thu = new CalendarItemDay("", 0, 0);
                            currentWeek.addDay(thu);
                        case 3:
                            // Wednesday
                            CalendarItemDay wed = new CalendarItemDay("", 0, 0);
                            currentWeek.addDay(wed);
                        case 2:
                            // Tuesday
                            CalendarItemDay tue = new CalendarItemDay("", 0, 0);
                            currentWeek.addDay(tue);
                            break;
                    }
                }
                currentWeek.addDay(dayItem);
            }
        }

        return monthList;
    }

    /**
     * @return the monthList
     */
    public List<CalendarItemMonth> getMonthList() {
        return monthList;
    }

    /**
     * @return the yearEnd
     */
    public int getYearEnd() {
        return yearEnd;
    }

    /**
     * @return the yearStart
     */
    public int getYearStart() {
        return yearStart;
    }

    /**
     * @param yearEnd the yearEnd to set
     */
    public void setYearEnd(int yearEnd) {
        this.yearEnd = yearEnd;
    }

    /**
     * @param yearStart the yearStart to set
     */
    public void setYearStart(int yearStart) {
        this.yearStart = yearStart;
    }

    /**
     * @param collection the collection to set
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public void setCollection(String collection) throws PresentationException, IndexUnreachableException {
        if (this.collection == null || !this.collection.equals(collection)) {
            this.collection = collection;
            populateMonthsWithDays(selectYear, collection, docstructFilterQuery);
        }
    }

    /**
     * @return the collection
     */
    public String getCollection() {
        return collection;
    }

    public String limitSearch() throws IndexUnreachableException, PresentationException {
        populateYearData();
        return "";
    }

    /**
     * 
     * @param month
     * @param day
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public String searchCalendar(CalendarItemMonth month, CalendarItemDay day) throws PresentationException, IndexUnreachableException, DAOException {
        currentMonth = month;
        currentDay = day;
        return searchCalendar();
    }

    /**
     * This method generates the search string for the calendar search tab. The search string will be handed over to the search bean to execute the
     * search.
     *
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */

    public String searchCalendar() throws PresentationException, IndexUnreachableException, DAOException {
        if (StringUtils.isEmpty(selectYear)) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        //        if (collection != null && !collection.isEmpty()) {
        //            builder.append(SolrConstants.DC).append(':').append(collection).append("* AND ");
        //        }
        if (currentDay != null) {
            builder.append(SolrConstants._CALENDAR_DAY).append(':');
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
            builder.append(SolrConstants._CALENDAR_MONTH).append(':');
            builder.append(selectYear);
            if (currentMonth.getValue() < 10) {
                builder.append('0').append(currentMonth.getValue());
            } else {
                builder.append(currentMonth.getValue());
            }
        } else {
            builder.append(SolrConstants._CALENDAR_YEAR).append(':');
            builder.append(selectYear);
        }

        // builder.append(docstructFilterQuery);
        searchBean.resetSearchResults();
        searchBean.setCurrentPage(1);
        searchBean.getFacets().setCurrentHierarchicalFacetString("-");
        searchBean.mirrorAdvancedSearchCurrentHierarchicalFacets();
        searchBean.setExactSearchString(builder.toString());
        searchBean.executeSearch();
        return "pretty:searchCalendar5";
    }

    /**
     * This method generates a search string to search for data with a value in YEAR but without a value in YEARMONTHDAY.
     *
     * @param date
     * @return
     */

    private String getQueryForIncompleteData(String date) {
        StringBuilder searchString = new StringBuilder();
        if (collection != null && !collection.isEmpty()) {
            searchString.append(SolrConstants.DC).append(':').append(collection).append("* AND ");

        }
        searchString.append(SolrConstants._CALENDAR_YEAR).append(':').append(date);
        searchString.append(" AND -").append(SolrConstants._CALENDAR_DAY).append(":*");
        searchString.append(docstructFilterQuery);
        return searchString.toString();
    }

    /**
     * This method returns the count of all data without a value in YEARMONTHDAY for the selected year.
     *
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public int getCountOfIncompleteData() throws PresentationException, IndexUnreachableException {
        String year = getActualYear();
        if (year == null) {
            return 0;
        }
        String query = getQueryForIncompleteData(year);
        QueryResponse resp = SearchHelper.searchCalendar(query, Collections.singletonList(SolrConstants._CALENDAR_YEAR), 0, false);
        FacetField field = resp.getFacetField(SolrConstants._CALENDAR_YEAR);
        List<Count> fieldValues = field.getValues() != null ? field.getValues() : new ArrayList<>();
        for (Count count : fieldValues) {
            if (count.getName().equals(year)) {
                return (int) count.getCount();
            }
        }

        return 0;
    }

    public String getActualYear() {
        String year = "";
        if (searchBean.getActiveSearchType() == 2) {
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
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */

    public String searchForIncompleteData() throws PresentationException, IndexUnreachableException, DAOException {
        String year = "";
        if (searchBean.getActiveSearchType() == 2) {
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
        searchBean.getFacets().setCurrentHierarchicalFacetString("-");
        searchBean.mirrorAdvancedSearchCurrentHierarchicalFacets();
        searchBean.setExactSearchString(getQueryForIncompleteData(year));
        searchBean.executeSearch();
        return "pretty:newSearch5";
    }

}
