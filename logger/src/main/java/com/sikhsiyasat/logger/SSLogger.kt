package com.sikhsiyasat.logger

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by ranjeetsingh on 17/10/17.
 */
class SSLogger private constructor(tag: String) {
    private val log: Logger = LoggerFactory.getLogger(tag + BuildConfig.VERSION_NAME)
    fun info(message: String?) {
        log.info(message)
    }

    fun info(format: String?, vararg args: Any?) {
        log.info(format, *args)
    }

    fun debug(message: String?) {
        log.debug(message)
    }

    fun error(message: String?) {
        log.error(message)
    }

    fun error(format: String?, vararg data: Any?) {
        log.error(String.format(format!!, *data))
    }

    fun error(msg: String?, throwable: Throwable?) {
        log.error(msg, throwable)
    }

    companion object {
        @JvmStatic
        fun getLogger(tag: String): SSLogger {
            return SSLogger(tag)
        }
    }

}