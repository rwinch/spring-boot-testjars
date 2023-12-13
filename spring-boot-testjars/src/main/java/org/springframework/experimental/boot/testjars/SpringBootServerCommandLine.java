/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.experimental.boot.testjars;

import org.apache.commons.exec.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpringBootServerCommandLine extends CommandLine {
	private final File applicationPortFile;

	public SpringBootServerCommandLine(String command, File applicationPortFile) {
		super(command);
		this.applicationPortFile = applicationPortFile;
	}

	public File getApplicationPortFile() {
		return applicationPortFile;
	}

	public static SpringBootServerCommandLineBuilder builder() {
		return new SpringBootServerCommandLineBuilder();
	}

	public static class SpringBootServerCommandLineBuilder {
		private String executable = currentJavaExecutable();

		private List<String> classpath = new ArrayList<>();

		private Map<String, String> systemProperties = new HashMap<>();

		private String mainClass = "org.springframework.boot.loader.JarLauncher";

		private File applicationPortFile = createApplicationPortFile();

		private static File createApplicationPortFile() {
			try {
				return File.createTempFile("application-", ".port");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public SpringBootServerCommandLineBuilder addClasspathEntries(String... classpathEntries) {
			addAll(this.classpath, classpathEntries);
			return this;
		}

		public SpringBootServerCommandLineBuilder addSystemProperties(Map<String, String> systemProperties) {
			this.systemProperties.putAll(systemProperties);
			return this;
		}

		public SpringBootServerCommandLine build() {
			SpringBootServerCommandLine commandLine = new SpringBootServerCommandLine(this.executable, this.applicationPortFile);
			commandLine.addArguments(createSystemPropertyArgs(), false);
			commandLine.addArgument("-classpath", false);
			commandLine.addArguments(createClasspathArgValue(), false);
			commandLine.addArgument(this.mainClass);
			return commandLine;
		}

		private String[] createSystemPropertyArgs() {
			Map<String, String> systemPropertyArgs = new HashMap<>(this.systemProperties);
			systemPropertyArgs.put("PORTFILE", this.applicationPortFile.getAbsolutePath());
			systemPropertyArgs.put("server.port", "0");
			return systemPropertyArgs.entrySet().stream()
					.map(e -> "-D" + e.getKey() + "=" + e.getValue() + "")
					.toArray(String[]::new);
		}

		private String createClasspathArgValue() {
			return this.classpath.stream()
					.collect(Collectors.joining(File.pathSeparator));
		}

		private static void addAll(List<String> list, String... entries) {
			for (String classpathEntry : entries) {
				list.add(classpathEntry);
			}
		}

		private static String currentJavaExecutable() {
			ProcessHandle processHandle = ProcessHandle.current();
			return processHandle.info().command().get();
		}
	}
}