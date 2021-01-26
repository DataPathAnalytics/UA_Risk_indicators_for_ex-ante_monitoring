CREATE OR REPLACE FUNCTION get_tender_amount_uah(_id INTEGER)
    RETURNS DOUBLE PRECISION
AS
$$

DECLARE
    _tender RECORD;
    _rate   RECORD;
BEGIN

    SELECT date_created, amount, currency FROM tender WHERE id = _id INTO _tender;
    SELECT * FROM exchange_rate WHERE date::DATE = _tender.date_created::DATE AND code = _tender.currency INTO _rate;

    IF _tender.currency = 'UAH' THEN
        RETURN _tender.amount;
    ELSE
        IF _rate IS NULL THEN
            RAISE EXCEPTION 'Can not find rate by date % and currency %' , _tender.date_created, _tender.currency;
        END IF;
        RETURN _tender.amount * _rate.rate;
    END IF;

END;
$$ LANGUAGE plpgsql;