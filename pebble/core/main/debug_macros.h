#pragma once

/* DEBUGLOG symbol
 *
 * Write messages to the pebble console, if defined,
 * otherwise logging is disabled.
 */

/*
#define DEBUGLOG
*/
 
#ifdef DEBUGLOG

#define APP_LOG_DEBUG(fmt, args...) APP_LOG(APP_LOG_LEVEL_DEBUG, fmt, ##args)

#else

#define APP_LOG_DEBUG(fmt, args...)

#endif

