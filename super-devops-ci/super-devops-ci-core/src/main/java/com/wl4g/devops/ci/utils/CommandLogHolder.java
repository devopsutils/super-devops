/*
 * Copyright 2017 ~ 2025 the original author or authors. <wanglsir@gmail.com, 983708408@qq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wl4g.devops.ci.utils;

import static java.lang.ThreadLocal.withInitial;
import static java.util.Collections.synchronizedMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.util.Assert.hasText;
import static org.springframework.util.Assert.notNull;
import static org.springframework.util.Assert.state;

import java.util.HashMap;
import java.util.Map;

import com.google.common.annotations.Beta;

/**
 * Command logs holders.
 * 
 * @author Wangl.sir <wanglsir@gmail.com, 983708408@qq.com>
 * @version v1.0 2019年10月23日
 * @since
 */
@Beta
public abstract class CommandLogHolder {

	/** Current logs cache. */
	final private static ThreadLocal<Map<String, LogAppender>> logCache = withInitial(() -> synchronizedMap(new HashMap<>(8)));

	/**
	 * Append log message to current buffer cache.
	 * 
	 * @param key
	 * @param message
	 * @return
	 */
	public static StringBuffer logAdd(String key, String message) {
		return getLogAppender(key).getMessage().append(message);
	}

	/**
	 * Get message buffer cache by key.
	 * 
	 * @param key
	 * @return
	 */
	public static String getCleanup(String key) {
		String text = null;
		LogAppender appender = getLogAppender(key);
		StringBuffer buffer = appender.getMessage();
		if (nonNull(buffer)) {
			text = buffer.toString();
			buffer.setLength(0);
		}
		return text;
	}

	/**
	 * Cleanup multiple buffer cache by key.
	 * 
	 * @param key
	 */
	public static void cleanup(String key) {
		LogAppender appender = getLogAppender(key);
		appender.getMessage().setLength(0);
		logCache.get().remove(key);
	}

	/**
	 * Cleanup multiple buffer cache all.
	 */
	public static void cleanup() {
		logCache.get().clear();
	}

	/**
	 * Get or create log appender.
	 * 
	 * @param key
	 * @return
	 */
	public static LogAppender getLogAppender(Number key) {
		notNull(key, "Log appender key must not be null.");
		return getLogAppender(String.valueOf(key));
	}

	/**
	 * Get or create log appender.
	 * 
	 * @param key
	 * @return
	 */
	public static LogAppender getLogAppender(String key) {
		hasText(key, "Log appender key must not be empty.");
		Map<String, LogAppender> cache = logCache.get();
		LogAppender appender = cache.get(key);
		if (isNull(appender)) {
			state(isNull(cache.putIfAbsent(key, (appender = new LogAppender(key)))),
					String.format("Already log appender with key: %s", key));
		}
		return appender;
	}

	/**
	 * Log appender.
	 * 
	 * @author Wangl.sir <wanglsir@gmail.com, 983708408@qq.com>
	 * @version v1.0 2019年10月23日
	 * @since
	 */
	public static class LogAppender {

		/** Log appender key-name. */
		final private String key;

		/** Log appended message. */
		final private StringBuffer message = new StringBuffer(64);

		public LogAppender(String key) {
			hasText(key, "Log appender key must not be empty.");
			this.key = key;
		}

		public String getKey() {
			return key;
		}

		public StringBuffer getMessage() {
			return message;
		}

		/**
		 * Append log message to current buffer cache.
		 * 
		 * @param message
		 */
		public void logAdd(String message) {
			CommandLogHolder.logAdd(key, message);
		}

		/**
		 * Get message buffer cache by key.
		 * 
		 * @param key
		 * @return
		 */
		public String getCleanup(String key) {
			return CommandLogHolder.getCleanup(key);
		}

	}

	public static void main(String[] args) {
		LogAppender appender1 = CommandLogHolder.getLogAppender("test1");
		appender1.logAdd("asasdfasdf");
		appender1.logAdd("6734665347");
		LogAppender appender2 = CommandLogHolder.getLogAppender("test2");
		appender2.logAdd("2rerwqsadfa");
		System.out.println(appender1.getCleanup("test1"));
		System.out.println(appender2.getCleanup("test2"));
	}

}