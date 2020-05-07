Release notes

#### Version 1.1.0

Changed log level for different parts of application	
- Trace log level for validation and existence modules
- Removed debug logs for tenders that were updated but their indicator value didn't change

Optimized error handling for tenders batch, for all indicators.

Re-wrote logic for saving indicator calculations (process has been accelerated twice)

Fixed loading data from Prozorro, when server always provides non-empty list of data (on the last page the data is non empty and one tender is returned all the time)
- Tender loading module
- Contracts loading module

Changes to druid
- Changed granularity in ingestion schema
- Using compaction tasks to merge small segments into one bigger
- Added shell script to run compaction from command line
- Druid version has been updated to 0.16.1
- Updated requests for the new druid version. 


Increased pool size
- For running scheduled tasks
- For tasks for calculating indicators that running asynchronous

Small fixes
- Removed 2 instances of RestTemplate and configure primary with connection timeout and read timeout
- Removed currently unused methods.
- Added admin functionality to configure Queue API
- Added workdays repository to config.
- Configured indicators schedule to calculate in batches, to reduce the server load
- Added general cron parameter, if all indicators should be running together


New application properties

`risk-common.cron=0 /30 * * * *`

`weekends-on.url=https://prozorroukr.github.io/standards/calendars/weekends_on.json`

`workdays-off.url=https://prozorroukr.github.io/standards/calendars/workdays_off.json`



DB:

Setting up queue parameters

table `indicators_queue_configuration`

Field `tendersCompletedDays` - updating amount of days to leave completed procedures in queue 

field `low_top_risk_percentage`

field `medium_top_risk_percentage`

field `high_top_risk_percentage`

field `procuring_entity_percentages`

Setup Indicator Impact Value

table `indicator`

field `impact`

