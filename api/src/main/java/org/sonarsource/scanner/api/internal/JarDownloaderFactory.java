/*
 * SonarQube Scanner API
 * Copyright (C) 2011-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.scanner.api.internal;

import javax.annotation.Nullable;
import org.sonarsource.scanner.api.internal.JarDownloader.ScannerFileDownloader;
import org.sonarsource.scanner.api.internal.cache.FileCache;
import org.sonarsource.scanner.api.internal.cache.FileCacheBuilder;
import org.sonarsource.scanner.api.internal.cache.Logger;

import java.util.Map;

class JarDownloaderFactory {
  private final Logger logger;
  private final String userHome;
  private final Map<String, String> props;

  JarDownloaderFactory(Logger logger, @Nullable String userHome, Map<String, String> props) {
    this.logger = logger;
    this.userHome = userHome;
    this.props = props;
  }

  JarDownloader create() {
    FileCache fileCache = new FileCacheBuilder(logger)
      .setUserHome(userHome)
      .build();
    BootstrapIndexDownloader bootstrapIndexDownloader = new BootstrapIndexDownloader(logger);
    JarExtractor jarExtractor = new JarExtractor();
    return new JarDownloader(bootstrapIndexDownloader, fileCache, jarExtractor, logger, props);
  }
}
