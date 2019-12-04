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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonarsource.scanner.api.internal.BootstrapIndexDownloader.JarEntry;
import org.sonarsource.scanner.api.internal.cache.FileCache;
import org.sonarsource.scanner.api.internal.cache.Logger;

import static java.lang.String.format;

class JarDownloader {
  private final FileCache fileCache;
  private final JarExtractor jarExtractor;
  private final Logger logger;
  private final BootstrapIndexDownloader bootstrapIndexDownloader;
  private final Map<String, String> props;

  JarDownloader(BootstrapIndexDownloader bootstrapIndexDownloader, FileCache fileCache, JarExtractor jarExtractor, Logger logger,Map<String, String> props) {
    this.bootstrapIndexDownloader = bootstrapIndexDownloader;
    this.logger = logger;
    this.fileCache = fileCache;
    this.jarExtractor = jarExtractor;
    this.props = props;
  }

  List<File> download() {
    List<File> files = new ArrayList<>();
    logger.debug("Extract sonar-scanner-api-batch in temp...");
    files.add(jarExtractor.extractToTemp("sonar-scanner-api-batch").toFile());
    files.addAll(getScannerEngineFiles());
    return files;
  }

  private List<File> getScannerEngineFiles() {
    Collection<JarEntry> index = bootstrapIndexDownloader.getIndex();
    return index.stream()
      .map(jar -> fileCache.get(jar.getFilename(), jar.getHash(), props))
      .collect(Collectors.toList());
  }

  static class ScannerFileDownloader implements FileCache.Downloader {
    private final ServerConnection connection;

    ScannerFileDownloader(ServerConnection conn) {
      this.connection = conn;
    }

    @Override
    public void download(String filename, File toFile) throws IOException {
      connection.downloadFile(format("/batch/file?name=%s", filename), toFile.toPath());
    }
  }
}
