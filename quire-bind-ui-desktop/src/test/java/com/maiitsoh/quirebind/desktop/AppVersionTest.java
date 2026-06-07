/*
 * Copyright 2025 QuireBind Contributors
 *
 * This file is part of QuireBind.
 *
 * QuireBind is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QuireBind is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with QuireBind.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.maiitsoh.quirebind.desktop;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that the version string can be read from the filtered properties
 * file using the absolute classpath path used by MainController.appVersion().
 * Relative paths with ".." are not normalised inside JARs, so this test
 * catches that class of regression before the app is packaged.
 */
class AppVersionTest {

    @Test
    void versionResourceIsPresent() throws IOException {
        try (InputStream in = AppVersionTest.class.getResourceAsStream(
                "/com/maiitsoh/quirebind/desktop/application.properties")) {
            assertNotNull(in, "application.properties not found on classpath — "
                + "check Maven resource filtering and the absolute path used in "
                + "MainController.appVersion()");
            Properties props = new Properties();
            props.load(in);
            String version = props.getProperty("version");
            assertNotNull(version, "version key missing from application.properties");
            assertNotEquals("", version.trim(), "version is empty");
            assertNotEquals("${project.version}", version,
                "Maven placeholder was not replaced — resource filtering may be disabled");
        }
    }
}
