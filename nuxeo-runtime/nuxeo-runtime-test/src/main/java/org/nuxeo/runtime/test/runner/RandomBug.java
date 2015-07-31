/*
 * (C) Copyright 2014-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     slacoin, jcarsique
 *
 */
package org.nuxeo.runtime.test.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import javax.inject.Inject;

import org.apache.log4j.MDC;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * Define execution rules for an annotated random bug.
 * <p>
 * Principle is to increase consistency on tests which have a random behavior. Such test is a headache because:
 * <ul>
 * <li>some developers may ask to ignore a random test since it's not reliable and produces useless noise most of the
 * time,</li>
 * <li>however, the test may still be useful in continuous integration for checking the non-random part of code it
 * covers,</li>
 * <li>and, after all, there's a random bug which should be fixed!</li>
 * </ul>
 * </p>
 * <p>
 * Compared to the @{@link Ignore} JUnit annotation, the advantage is to provide different behaviors for different use
 * cases. The wanted behavior depending on whereas:
 * <ul>
 * <li>we are working on something else and don't want being bothered by an unreliable test,</li>
 * <li>we are working on the covered code and want to be warned in case of regression,</li>
 * <li>we are working on the random bug and want to reproduce it.</li>
 * </ul>
 * </p>
 * That means that a random bug cannot be ignored. But must attempt to reproduce or hide its random aspect, depending on
 * its execution context. For instance: <blockquote>
 *
 * <pre>
 * <code>
 * import org.nuxeo.runtime.test.runner.FeaturesRunner;
 * import org.nuxeo.runtime.test.RandomBugRule;
 *
 * {@literal @}RunWith(FeaturesRunner.class)
 * public class TestSample {
 *     public static final String NXP99999 = "Some comment or description";
 *
 *     {@literal @}Test
 *     {@literal @}RandomBugRule.Repeat(issue = NXP99999, onFailure=5, onSuccess=50)
 *     public void testWhichFailsSometimes() throws Exception {
 *         assertTrue(java.lang.Math.random() > 0.2);
 *     }
 * }</code>
 * </pre>
 *
 * </blockquote>
 * <p>
 * In the above example, the test fails sometimes. With the {@link RandomBug.Repeat} annotation, it will be repeated in
 * case of failure up to 5 times until success. This is the default {@link Mode#RELAX} mode. In order to reproduce the
 * bug, use the {@link Mode#STRICT} mode. It will be repeated in case of success up to 50 times until failure. In
 * {@link Mode#BYPASS} mode, the test is ignored.
 * </p>
 * <p>
 * You may also repeat a whole suite in the same way by annotating the class itself. You may want also want to skip some
 * tests, then you can annotate them and set {@link Repeat#bypass()} to true.
 * </p>
 *
 * @see Mode
 * @since 5.9.5
 */
public class RandomBug {

    protected static final RandomBug self = new RandomBug();

    /**
     * Repeat condition based on
     *
     * @see Mode
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.TYPE })
    @Inherited
    public @interface Repeat {
        /**
         * Reference in issue management system. Recommendation is to use a constant which name is the issue reference
         * and value is a description or comment.
         */
        String issue();

        /**
         * Times to repeat until failure in case of success
         */
        int onSuccess() default 30;

        /**
         * Times to repeat until success in case of failure
         */
        int onFailure() default 10;

        /**
         * Bypass a method/suite ....
         */
        boolean bypass() default false;
    }

    public static class Feature extends SimpleFeature {
        @ClassRule
        public static TestRule onClass() {
            return self.onTest();
        }

        @Rule
        public MethodRule onMethod() {
            return self.onMethod();
        }
    }

    public class RepeatRule implements TestRule, MethodRule {
        @Inject
        protected RunNotifier notifier;

        public RepeatStatement statement;

        @Override
        public Statement apply(Statement base, Description description) {
            final Repeat actual = description.getAnnotation(Repeat.class);
            if (actual == null) {
                return base;
            }
            return statement = onRepeat(actual, notifier, base);
        }

        @Override
        public Statement apply(Statement base, FrameworkMethod method, Object target) {
            final Repeat actual = method.getAnnotation(Repeat.class);
            if (actual == null) {
                return base;
            }
            return statement = onRepeat(actual, notifier, base);
        }
    }

    protected RepeatRule onTest() {
        return new RepeatRule();
    }

    protected RepeatRule onMethod() {
        return new RepeatRule();
    }

    public static final String MODE_PROPERTY = "nuxeo.tests.random.mode";

    /**
     * <ul>
     * <li>BYPASS: the test is ignored. Like with @{@link Ignore} JUnit annotation.</li>
     * <li>STRICT: the test must fail. On success, the test is repeated until failure or the limit number of tries
     * {@link Repeat#onSuccess()} is reached. If it does not fail during the tries, then the whole test class is marked
     * as failed.</li>
     * <li>RELAX: the test must succeed. On failure, the test is repeated until success or the limit number of tries
     * {@link Repeat#onFailure()} is reached.</li>
     * </ul>
     * Could be set by the environment using the <em>nuxeo.tests.random.mode</em>T system property.
     */
    public static enum Mode {
        BYPASS, STRICT, RELAX
    };

    /**
     * The default mode if {@link #MODE_PROPERTY} is not set.
     */
    public final Mode DEFAULT = Mode.RELAX;

    protected Mode fetchMode() {
        String mode = System.getProperty(MODE_PROPERTY, DEFAULT.name());
        return Mode.valueOf(mode.toUpperCase());
    }

    protected abstract class RepeatStatement extends Statement {
        protected final Repeat params;

        protected final RunNotifier notifier;

        protected boolean gotFailure;

        protected final RunListener listener = new RunListener() {
            @Override
            public void testFailure(Failure failure) throws Exception {
                gotFailure = true;
            }
        };

        protected final Statement base;

        protected int serial;

        protected RepeatStatement(Repeat someParams, RunNotifier aNotifier, Statement aStatement) {
            params = someParams;
            notifier = aNotifier;
            base = aStatement;
        }

        protected void onEnter(int aSerial) {
            MDC.put("fRepeat", serial = aSerial);
        }

        protected void onLeave() {
            MDC.remove("fRepeat");
        }

        @Override
        public void evaluate() throws Throwable {
            Error error = error();
            notifier.addListener(listener);
            try {
                for (int i = 1; i <= retryCount(); i++) {
                    onEnter(i);
                    try {
                        base.evaluate();
                        if (!gotFailure && returnOnSuccess()) {
                            return;
                        }
                    } catch (Throwable cause) {
                        error.addSuppressed(cause);
                        if (returnOnFailure()) {
                            throw error;
                        }
                    } finally {
                        onLeave();
                    }
                    if (gotFailure && returnOnFailure()) {
                        return;
                    }
                }
            } finally {
                notifier.removeListener(listener);
            }
            throw error;
        }

        protected abstract Error error();

        protected abstract int retryCount();

        protected abstract boolean returnOnSuccess();

        protected abstract boolean returnOnFailure();

    }

    protected class RepeatOnFailure extends RepeatStatement {
        protected String issue;

        protected RepeatOnFailure(Repeat someParams, RunNotifier aNotifier, Statement aStatement) {
            super(someParams, aNotifier, aStatement);
        }

        @Override
        protected Error error() {
            return new AssertionError(String.format("No success after %d tries. Either the bug is not random "
                    + "or you should increase the 'onFailure' value.\n" + "Issue: %s", params.onFailure(), issue));
        }

        @Override
        protected int retryCount() {
            return params.onFailure();
        }

        @Override
        protected boolean returnOnFailure() {
            return false;
        }

        @Override
        protected boolean returnOnSuccess() {
            return true;
        }
    }

    protected class RepeatOnSuccess extends RepeatStatement {
        protected RepeatOnSuccess(Repeat someParams, RunNotifier aNotifier, Statement aStatement) {
            super(someParams, aNotifier, aStatement);
        }

        @Override
        protected Error error() {
            return new AssertionError(String.format("No failure after %d tries. Either the bug is fixed "
                    + "or you should increase the 'onSuccess' value.\n" + "Issue: %s", params.onSuccess(),
                    params.issue()));
        }

        @Override
        protected boolean returnOnFailure() {
            return true;
        }

        @Override
        protected boolean returnOnSuccess() {
            return false;
        }

        @Override
        protected int retryCount() {
            return params.onSuccess();
        }
    }

    protected class Bypass extends RepeatStatement {
        public Bypass(Repeat someParams, RunNotifier aNotifier, Statement aStatement) {
            super(someParams, aNotifier, aStatement);
        }

        @Override
        public void evaluate() throws Throwable {
            throw new AssumptionViolatedException("Random bug ignored (bypass mode): " + params.issue());
        }

        @Override
        protected Error error() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected int retryCount() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected boolean returnOnSuccess() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected boolean returnOnFailure() {
            return false;
        }
    }

    protected RepeatStatement onRepeat(Repeat someParams, RunNotifier aNotifier, Statement aStatement) {
        if (someParams.bypass()) {
            return new Bypass(someParams, aNotifier, aStatement);
        }
        switch (fetchMode()) {
        case BYPASS:
            return new Bypass(someParams, aNotifier, aStatement);
        case STRICT:
            return new RepeatOnSuccess(someParams, aNotifier, aStatement);
        case RELAX:
            return new RepeatOnFailure(someParams, aNotifier, aStatement);
        }
        throw new IllegalArgumentException("no such mode");
    }

}
