BEGIN;

DROP TABLE IF EXISTS logging_event;
DROP TABLE IF EXISTS logging_event_exception;

CREATE TABLE logging_event (
	event_id          BIGINT PRIMARY KEY,
	app_name          VARCHAR(254),
    timestmp          BIGINT,
    formatted_message TEXT,
    logger_name       VARCHAR(254),
    level_string      VARCHAR(254),
    thread_name       VARCHAR(254),
    reference_flag    SMALLINT,
    arg0              VARCHAR(254),
    arg1              VARCHAR(254),
    arg2              VARCHAR(254),
    arg3              VARCHAR(254),
    caller_filename   VARCHAR(254),
    caller_class      VARCHAR(254),
    caller_method     VARCHAR(254),
    caller_line       CHAR(4)
);


CREATE TABLE logging_event_exception (
    event_id         BIGINT,
    i                SMALLINT,
    trace_line       TEXT,
    PRIMARY KEY(event_id, i)
);

COMMIT;