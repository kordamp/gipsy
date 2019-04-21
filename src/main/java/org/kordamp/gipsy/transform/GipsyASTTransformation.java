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

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.control.SourceUnit;
import org.kordamp.jipsy.processor.CheckResult;
import org.kordamp.jipsy.processor.Logger;
import org.kordamp.jipsy.processor.Options;
import org.kordamp.jipsy.processor.ProcessorLogger;
import org.kordamp.jipsy.processor.service.ServiceProviderProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Andres Almiray
 */
public abstract class GipsyASTTransformation extends AbstractASTTransformation {
    protected Options options;
    protected Logger logger;
    private boolean disabled;

    @Override
    public void visit(ASTNode[] nodes, SourceUnit source) {
        if (nodes.length != 1 || !(nodes[0] instanceof ModuleNode)) {
            return;
        }

        ModuleNode moduleNode = (ModuleNode) nodes[0];
        initialize(moduleNode);
        if (disabled) {
            return;
        }

        String mainClassName = moduleNode.getMainClassName();
        PackageNode modulePackage = moduleNode.getPackage();
        List<AnnotationNode> packageAnnotations = modulePackage != null ? modulePackage.getAnnotations() : new ArrayList<AnnotationNode>();

        for (ClassNode classNode : moduleNode.getClasses()) {
            if (classNode.isDerivedFrom(ClassHelper.SCRIPT_TYPE) &&
                mainClassName.equals(classNode.getName()) &&
                !packageAnnotations.isEmpty()) {
                process(classNode, packageAnnotations, moduleNode);
                continue;
            }

            List<AnnotationNode> annotations = classNode.getAnnotations(getAnnotationClassNode());
            if (annotations.isEmpty()) {
                continue;
            }
            process(classNode, annotations, moduleNode);
        }
    }

    protected void initialize(ModuleNode moduleNode) {
        options = new Options(ServiceProviderProcessor.NAME, Collections.<String, String>emptyMap());
        if (options.disabled()) {
            return;
        }
        logger = new ProcessorLogger(new DefaultMessager(), options);
    }

    protected abstract ClassNode getAnnotationClassNode();

    protected final void process(ClassNode classNode, List<AnnotationNode> annotations, ModuleNode moduleNode) {
        removeStaleData(classNode, moduleNode);
        handleAnnotations(classNode, annotations, moduleNode);
        writeData();
    }

    protected abstract void removeStaleData(ClassNode classNode, ModuleNode moduleNode);

    protected abstract void handleAnnotations(ClassNode classNode, List<AnnotationNode> annotations, ModuleNode moduleNode);

    protected abstract void writeData();

    public static boolean hasNoArgsConstructor(ClassNode classNode) {
        for (ConstructorNode constructorNode : classNode.getDeclaredConstructors()) {
            Parameter[] parameters = constructorNode.getParameters();
            if (constructorNode.isPublic() && parameters == null || parameters.length == 0) {
                return true;
            }
        }
        return classNode.getDeclaredConstructors().size() == 0;
    }

    protected CheckResult isImplementation(ClassNode classNode, ClassNode type) {
        if (classNode.implementsInterface(type)) {
            return CheckResult.OK;
        }

        String message;
        if (type.isInterface()) {
            message = "does not implement";
        } else {
            message = "does not extend";
        }
        return CheckResult.valueOf(message + " " + type.getName());
    }

    protected List<Expression> findCollectionValueMember(AnnotationNode annotation, String memberName) {
        Expression value = annotation.getMember(memberName);
        if (value instanceof ListExpression) {
            ListExpression list = (ListExpression) value;
            return list.getExpressions();
        } else if (value != null) {
            return Collections.singletonList(value);
        }
        throw new IllegalStateException("No value found for member " + memberName);
    }

    protected Expression findSingleValueMember(AnnotationNode annotation, String memberName) {
        Expression value = annotation.getMember(memberName);
        if (value instanceof ListExpression) {
            ListExpression list = (ListExpression) value;
            List<Expression> values = list.getExpressions();
            return values != null && !values.isEmpty() ? values.get(0) : null;
        }
        return value;
    }
}
