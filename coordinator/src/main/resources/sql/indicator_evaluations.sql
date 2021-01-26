INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (1,
        'Переговорна процедура ініційована на підставі торгів, які були відмінені за межами періоду в один рік (законом не визначено часовий проміжок).',
        'RISK1-1')
ON CONFLICT DO NOTHING;
INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (2, 'Процедура ініційована напідставі відмінених торгів за лотом.', 'RISK1-1')
ON CONFLICT DO NOTHING;

INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (3, 'Хибне значення індикатору через помилково обрану валюту закупівлі.', 'RISK1-2_1')
ON CONFLICT DO NOTHING;
INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (4,
        'Індикатор відображає хибне значення через те, що сприймає предмет закупівлі як товар чи послугу а замовник купує "роботи" (немає визначення робіт, товарів чи послуг законом) чи навпаки.',
        'RISK1-2_1')
ON CONFLICT DO NOTHING;

INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (5, 'Хибне значення індикатору через помилково обрану валюту закупівлі.', 'RISK1-2_2')
ON CONFLICT DO NOTHING;
INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (6,
        'Індикатор відображає хибне значення через те, що сприймає предмет закупівлі як товар чи послугу а замовник купує "роботи" (немає визначення робіт, товарів чи послуг законом) чи навпаки.',
        'RISK1-2_2')
ON CONFLICT DO NOTHING;

INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (7, 'Замовник наклав кваліфікований електронний підпис.', 'RISK1-3_1')
ON CONFLICT DO NOTHING;
INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (8, 'Замовник наклав електронну печатку замість кваліфікованого електронного підпису.', 'RISK1-3_1')
ON CONFLICT DO NOTHING;

INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (9, 'Замовник наклав кваліфікований електронний підпис.', 'RISK1-3_2')
ON CONFLICT DO NOTHING;
INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (10, 'Замовник наклав електронну печатку замість кваліфікованого електронного підпису.', 'RISK1-3_2')
ON CONFLICT DO NOTHING;

INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (11, 'Замовник наклав кваліфікований електронний підпис.', 'RISK1-3_3')
ON CONFLICT DO NOTHING;
INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (12, 'Замовник наклав електронну печатку замість кваліфікованого електронного підпису.', 'RISK1-3_3')
ON CONFLICT DO NOTHING;

INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (13, 'Індикатор відображає хибне значення через не враховані святкові дні.', 'RISK1-4_1')
ON CONFLICT DO NOTHING;
INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (14,
        'Індикатор відображає хибне значення через публікацію протоколу пізніше, ніж за 5 робочих днів (законом не встановлені часові обмеження для публікації протоколу).',
        'RISK1-4_1')
ON CONFLICT DO NOTHING;

INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (15,
        'Індикатор відображає хибне значення через те, що сприймає предмет закупівлі як товар чи послугу а замовник купує "роботи" (немає визначення робіт, товарів чи послуг законом) чи навпаки.',
        'RISK1-5_2')
ON CONFLICT DO NOTHING;

INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (16, 'Хибне значення індикатора, так як усі файли договору зашифровані за допомогою електронного підпису.',
        'RISK1-6')
ON CONFLICT DO NOTHING;

INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (17,
        'Хибне значення індикатора, замовник завантажив інші документи а не документи договру  - порушення відсутне так як завантажені документи не є договором.',
        'RISK1-8_1')
ON CONFLICT DO NOTHING;

INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (18, 'Хибне значення індикатору через помилково обрану валюту закупівлі.', 'RISK1-10_1')
ON CONFLICT DO NOTHING;
INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (19,
        'Індикатор відображає хибне значення через те, що сприймає предмет закупівлі як товар чи послугу а замовник купує "роботи" (немає визначення робіт, товарів чи послуг законом) чи навпаки.',
        'RISK1-10_1')
ON CONFLICT DO NOTHING;

INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (20, 'Хибне значення індикатору через помилково обрану валюту закупівлі.', 'RISK1-10_2')
ON CONFLICT DO NOTHING;

INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (21, 'Хибне значення індикатору через помилково обрану валюту закупівлі.', 'RISK1-10_3')
ON CONFLICT DO NOTHING;
INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (22,
        'Індикатор відображає хибне значення через те, що сприймає предмет закупівлі як товар чи послугу а замовник купує "роботи" (немає визначення робіт, товарів чи послуг законом) чи навпаки.',
        'RISK1-10_3')
ON CONFLICT DO NOTHING;

INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (23, 'Хибне значення індикатору через помилково обрану валюту закупівлі.', 'RISK1-10_4')
ON CONFLICT DO NOTHING;

INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (24, 'Хибне значення індикатору через помилково обрану валюту закупівлі.', 'RISK2-5')
ON CONFLICT DO NOTHING;
INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (25,
        'Індикатор відображає хибне значення через те, що сприймає предмет закупівлі як товар чи послугу а замовник купує "роботи" (немає визначення робіт, товарів чи послуг законом) чи навпаки.',
        'RISK2-5')
ON CONFLICT DO NOTHING;

INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (26, 'Хибне значення індикатору через помилково обрану валюту закупівлі.', 'RISK2-5_1')
ON CONFLICT DO NOTHING;
INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (27,
        'Індикатор відображає хибне значення через те, що сприймає предмет закупівлі як товар чи послугу а замовник купує "роботи" (немає визначення робіт, товарів чи послуг законом) чи навпаки.',
        'RISK2-5_1')
ON CONFLICT DO NOTHING;

INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (28, 'Хибне значення індикатору через помилково обрану валюту закупівлі.', 'RISK2-5_2')
ON CONFLICT DO NOTHING;
INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (29,
        'Індикатор відображає хибне значення через те, що сприймає предмет закупівлі як товар чи послугу а замовник купує "роботи" (немає визначення робіт, товарів чи послуг законом) чи навпаки.',
        'RISK2-5_2')
ON CONFLICT DO NOTHING;

INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (30, 'Хибне значення індикатору через помилково обрану валюту закупівлі.', 'RISK2-5_3')
ON CONFLICT DO NOTHING;
INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (31,
        'Індикатор відображає хибне значення через те, що сприймає предмет закупівлі як товар чи послугу а замовник купує "роботи" (немає визначення робіт, товарів чи послуг законом) чи навпаки.',
        'RISK2-5_3')
ON CONFLICT DO NOTHING;

INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (32, 'Хибне значення індикатору через помилково обрану валюту закупівлі.', 'RISK2-6')
ON CONFLICT DO NOTHING;

INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (33, 'Хибне значення індикатору через помилково обрану валюту закупівлі.', 'RISK2-6_1')
ON CONFLICT DO NOTHING;

INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (34, 'Хибне значення індикатору через помилково обрану валюту закупівлі.', 'RISK2-6_2')
ON CONFLICT DO NOTHING;

INSERT INTO indicator_evaluations(id, evaluation, indicator_id)
VALUES (35, 'Хибне значення індикатору через помилково обрану валюту закупівлі.', 'RISK2-6_3')
ON CONFLICT DO NOTHING;