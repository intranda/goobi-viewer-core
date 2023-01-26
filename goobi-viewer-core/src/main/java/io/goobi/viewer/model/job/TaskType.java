package io.goobi.viewer.model.job;

public enum TaskType {
    /** Send emails to all search owners if their searches have changed results */
    NOTIFY_SEARCH_UPDATE,
    /** Remove expired born digital content download tickets from the DB */
    PURGE_EXPIRED_DOWNLOAD_TICKETS,
    /** Handle asynchronous generation of excel sheets with search results */
    SEARCH_EXCEL_EXPORT,
    /** Update the application sitemap */
    UPDATE_SITEMAP,
    /** Update data repository names of a record */
    UPDATE_DATA_REPOSITORY_NAMES,
    /** Update uploaded processes status. */
    UPDATE_UPLOAD_JOBS,
    /** Move daily usage statistics to SOLR */
    INDEX_USAGE_STATISTICS,
    /**Create a pdf for a record or part of record to be offered as download**/
    DOWNLOAD_PDF,
    /**Create single page pdfs for a record to be used when creating a record pdf**/
    CREATE_PAGE_PDFS;
}
