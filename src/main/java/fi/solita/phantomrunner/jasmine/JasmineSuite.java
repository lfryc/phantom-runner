/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2012 Solita Oy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package fi.solita.phantomrunner.jasmine;

import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;

import fi.solita.phantomrunner.PhantomProcessNotifier;
import fi.solita.phantomrunner.testinterpreter.JavascriptTest;
import fi.solita.phantomrunner.util.JavascriptBlockUtils;
import fi.solita.phantomrunner.util.ObjectMemoizer;
import fi.solita.phantomrunner.util.ParametrizedFactory;
import fi.solita.phantomrunner.util.Strings;

/**
 * Class representing Jasmine's test suite described with function 'describe'
 *
 */
public class JasmineSuite implements JavascriptTest {

    private final String name;
    private final String data;
    private final ObjectMemoizer<Description, Class<?>> description;
    
    private final List<JasmineSpec> specs = new ArrayList<JasmineSpec>();

    public JasmineSuite(String describe, Class<?> parentTestClass) {
        this.name = Strings.firstMatch(describe, "(?<=\").*(?=\")");
        this.data = describe;
        
        // AST would be so much better than this string pseudo parsing but there just isn't reasonable
        // lightweight javascript parsers available. Tried Rhino and ANTLR with proper grammar to no
        // avail. Google Caja might be a solution but there's no maven artifacts available :(
        for (String spec : JavascriptBlockUtils.findBlocks(describe, "it(")) {
            if (!spec.isEmpty()) {
                specs.add(new JasmineSpec(spec, this, parentTestClass));
            }
        }
        
        this.description = new ObjectMemoizer<Description, Class<?>>(
                new JasmineSuiteDescriptionFactory(parentTestClass), parentTestClass);
    }
    
    @Override
    public Description asDescription(Class<?> parentTestClass) {
        Description desc = Description.createSuiteDescription(getTestName());
        for (JasmineSpec spec : specs) {
            desc.addChild(spec.asDescription(parentTestClass));
        }
        return desc;
    }
    
    @Override
    public void run(RunNotifier notifier, PhantomProcessNotifier processNotifier) {
        notifier.fireTestStarted(description.get());
        for (JasmineSpec spec : specs) {
            spec.run(notifier, processNotifier);
        }
        notifier.fireTestFinished(description.get());
    }

    @Override
    public String getTestName() {
        return name;
    }

    @Override
    public String getTestData() {
        return data;
    }
    
    @Override
    public boolean isTest() {
        return false;
    }

    @Override
    public String getSuiteName() {
        return getTestName();
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Jasmine Test Suite:\n");
        builder.append("  Suite data:\n");
        builder.append(Strings.indentLines(data, 4));
        builder.append("\n  Specifications:\n");
        
        for (int i = 0; i < specs.size(); i++) {
            if (i > 0) {
                builder.append(",\n");
            }
            builder.append(Strings.indentLines(specs.get(i).toString(), 4));
        }
        return builder.toString();
    }
    
    private class JasmineSuiteDescriptionFactory implements ParametrizedFactory<Description, Class<?>> {
        private final Class<?> testClass;

        public JasmineSuiteDescriptionFactory(Class<?> parentTestClass) {
            this.testClass = parentTestClass;
        }

        @Override
        public Description create(Class<?> param) {
            Description desc = Description.createSuiteDescription(getTestName());
            for (JasmineSpec spec : specs) {
                desc.addChild(spec.asDescription(testClass));
            }
            return desc;
        }
    }

}