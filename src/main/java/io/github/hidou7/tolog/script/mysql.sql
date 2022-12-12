BEGIN;

DROP TABLE IF EXISTS logging_event_property;
DROP TABLE IF EXISTS logging_event_exception;
DROP TABLE IF EXISTS logging_event;

CREATE TABLE logging_event (
	event_id          VARCHAR(40) NOT NULL PRIMARY KEY,
	app_name          VARCHAR(254),
    timestmp         BIGINT NOT NULL,
    formatted_message  TEXT NOT NULL,
    logger_name       VARCHAR(254) NOT NULL,
    level_string      VARCHAR(254) NOT NULL,
    thread_name       VARCHAR(254),
    reference_flag    SMALLINT,
    arg0              VARCHAR(254),
    arg1              VARCHAR(254),
    arg2              VARCHAR(254),
    arg3              VARCHAR(254),
    caller_filename   VARCHAR(254) NOT NULL,
    caller_class      VARCHAR(254) NOT NULL,
    caller_method     VARCHAR(254) NOT NULL,
    caller_line       CHAR(4) NOT NULL
);


CREATE TABLE logging_event_exception(
    event_id         VARCHAR(40) NOT NULL,
    i                SMALLINT NOT NULL,
    trace_line       TEXT NOT NULL,
    PRIMARY KEY(event_id, i)
);

CREATE TABLE logging_event_property (
    event_id	      VARCHAR(40) NOT NULL,
    mapped_key        VARCHAR(254) NOT NULL,
    mapped_value      TEXT,
    PRIMARY KEY(event_id, mapped_key)
);

COMMIT;