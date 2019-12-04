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
package org.sonarsource.scanner.api.internal.cache;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import javax.annotation.CheckForNull;

/**
 * This class is responsible for managing Sonar batch file cache. You can put file into cache and
 * later try to retrieve them. MD5 is used to differentiate files (name is not secure as files may come
 * from different Sonar servers and have same name but be actually different, and same for SNAPSHOTs).
 */
public class FileCache {

  private final Path dir;
  private final Path tmpDir;
  private final FileHashes hashes;
  private final Logger logger;

  FileCache(Path dir, FileHashes fileHashes, Logger logger) {
    this.hashes = fileHashes;
    this.logger = logger;
    this.dir = createDir(dir, "user cache: ");
    logger.info(String.format("User cache: %s", dir.toString()));
    this.tmpDir = createDir(dir.resolve("_tmp"), "temp dir");
  }

  static FileCache create(Path dir, Logger logger) {
    return new FileCache(dir, new FileHashes(), logger);
  }

  public File getDir() {
    return dir.toFile();
  }

  /**
   * Look for a file in the cache by its filename and md5 checksum. If the file is not
   * present then return null.
   */
  @CheckForNull
  public File get(String filename, String hash) {
    Path cachedFile = dir.resolve(hash).resolve(filename);
    if (Files.exists(cachedFile)) {
      return cachedFile.toFile();
    }
    logger.debug(String.format("No file found in the cache with name %s and hash %s", filename, hash));
    return null;
  }

  @FunctionalInterface
  public interface Downloader {
    void download(String filename, File toFile) throws IOException;
  }

  public File get(String filename, String hash, Map<String, String> props) {
    // Does not fail if another process tries to create the directory at the same time.
    Path hashDir = hashDir(hash);
    Path targetFile = hashDir.resolve(filename);
    if (!Files.exists(targetFile)) {
      String filePath = props.get("sonar.jarDir") + File.separator  + filename;
      File file = new File(filePath);
      String realHash = hashes.of(file);
      if (!hash.equals(realHash)) {
        throw new IllegalStateException("INVALID HASH: File " + filePath + " was expected to have hash " + hash
          + " but was exist with hash " + realHash);
      }
      mkdirQuietly(hashDir);
      try {
        Files.copy(file.toPath(), targetFile);
      } catch (IOException e) {
        throw new IllegalStateException("Fail to copy " + filePath + " to " + targetFile.toString(), e);
      }
    }
    return targetFile.toFile();
  }

  private void renameQuietly(Path sourceFile, Path targetFile) {
    try {
      Files.move(sourceFile, targetFile, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException ex) {
      logger.warn(String.format("Unable to rename %s to %s", sourceFile.toAbsolutePath(), targetFile.toAbsolutePath()));
      logger.warn("A copy/delete will be tempted but with no guarantee of atomicity");
      try {
        Files.move(sourceFile, targetFile);
      } catch (IOException e) {
        throw new IllegalStateException("Fail to move " + sourceFile.toAbsolutePath() + " to " + targetFile, e);
      }
    } catch (FileAlreadyExistsException e) {
      // File was probably cached by another process in the mean time
    } catch (IOException e) {
      throw new IllegalStateException("Fail to move " + sourceFile.toAbsolutePath() + " to " + targetFile, e);
    }
  }

  private Path hashDir(String hash) {
    return dir.resolve(hash);
  }

  private static void mkdirQuietly(Path hashDir) {
    try {
      Files.createDirectories(hashDir);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create cache directory: " + hashDir, e);
    }
  }

  private Path newTempFile() {
    try {
      return Files.createTempFile(tmpDir, "fileCache", null);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create temp file in " + tmpDir, e);
    }
  }

  private Path createDir(Path dir, String debugTitle) {
    logger.debug("Create: " + dir.toString());
    try {
      Files.createDirectories(dir);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create " + debugTitle + dir.toString(), e);
    }
    return dir;
  }
}
