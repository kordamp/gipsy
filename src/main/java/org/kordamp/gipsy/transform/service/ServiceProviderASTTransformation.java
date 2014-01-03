/*
 * Copyright 2013-2014 the original author or authors.
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

package org.kordamp.gipsy.transform.service;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.kordamp.gipsy.transform.GipsyASTTransformation;
import org.kordamp.jipsy.ServiceProviderFor;
import org.kordamp.jipsy.processor.CheckResult;
import org.kordamp.jipsy.processor.Persistence;
import org.kordamp.jipsy.processor.service.Service;
import org.kordamp.jipsy.processor.service.ServiceCollector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

/**
 * @author Andres Almiray
 */
@ServiceProviderFor(ASTTransformation.class)
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class ServiceProviderASTTransformation extends GipsyASTTransformation {
    public static final String NAME = ServiceProviderASTTransformation.class.getName()
        + " (" + ServiceProviderASTTransformation.class.getPackage().getImplementationVersion() + ")";

    private static final ClassNode SERVICE_PROVIDER_FOR_TYPE = makeClassSafe(ServiceProviderFor.class);

    private Persistence persistence;
    private ServiceCollector data;

    @Override
    protected ClassNode getAnnotationClassNode() {
        return SERVICE_PROVIDER_FOR_TYPE;
    }

    @Override
    protected void initialize(ModuleNode moduleNode) {
        super.initialize(moduleNode);

        File outputDir = moduleNode.getContext().getConfiguration().getTargetDirectory();
        persistence = new ServicePersistence(NAME, options.dir(), outputDir, logger);
        data = new ServiceCollector(persistence.getInitializer(), logger);

        // Initialize if possible
        for (String serviceName : persistence.tryFind()) {
            data.getService(serviceName);
        }
    }

    @Override
    protected void removeStaleData(ClassNode classNode, ModuleNode moduleNode) {
        data.removeProvider(classNode.getName());
    }

    protected void handleAnnotations(ClassNode classNode, List<AnnotationNode> annotations, ModuleNode moduleNode) {
        CheckResult checkResult = checkCurrentClass(classNode);
        if (checkResult.isError()) {
            addError(checkResult.getMessage(), classNode, moduleNode.getContext());
            return;
        }

        for (ClassNode service : findServices(annotations)) {
            CheckResult implementationResult = isImplementation(classNode, service);
            if (implementationResult.isError()) {
                addError(implementationResult.getMessage(), classNode, moduleNode.getContext());
            } else {
                register(service.getName(), classNode);
            }
        }
    }

    @Override
    protected void writeData() {
        for (Service service : data.services()) {
            try {
                persistence.write(service.getName(), service.toProviderNamesList());
            } catch (IOException e) {
                // TODO print out error
            }
        }
        persistence.writeLog();
    }

    private CheckResult checkCurrentClass(ClassNode currentClass) {
        if (currentClass.isInterface()) {
            return CheckResult.valueOf("is not a class");
        }
        if (!isPublic(currentClass.getModifiers())) {
            return CheckResult.valueOf("is not a public class");
        }

        if (isStatic(currentClass.getModifiers())) {
            return CheckResult.valueOf("is a static class");
        }

        if (!hasNoArgsConstructor(currentClass)) {
            return CheckResult.valueOf("has no public no-args constructor");
        }

        return CheckResult.OK;
    }

    private List<ClassNode> findServices(List<AnnotationNode> annotations) {
        List<ClassNode> services = new ArrayList<ClassNode>();

        for (AnnotationNode annotation : annotations) {
            for (Expression expr : findCollectionValueMember(annotation, "value")) {
                if (expr instanceof ClassExpression) {
                    services.add(((ClassExpression) expr).getType());
                }
            }
        }

        return services;
    }

    private void register(String serviceName, ClassNode provider) {
        data.getService(serviceName).addProvider(provider.getName());
    }
}