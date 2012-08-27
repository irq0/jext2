/*
 * Copyright (c) 2011 Marcel Lauhoff.
 * 
 * This file is part of jext2.
 * 
 * jext2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * jext2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with jext2.  If not, see <http://www.gnu.org/licenses/>.
 */

package jext2.exceptions;

import java.util.logging.Level;
import java.util.logging.Logger;

import jext2.Filesystem;

public class JExt2Exception extends Exception {
	protected static final int ERRNO = -1;

	private static final long serialVersionUID = -7429088074385678308L;

	Logger logger = Filesystem.getLogger();

	public JExt2Exception() {
		log("");
	}

	public JExt2Exception(String msg) {
		log(msg);
	}

	private void log(String msg) {
		if (logger.isLoggable(Level.FINE)) {
			StackTraceElement[] stack = getStackTrace();

			StringBuilder log = new StringBuilder();
			log.append("JEXT2 exception was raised: ");
			log.append(this.getClass().getSimpleName());
			log.append(" source=");
			log.append(stack[0].getClassName());
			log.append("->");
			log.append(stack[0].getMethodName());
			log.append(" msg=");
			log.append(msg);
			log.append(" errno=");
			log.append(getErrno());

			logger.fine(log.toString());
		}
	}

	public int getErrno() {
		throw new RuntimeException("This method should not be executed. " +
				"- The errno value defined here is meaningless");
	}
}
