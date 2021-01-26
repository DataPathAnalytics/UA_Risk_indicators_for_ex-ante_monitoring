create or replace function work_days_count(start_date timestamp, end_date timestamp)
  returns int as $$
declare
  result int;
begin
  with all_dates as (SELECT date_trunc('day', dd) :: date as date
                     FROM generate_series(start_date + interval '1' day, end_date, '1 day' :: interval) dd)
  select count(date)
  from all_dates
  where extract(dow from date) not in (0, 6) into result;

  result = result +
           (select count(*) from weekend_on where date between start_date + interval '1' day and end_date) -
           (select count(*) from workday_off where date between start_date + interval '1' day and end_date);
  return result;
end;
$$
language plpgsql;