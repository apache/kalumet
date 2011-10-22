/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.kalumet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.vfs.*;
import org.apache.kalumet.model.Application;
import org.apache.kalumet.model.Environment;
import org.apache.kalumet.model.Software;
import org.apache.oro.text.regex.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

/**
 * Virtual file system (VFS) wrapper to perform various actions on files or directories (local or remote).
 */
public class FileManipulator {

    private final static transient Logger LOGGER = LoggerFactory.getLogger(FileManipulator.class);

    private static final String BASE_DIR = SystemUtils.USER_DIR;
    private static final String WORKING_DIR = "work";
    private static final String PROTOCOL_REGEX = "(.+):(.+)";

    private static final String JAR_EXTENSION = ".jar";
    private static final String ZIP_EXTENSION = ".zip";
    private static final String TGZ_EXTENSION = ".tgz";
    private static final String TARGZ_EXTENSION = ".tar.gz";
    private static final String TBZ2_EXTENSION = ".tbz2";

    private static final String JAR_PROTOCOL = "jar:";
    private static final String ZIP_PROTOCOL = "zip:";
    private static final String TGZ_PROTOCOL = "tgz:";
    private static final String TARGZ_PROTOCOL = "tgz:";
    private static final String TBZ2_PROTOCOL = "tbz2:";

    private static FileManipulator _singleton = null;
    private FileSystemManager fileSystemManager;

    /**
     * Private constructor to init the singleton.
     *
     * @throws FileManipulatorException in <code>FileManipulator</code> init failed.
     */
    private FileManipulator() throws FileManipulatorException {
        try {
            LOGGER.debug("Creating VFS file system manager ...");
            this.fileSystemManager = VFS.getManager();
            // this.fileSystemManager = new StandardFileSystemManager();
            // fileSystemManager.setCacheStrategy(CacheStrategy.ON_CALL);
            // fileSystemManager.setReplicator(new KalumetFileReplicator());
            // fileSystemManager.init();
        } catch (Exception e) {
            throw new FileManipulatorException(e);
        }
    }

    /**
     * Get a single instance of the <code>FileManipulator</code>.
     *
     * @return the <code>FileManipulator</code> instance.
     * @throws FileManipulatorException in case of init failure.
     */
    public static FileManipulator getInstance() throws FileManipulatorException {
        try {
            if (_singleton == null) {
                _singleton = new FileManipulator();
                LOGGER.debug("File manipulator initialized");
            }
            return _singleton;
        } catch (Exception e) {
            LOGGER.error("File manipulator initialization failure", e);
            throw new FileManipulatorException("File manipulator initialization failure", e);
        }
    }

    /**
     * Get the current basedir path.
     *
     * @return the path of the local basedir directory.
     * @throws FileManipulatorException
     */
    public static String getBaseDir() throws FileManipulatorException {
        try {
            File baseDir = new File(FileManipulator.BASE_DIR);
            return baseDir.getPath();
        } catch (Exception e) {
            LOGGER.error("Can't get basedir", e);
            throw new FileManipulatorException("Can't get basedir", e);
        }
    }

    /**
     * Resolve VFS path to add filename regex selector.
     *
     * @param vfsPath the file to resolve (can look like /tmp/folder/file*).
     * @return the resolved file object.
     * @throws FileSystemException  if the VFS file object can't be resolved.
     * @throws FileManipulatorException if the regex is not valid (regex is allowed only on files, not directories).
     */
    public FileObject resolveFile(String vfsPath) throws FileSystemException, FileManipulatorException {
        LOGGER.debug("Resolve VFS path {}", vfsPath);
        LOGGER.debug("Check if the file name regex selector is required");
        if ((vfsPath.indexOf("/") == -1) || (vfsPath.indexOf("*") == -1)) {
            LOGGER.debug("Regex select is not required for {}", vfsPath);
            return fileSystemManager.resolveFile(vfsPath);
        }
        LOGGER.debug("Isolating the path end");
        LOGGER.debug("Finding the last index of / separator");
        int separatorIndex = vfsPath.lastIndexOf('/');
        int tokenIndex = vfsPath.lastIndexOf('*');
        if (tokenIndex < separatorIndex) {
            LOGGER.error("Wildcard * is only supported on the file name, not on directories");
            throw new FileManipulatorException("Wildcard * is only supported on the file name, not on directories");
        }
        String pattern = vfsPath.substring(separatorIndex + 1);
        LOGGER.debug("{} pattern found", pattern);
        String baseName = vfsPath.substring(0, separatorIndex + 1);
        LOGGER.debug("Getting the base name {}", baseName);
        LOGGER.debug("Looking for the file (first found is returned)");
        FileObject baseUrl = fileSystemManager.resolveFile(baseName);
        FileObject[] fileObjects = baseUrl.findFiles(new FileNameRegexSelector(pattern));
        if (fileObjects.length < 1) {
            LOGGER.error("No file matching {} found on {}", pattern, baseName);
            throw new FileManipulatorException("No file matching " + pattern + " found on " + baseName);
        }
        return fileObjects[0];
    }

    /**
     * Check if the given VFS path is available (exists).
     *
     * @param vfsPath the VFS path
     * @return true if the VFS path exists, false else
     */
    public boolean pathAvailable(String vfsPath) {
        FileObject fileObject = null;
        try {
            fileObject = this.resolveFile(vfsPath);
            return fileObject.exists();
        } catch (Exception e) {
            LOGGER.warn("Can't check if the VFS path {} exists", vfsPath, e);
            return false;
        } finally {
            if (fileObject != null) {
                try {
                    fileObject.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Compare the content of two files.
     *
     * @param src the source file to compare.
     * @param dest the destination file to compare with.
     * @return true if the two files have exactly the same content, false else.
     * @throws FileManipulatorException if the compare failed.
     */
    public boolean contentEquals(String src, String dest) throws FileManipulatorException {
        FileObject srcFile = null;
        FileObject destFile = null;
        try {
            LOGGER.debug("Comparing the content of {} and {}", src, dest);
            srcFile = this.resolveFile(src);
            destFile = this.resolveFile(dest);
            if (!srcFile.exists() || !destFile.exists()) {
                LOGGER.debug("{} or {} don't exist", src, dest);
                return false;
            }
            if (!srcFile.getType().equals(FileType.FILE)) {
                LOGGER.error("The source {} is not a file", src);
                throw new IllegalArgumentException("The source URI " + src + " is not a file");
            }
            if (!destFile.getType().equals(FileType.FILE)) {
                LOGGER.error("The destination {} is not a file", dest);
                throw new IllegalArgumentException("The destination URI " + dest + " is not a file");
            }
            return IOUtils.contentEquals(srcFile.getContent().getInputStream(), destFile.getContent().getInputStream());
        } catch (Exception e) {
            LOGGER.error("Can't compare content of {} and {}", new Object[]{ src, dest }, e);
            throw new FileManipulatorException("Can't compare content of " + src + " and " + dest, e);
        } finally {
            if (srcFile != null) {
                try {
                    srcFile.close();
                } catch (Exception e) {
                    // ignore
                }
            }
            if (destFile != null) {
                try {
                    destFile.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Compare checksum of two files.
     *
     * @param src the source file.
     * @param dest the destination file.
     * @return true if the two files are the same (same signature), false else.
     * @throws FileManipulatorException
     */
    public boolean checksumEquals(String src, String dest) throws FileManipulatorException {
        FileObject srcFile = null;
        FileObject destFile = null;
        try {
            if (!srcFile.exists()) {
                LOGGER.error("Source {} doesn't exist", src);
                throw new FileManipulatorException("Source " + src + " doesn't exist");
            }
            if (destFile.exists() && destFile.getType().equals(FileType.FOLDER)) {
                destFile = this.resolveFile(dest + "/" + srcFile.getName().getBaseName());
            }
            if (!destFile.exists()) {
                return false;
            }
            if (!srcFile.getType().equals(FileType.FILE)) {
                LOGGER.error("Source {} is not a file", src);
                throw new FileManipulatorException("Source " + src + " is not a file");
            }
            if (!destFile.getType().equals(FileType.FILE)) {
                LOGGER.error("Destination {} is not a file", dest);
                throw new FileManipulatorException("Destination " + dest + " is not a file");
            }
            LOGGER.debug("Create the message digest");
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            LOGGER.debug("Generate the checksum for the source");
            DigestInputStream srcStream = new DigestInputStream(srcFile.getContent().getInputStream(), messageDigest);
            byte[] srcBuffer = new byte[8192];
            while (srcStream.read(srcBuffer) != -1) ;
            byte[] srcMd5 = messageDigest.digest();
            // reset the message digest
            messageDigest.reset();
            LOGGER.debug("Generate the checksum for the destination");
            DigestInputStream destStream = new DigestInputStream(destFile.getContent().getInputStream(), messageDigest);
            byte[] destBuffer = new byte[8192];
            while (destStream.read(destBuffer) != -1) ;
            byte[] destMd5 = messageDigest.digest();
            LOGGER.debug("Compare the checksum");
            return MessageDigest.isEqual(srcMd5, destMd5);
        } catch (Exception e) {
            LOGGER.error("Can't compare checksum of {} and {}", new Object[]{ src, dest }, e);
            throw new FileManipulatorException("Can't compare checksum of " + src + " and " + dest, e);
        } finally {
            if (srcFile != null) {
                try {
                    srcFile.close();
                } catch (Exception e) {
                    // ignore
                }
            }
            if (destFile != null) {
                try {
                    destFile.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Copy files.
     *
     * @param src the source VFS path.
     * @param dest the destination VFS path.
     * @throws FileManipulatorException in case of copy failure.
     */
    public void copy(String src, String dest) throws FileManipulatorException {
        FileObject srcFile = null;
        FileObject destFile = null;
        try {
            srcFile = this.resolveFile(src);
            destFile = this.resolveFile(dest);
            if (srcFile.getType().equals(FileType.FOLDER)) {
                LOGGER.debug("Source {} is a folder", src);
                if (!destFile.exists()) {
                    LOGGER.debug("Destination folder {} doesn't exist, create it", dest);
                    destFile.createFolder();
                }
                if (!destFile.getType().equals(FileType.FOLDER)) {
                    LOGGER.error("Destination {} must be a folder", dest);
                    throw new IllegalArgumentException("Destination " + dest + " must be a folder");
                }
                LOGGER.debug("Copy source folder {} to {} using SELECT_ALL selector", src, dest);
                destFile.copyFrom(srcFile, Selectors.SELECT_ALL);
            } else {
                LOGGER.debug("Source {} is a file");
                if (destFile.exists() && destFile.getType().equals(FileType.FOLDER)) {
                    destFile = this.resolveFile(dest + "/" + srcFile.getName().getBaseName());
                }
                destFile.copyFrom(srcFile, Selectors.SELECT_SELF);
            }
        } catch (Exception e) {
            LOGGER.error("Can't copy from {} to {}", new Object[]{ src, dest }, e);
            throw new FileManipulatorException("Can't copy from " + src + " to " + dest, e);
        } finally {
            if (srcFile != null) {
                try {
                    srcFile.close();
                } catch (Exception e) {
                    // ignore
                }
            }
            if (destFile != null) {
                try {
                    destFile.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Check if a given path is a directory.
     *
     * @param vfsPath the VFS path to check.
     * @return true if the path is a folder, false else.
     */
    public boolean isFolder(String vfsPath) {
        FileObject file = null;
        try {
            file = this.resolveFile(vfsPath);
            return (file.getType().equals(FileType.FOLDER));
        } catch (Exception e) {
            LOGGER.warn("Can't check if {} is a folder", vfsPath, e);
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        return false;
    }

    /**
     * Browse (Return the children) of a give VFS path
     *
     * @param vfsPath the VFS path.
     * @return the children array.
     */
    public FileObject[] browse(String vfsPath) {
        FileObject file = null;
        try {
            file = this.resolveFile(vfsPath);
            if (!file.getType().equals(FileType.FOLDER)) {
                throw new IllegalArgumentException("{} is not a directory");
            }
            return file.getChildren();
        } catch (Exception e) {
            LOGGER.warn("Can't get {} children", vfsPath, e);
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        return null;
    }

    /**
     * Read a VFS path and return the input stream content.
     *
     * @param vfsPath the VFS path.
     * @return the input stream content.
     * @throws FileManipulatorException in case of read failure.
     */
    public InputStream read(String vfsPath) throws FileManipulatorException {
        FileObject file = null;
        try {
            file = this.resolveFile(vfsPath);
            if (!file.exists() || !file.getType().equals(FileType.FILE)) {
                LOGGER.error("{} doesn't exist or is not a file");
                throw new IllegalArgumentException(vfsPath + " doesn't exist or is not a file");
            }
            return file.getContent().getInputStream();
        } catch (Exception e) {
            LOGGER.error("Can't read {}", vfsPath, e);
            throw new FileManipulatorException("Can't read " + vfsPath, e);
        }
    }

    /**
     * Get the VFS path output stream to write into.
     *
     * @param vfsPath the VFS path.
     * @return the output stream.
     * @throws FileManipulatorException in case of writing failure.
     */
    public OutputStream write(String vfsPath) throws FileManipulatorException {
        FileObject file = null;
        try {
            file = this.resolveFile(vfsPath);
            if (file.exists() && !file.getType().equals(FileType.FILE)) {
                LOGGER.error("{} is not a file", vfsPath);
                throw new IllegalArgumentException(vfsPath + " is not a file");
            }
            return file.getContent().getOutputStream();
        } catch (Exception e) {
            LOGGER.error("Can't write {}", vfsPath, e);
            throw new FileManipulatorException("Can't write " + vfsPath, e);
        }
    }

    /**
     * Creates the environment cache directory.
     *
     * @param environment the <code>Environment</code>.
     * @return the environment cache directory path.
     * @throws FileManipulatorException in case of creation failure.
     */
    public static String createEnvironmentCacheDir(Environment environment) throws FileManipulatorException {
        String directory = FileManipulator.getBaseDir() + "/" + FileManipulator.WORKING_DIR + "/" + environment.getName();
        FileManipulator fileManipulator = FileManipulator.getInstance();
        fileManipulator.createDirectory(directory);
        return directory;
    }

    /**
     * Creates an environment application cache directory.
     *
     * @param environment the <code>Environment</code>.
     * @param application the <code>Application</code>.
     * @return the environment application cache directory path.
     * @throws FileManipulatorException in case of creation failure.
     */
    public static String createEnvironmentApplicationCacheDir(Environment environment, Application application) throws FileManipulatorException {
        String directory = FileManipulator.createEnvironmentCacheDir(environment);
        directory = directory + "/applications/" + application.getName();
        FileManipulator fileManipulator = FileManipulator.getInstance();
        fileManipulator.createDirectory(directory);
        return directory;
    }

    /**
     * Creates an environment software cache directory.
     *
     * @param environment the <code>Environment</code>.
     * @param software the <code>Software</code>.
     * @return the environment software cache directory path.
     * @throws FileManipulatorException in case of creation failure.
     */
    public static String createEnvironmentSoftwareCacheDir(Environment environment, Software software) throws FileManipulatorException {
        String directory = FileManipulator.createEnvironmentCacheDir(environment);
        directory = directory + "/softwares/" + software.getName();
        FileManipulator fileManipulator = FileManipulator.getInstance();
        fileManipulator.createDirectory(directory);
        return directory;
    }

    /**
     * Creates a directory.
     *
     * @param path the directory path to create.
     * @throws FileManipulatorException in case of creation failure.
     */
    public void createDirectory(String path) throws FileManipulatorException {
        FileObject directory = null;
        try {
            directory = this.resolveFile(path);
            directory.createFolder();
        } catch (Exception e) {
            LOGGER.error("Can't create directory {}", path, e);
            throw new FileManipulatorException("Can't create directory " + path, e);
        } finally {
            if (directory != null) {
                try {
                    directory.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Check if the given VFS path begins with a protocol (file:, http:, ...).
     *
     * @param path the VFS path to check.
     * @return true
     */
    public static boolean protocolExists(String path) {
        // make a regex on the path
        LOGGER.debug("Looking for protocol in {}", path);
        PatternMatcher matcher = new Perl5Matcher();
        PatternCompiler compiler = new Perl5Compiler();
        Pattern pattern = null;
        try {
            pattern = compiler.compile(FileManipulator.PROTOCOL_REGEX);
        } catch (MalformedPatternException malformedPatternException) {
            LOGGER.warn("URL protocol check failed", malformedPatternException);
            return false;
        }
        PatternMatcherInput input = new PatternMatcherInput(path);
        if (matcher.contains(input, pattern)) {
            LOGGER.debug("{} matches the protocol regex");
            return true;
        }
        LOGGER.debug("{} doesn't match the protocol regex");
        return false;
    }

    /**
     * Format an URL to a VFS compliant URL (finding the protocol corresponding to the extension).
     *
     * @param url source URL
     * @return the VFS formatted URL.
     */
    public static String format(String url) {
        String formattedUrl = url.trim();
        if (formattedUrl.endsWith(JAR_EXTENSION) && !formattedUrl.startsWith(JAR_PROTOCOL)) {
            return JAR_PROTOCOL + formattedUrl;
        }
        if (formattedUrl.endsWith(ZIP_EXTENSION) && !formattedUrl.startsWith(ZIP_PROTOCOL)) {
            return ZIP_PROTOCOL + formattedUrl;
        }
        if (formattedUrl.endsWith(TGZ_EXTENSION) && !formattedUrl.startsWith(TGZ_PROTOCOL)) {
            return TGZ_PROTOCOL + formattedUrl;
        }
        if (formattedUrl.endsWith(TARGZ_EXTENSION) && !formattedUrl.startsWith(TARGZ_PROTOCOL)) {
            return TARGZ_PROTOCOL + formattedUrl;
        }
        if (formattedUrl.endsWith(TBZ2_EXTENSION) && !formattedUrl.startsWith(TBZ2_PROTOCOL)) {
            return TBZ2_PROTOCOL + formattedUrl;
        }
        return formattedUrl;
    }

    /**
     * Delete a VFS path.
     *
     * @param path the VFS path.
     * @throws FileManipulatorException in case of deletion failure.
     */
    public void delete(String path) throws FileManipulatorException {
        FileObject file = null;
        try {
            file = this.resolveFile(path);
            file.delete(Selectors.SELECT_ALL);
        } catch (Exception e) {
            LOGGER.error("Can't delete {}", path, e);
            throw new FileManipulatorException("Can't delete " + path, e);
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Search and replace a regex in a given VFS path.
     *
     * @param regex the regexp to search.
     * @param substitute the replacement string.
     * @param path the VFS path where to search and replace.
     */
    public static void searchAndReplace(String path, String regex, String substitute) {
        try {
            String content = FileUtils.readFileToString(new File(path), null);
            content = StringUtils.replace(content, regex, substitute);
            FileUtils.writeStringToFile(new File(path), content, null);
        } catch (IOException ioException) {
            LOGGER.warn("Can't replace {} with {} in {}", new Object[]{ regex, substitute, path }, ioException);
        }
    }

}
