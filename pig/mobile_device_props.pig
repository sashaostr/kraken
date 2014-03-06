REGISTER 'geoip-1.2.9-patch-2-SNAPSHOT.jar'
REGISTER 'kraken-generic-0.0.2-SNAPSHOT-jar-with-dependencies.jar'
REGISTER 'kraken-dclass-0.0.2-SNAPSHOT.jar'
REGISTER 'kraken-pig-0.0.2-SNAPSHOT.jar'

-- Script Parameters: pass via -p param_name=param_value, ex: -p date_bucket_regex=2013-03-24_00
%default date_bucket_format 'yyyy-MM-dd_HH';    -- Format applied to timestamps for aggregation into buckets. Default: hourly.
%default date_bucket_regex '.*';                -- Regex used to filter the formatted date_buckets; must match whole line. Default: no filtering.

DEFINE DATE_BUCKET  org.wikimedia.analytics.kraken.pig.ConvertDateFormat('yyyy-MM-dd\'T\'HH:mm:ss', '$date_bucket_format');
DEFINE DCLASS       org.wikimedia.analytics.kraken.pig.UserAgentClassifier();
DEFINE IS_PAGEVIEW  org.wikimedia.analytics.kraken.pig.PageViewFilterFunc();

IMPORT 'include/load_webrequest.pig'; -- See include/load_webrequest.pig
log_fields = LOAD_WEBREQUEST('$input');

log_fields = FILTER log_fields
    BY (    (DATE_BUCKET(timestamp) MATCHES '$date_bucket_regex')
        AND IS_PAGEVIEW(uri, referer, user_agent, http_status, remote_addr, content_type, request_method)
    );

device_info_with_non_wmf = FOREACH log_fields
    GENERATE
        DATE_BUCKET(timestamp)      AS date_bucket:chararray,
        FLATTEN(DCLASS(user_agent)) AS (
            vendor:chararray,
            model:chararray,
            device_os:chararray,
            device_os_version:chararray,
            device_class:chararray,
            browser:chararray,
            browser_version:chararray,
            wmf_mobile_app:chararray,
            has_javascript:boolean,
            display_dimensions:chararray,
            input_device:chararray,
            non_wmf_mobile_app:chararray
        );

device_info = FOREACH device_info_with_non_wmf
    GENERATE
        date_bucket,
        vendor,
        model,
        device_os,
        device_os_version,
        device_class,
        browser,
        browser_version,
        wmf_mobile_app,
        has_javascript,
        display_dimensions,
        input_device
    ;

device_info_count = FOREACH (GROUP device_info BY (date_bucket, device_class, device_os))
    GENERATE FLATTEN($0), COUNT($1) AS num:int;
device_info_count = ORDER device_info_count BY date_bucket, device_class, device_os;

STORE device_info_count INTO '$output' USING PigStorage();
