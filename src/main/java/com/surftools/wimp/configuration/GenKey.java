/**

The MIT License (MIT)

Copyright (c) 2019, Robert Tykulsker

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


*/

package com.surftools.wimp.configuration;

import com.surftools.wimp.utils.config.IConfigurationKey;

/**
 * the @{IConfigurationKey} for the exercise generator
 *
 * @author bobt
 *
 */
public enum GenKey implements IConfigurationKey {
	PATH_ROOT("path.root"), // dir where reference dirs/files are
	PATH_REFERENCE("path.reference"), // dir where reference dirs/files are
	PATH_REFERENCE_HISTORY("path.reference.history"), // path to all generated references
	PATH_RESOURCES("path.resources"), // path to resources dir

	PATH_SCHEDULE("path.schedule"), // path to the expanded schedule.csv; output
	PATH_META_SCHEDULE("path.metaSchedule"), // path to the meta-schedule.xlsx; input

	PATH_COMMIT_MESSAGE("path.commit.message"), // unique content to generate/publish
	PATH_EMAIL_PASSWORD("path.email.password"), // password for email provider

	PATH_PUBLICATION("path.publication"), // list of paths to remote folder for publishing results
	PATH_ARCHIVE("path.archive"), // list of paths to remote folder for archiving entire exercise

	GENERATOR_RNG_SEED("generator.rngSeed"), // to get consistent results
	GENERATOR_N_YEARS("generator.nYears"), // number of years to generate
	GENERATOR_INSTRUCTION_URL("generator.instruction.url"), // url at the bottom of each instruction
	GENERATOR_START_DATE("generator.start.date"), // date to start generating instructions
	GENERATOR_LEGACY_DATE("generator.legacy.date"), // date, before which, we copy from referency-legacy

	EMAIL_NOTIFICATION_FROM("email.notification.from"), //
	EMAIL_NOTIFICATION_TO("email.notification.to"), // comma-delimited list
	;

	private final String key;

	private GenKey(String key) {
		this.key = key;
	}

	public static GenKey fromString(String string) {
		for (GenKey key : GenKey.values()) {
			if (key.toString().equals(string)) {
				return key;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return key;
	}
}