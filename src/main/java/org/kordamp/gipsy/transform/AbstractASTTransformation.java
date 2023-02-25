/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2013-2022 Andres Almiray
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
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.transform.ASTTransformation;

import java.util.Collections;

/**
 * @author Andres Almiray
 */
public abstract class AbstractASTTransformation implements ASTTransformation {
    private static final ClassNode COLLECTIONS_CLASS = makeClassSafe(Collections.class);

    public void addError(String msg, ASTNode expr, SourceUnit source) {
        int line = expr.getLineNumber();
        int col = expr.getColumnNumber();
        source.getErrorCollector().addErrorAndContinue(
            new SyntaxErrorMessage(new SyntaxException(msg + '\n', line, col), source)
        );
    }

    protected void checkNodesForAnnotationAndType(ASTNode node1, ASTNode node2) {
        if (!(node1 instanceof AnnotationNode) || !(node2 instanceof ClassNode)) {
            throw new IllegalArgumentException("Internal error: wrong types: " + node1.getClass() + " / " + node2.getClass());
        }
    }

    public static Expression emptyMap() {
        return new StaticMethodCallExpression
            (COLLECTIONS_CLASS, "emptyMap", ArgumentListExpression.EMPTY_ARGUMENTS);
    }

    protected static ClassNode newClass(ClassNode classNode) {
        return classNode.getPlainNodeReference();
    }

    public static ClassNode makeClassSafe(String className) {
        return makeClassSafeWithGenerics(className);
    }

    public static ClassNode makeClassSafe(Class klass) {
        return makeClassSafeWithGenerics(klass);
    }

    public static ClassNode makeClassSafe(ClassNode classNode) {
        return makeClassSafeWithGenerics(classNode);
    }

    public static ClassNode makeClassSafeWithGenerics(String className, String... genericTypes) {
        GenericsType[] gtypes = new GenericsType[0];
        if (genericTypes != null) {
            gtypes = new GenericsType[genericTypes.length];
            for (int i = 0; i < gtypes.length; i++) {
                gtypes[i] = new GenericsType(makeClassSafe(genericTypes[i]));
            }
        }
        return makeClassSafe0(ClassHelper.make(className), gtypes);
    }

    public static ClassNode makeClassSafeWithGenerics(Class klass, Class... genericTypes) {
        GenericsType[] gtypes = new GenericsType[0];
        if (genericTypes != null) {
            gtypes = new GenericsType[genericTypes.length];
            for (int i = 0; i < gtypes.length; i++) {
                gtypes[i] = new GenericsType(makeClassSafe(genericTypes[i]));
            }
        }
        return makeClassSafe0(ClassHelper.make(klass), gtypes);
    }

    public static ClassNode makeClassSafeWithGenerics(ClassNode classNode, ClassNode... genericTypes) {
        GenericsType[] gtypes = new GenericsType[0];
        if (genericTypes != null) {
            gtypes = new GenericsType[genericTypes.length];
            for (int i = 0; i < gtypes.length; i++) {
                gtypes[i] = new GenericsType(newClass(genericTypes[i]));
            }
        }
        return makeClassSafe0(classNode, gtypes);
    }

    public static GenericsType makeGenericsType(String className, String[] upperBounds, String lowerBound, boolean placeHolder) {
        ClassNode[] up = new ClassNode[0];
        if (upperBounds != null) {
            up = new ClassNode[upperBounds.length];
            for (int i = 0; i < up.length; i++) {
                up[i] = makeClassSafe(upperBounds[i]);
            }
        }
        return makeGenericsType(makeClassSafe(className), up, makeClassSafe(lowerBound), placeHolder);
    }

    public static GenericsType makeGenericsType(Class klass, Class[] upperBounds, Class lowerBound, boolean placeHolder) {
        ClassNode[] up = new ClassNode[0];
        if (upperBounds != null) {
            up = new ClassNode[upperBounds.length];
            for (int i = 0; i < up.length; i++) {
                up[i] = makeClassSafe(upperBounds[i]);
            }
        }
        return makeGenericsType(makeClassSafe(klass), up, makeClassSafe(lowerBound), placeHolder);
    }

    public static GenericsType makeGenericsType(ClassNode classNode, ClassNode[] upperBounds, ClassNode lowerBound, boolean placeHolder) {
        classNode = newClass(classNode);
        classNode.setGenericsPlaceHolder(placeHolder);
        return new GenericsType(classNode, upperBounds, lowerBound);
    }

    public static ClassNode makeClassSafe0(ClassNode classNode, GenericsType... genericTypes) {
        ClassNode plainNodeReference = newClass(classNode);
        if (genericTypes != null && genericTypes.length > 0)
            plainNodeReference.setGenericsTypes(genericTypes);
        return plainNodeReference;
    }
}
