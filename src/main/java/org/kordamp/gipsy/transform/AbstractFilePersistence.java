/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2013-2019 Andres Almiray
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kordamp.gipsy.transform;

import org.kordamp.jipsy.processor.AbstractPersistence;
import org.kordamp.jipsy.processor.Initializer;
import org.kordamp.jipsy.processor.LogLocation;
import org.kordamp.jipsy.processor.Logger;

import java.io.*;
import java.net.URI;

/**
 * @author Andres Almiray
 */
public abstract class AbstractFilePersistence extends AbstractPersistence {
    private static final String DEFAULT_TARGET_DIR = "org.kordamp.gipsy.DEFAULT_TARGET_DIR";

    protected final File outputDir;

    public AbstractFilePersistence(File outputDir, String name, Logger logger, String path) {
        super(name, logger, path);
        if (outputDir == null) {
            String defaultPath = System.getProperty(DEFAULT_TARGET_DIR);
            if (defaultPath != null && defaultPath.trim().length() > 0) {
                try {
                    this.outputDir = new File(defaultPath);
                    this.outputDir.mkdirs();
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            } else {
                try {
                    this.outputDir = File.createTempFile(new File(path).getName(), "");
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        } else {
            this.outputDir = outputDir;
        }
    }

    @Override
    public Initializer getInitializer() {
        return new DefaultFileInitializer(outputDir, path, logger);
    }

    @Override
    public File determineOutputLocation() {
        File resource;
        try {
            resource = getResourceFile("locator");
        } catch (FileNotFoundException e) {
            // Could happen
            return null;
        } catch (IOException e) {
            logger.note(LogLocation.MESSAGER, "IOException while determining output location: " + e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            // Happens when the path is invalid. For instance absolute or relative to a path
            // not part of the class output folder.
            //
            // Due to a bug in javac for Linux, this also occurs when no output path is specified
            // for javac using the -d parameter.
            // See http://forums.sun.com/thread.jspa?threadID=5240999&tstart=45
            // and http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6647996

            // logger.toConsole("IllegalArgumentException: " + e.getMessage());
            return null;
        }

        URI uri = resource.toURI();
        if (uri.isAbsolute()) {
            return new File(uri).getParentFile();
        }
        return new File(uri.toString()).getParentFile();
    }

    protected File getResourceFile(String name) throws IOException {
        return new File(outputDir.getAbsolutePath() + File.separator + path + name);
    }

    @Override
    public void delete() throws IOException {
        new File(outputDir.getAbsolutePath() + File.separator + path + name).delete();
    }

    @Override
    protected Writer createWriter(String name) throws IOException {
        return new BufferedWriter(new FileWriter(getResourceFile(name)));
    }
}
