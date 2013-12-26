/*
 * Copyright 2010-2014 the original author or authors.
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

package org.kordamp.gipsy.transform;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.kordamp.jipsy.processor.Initializer;
import org.kordamp.jipsy.processor.LogLocation;
import org.kordamp.jipsy.processor.Logger;

import java.io.*;

/**
 * @author Andres Almiray
 */
public abstract class AbstractFileInitializer implements Initializer {
    protected final File outputDir;
    protected final String path;
    protected final Logger logger;

    public AbstractFileInitializer(Logger logger, String path, File outputDir) {
        this.outputDir = outputDir;
        this.logger = logger;
        this.path = path;
    }

    @Override
    public CharSequence initialData(String name) {
        File file = new File(outputDir.getAbsolutePath() + File.separator + path + name);
        file.getParentFile().mkdirs();

        CharSequence result;
        try {
            // Eclipse can't handle the getCharContent
            // See https://bugs.eclipse.org/bugs/show_bug.cgi?id=246089
            // 2008-09-12 RoelS: I've posted a patch file
            result = tryWithReader(file);
        } catch (FileNotFoundException e) {
            // Could happen
            return null;
        } catch (IOException e) {
            logger.note(LogLocation.MESSAGER, "Eclipse gave an IOException: " + e.getMessage());
            return null;
        } catch (Exception other) {
            try {
                // Javac can't handle the openReader
                // Filed as a bug at bugs.sun.com and received a review ID: 1339738
                result = ResourceGroovyMethods.getText(file);
            } catch (FileNotFoundException e) {
                // Could happen
                return null;
            } catch (IOException e) {
                logger.note(LogLocation.MESSAGER, "Javac gave an IOException: " + e.getMessage());
                return null;
            }
        }
        return result;
    }

    protected CharSequence tryWithReader(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } finally {
            reader.close();
        }
        return sb;
    }
}
