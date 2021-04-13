INSERT INTO feedback_indicator_response(id, value)
VALUES (1, 'Порушень в закупівлі згідно з інформацією індикатора не виявлено')
ON CONFLICT DO NOTHING;

INSERT INTO feedback_indicator_response(id, value)
VALUES (2, 'Виявлені порушення в закупівлі згідно з інформацією індикатора')
ON CONFLICT DO NOTHING;

INSERT INTO feedback_indicator_response(id, value)
VALUES (3, 'Виявлені порушення опосередковано пов''язані з інформацією індикатора')
ON CONFLICT DO NOTHING;

INSERT INTO feedback_indicator_response(id, value)
VALUES (4, 'Інше (деталі в коментарі)')
ON CONFLICT DO NOTHING;