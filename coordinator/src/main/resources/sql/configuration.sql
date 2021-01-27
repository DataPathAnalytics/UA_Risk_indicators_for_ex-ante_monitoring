INSERT INTO configuration(key, value)
VALUES ('tenderScore', '0.5')
ON CONFLICT DO NOTHING;

INSERT INTO configuration(key, value)
VALUES ('expectedValue', '0.5')
ON CONFLICT DO NOTHING;

INSERT INTO configuration(key, value)
VALUES ('bucketRiskGroupMediumLeft', '0.5')
ON CONFLICT DO NOTHING;

INSERT INTO configuration(key, value)
VALUES ('bucketRiskGroupMediumRight', '1.1')
ON CONFLICT DO NOTHING;

INSERT INTO configuration(key, value)
VALUES ('tendersCompletedDays', '30')
ON CONFLICT (key) DO UPDATE SET value = '30';
