Release notes

#### Version 2.3.1

Added new handler for 1-11 indicator. 

Added gitignore 

Added  classes for working with auctions and bid-lots

#### Version 2.3.0

Added the logic to process phone numbers due to incorrectly used a phone number field. Some users add more than 1000 characters.

Updated auction bid lot amount data loading (added 404 http error processing)

Updated agreement data loading (added empty data processing)

Added multiple contracts to award supporting. Updated indicators which use a contract and award for calculating (1-15, 1-16, 1-17, 1-18, 2-1-2)

#### Version 2.2.0

Distributed indicators by time. 

Disabled deprecated indicators. 

Added new indicators cron properties according new names

#### Version 2.1.1

Ignore priceQuotation tenders

Moved cpv calculation after filtration test tenders

Added tender id to exception message when we can't calculate parent cpv

#### Version 2.1.0

Added new configuration property `prozorro.agreements.url`
Changed 1-3 and 1-11 indicators to handle tenders with absent fields for calculations

#### Version 2.0.1

Removed queue calculations at start up

#### Version 2.0.0

Added new indicators

Added new properties
`risk-common.cron`, `prozorro.requests.url`

#### Version 1.4.2

Changed supplier telephone columns from varchar to text type

#### Version 1.4.1

Moved calculating transaction variables after tender validation

#### Version 1.4.0

Changed an algorithm to indicate tender cpv parent

Ignore handling tenders where title contains '`Тестування`'

Changed risk procedure priority

Added new rule to exclude tenders from queue

Changed tenders ranking by risk level in procedure priority

Changed a formula to get exchange rate in indicator handlers  

Changed nightly recalculation interval from 1 year to 1 month

Ignored tenders with procurementMethodType equal `priceQuotation`

Handling new procuring entity kinds

To migrate to new version add new keys to `application.properties`

`queue.ignore.contract-statuses=terminated`

`queue.ignore.method-type=reporting,belowThreshold `

#### Version 1.3.0

Removed email reporting part on failed loading after logging was reworked within past release.

#### Version 1.2.0

Fixed issue for application stopping while loosing connectivity to Prozorro API. 

Setup 404 http code when requested tender or contract not found in a system

#### Version 1.1.3

Updated queue functionality to include value in risk score ranking

Added new configuration property

`queue.amount-based-tender-risk-score`

To enable value of the tender to be involved in risk score ranking, set the following

`queue.amount-based-tender-risk-score=true`

If `queue.amount-based-tender-risk-score=false`, risk score ranking would work with tender risk score data

#### Version 1.1.2

Implemented functionality to disable skipping test tenders from Prozorro.

Based on new configuration property

`prozorro.tenders.skip-test`
 
if false - application will save all tenders including test (mode equals test)

#### Version 1.1.1

Added new configuration properties for loading data from external resources

`nbu.exchange.url` 

Getting exchange rates from National Bank Of Ukraine

`prozorro.weekends-on.url`

Getting list of working weekends 

`prozorro.workdays-off.url`

Getting list of days off

`prozorro.monitorings.url`

Getting tender monitoring

`prozorro.tenders.url`

Getting Prozorro tenders

`prozorro.contracts.url`

Getting Prozorro contracts

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

