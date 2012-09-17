package fi.solita.phantomrunner.testinterpreter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;

import fi.collin.util.collections.UnmodifiableLinkedReferencingList;
import fi.solita.phantomrunner.jetty.PhantomProcessNotifier;

public final class MasterJavascriptTest implements JavascriptTest {

	private final Class<?> testClass;
	private final Map<String, List<JavascriptTest>> tests;
	private final JavascriptTestInterpreter interpreter;
	private final String[] extLibs;
	
	private Description cache;
	
	public MasterJavascriptTest(Class<?> testClass, JavascriptTestInterpreter interpreter, String[] extLibs) {
		this.testClass = testClass;
		this.interpreter = interpreter;
		this.extLibs = extLibs;
		
		MasterJavascriptListener listener = new MasterJavascriptListener();
		new JavascriptTestScanner(testClass, interpreter).parseTests(listener);
		this.tests = listener.getTests();
	}
	
	@Override
	public Description asDescription(Class<?> parentTestClass) {
		if (cache == null) {
			cache = Description.createSuiteDescription(testClass);
			for (JavascriptTest test : new UnmodifiableLinkedReferencingList<>(tests.values())) {
				cache.addChild(test.asDescription(testClass));
			}
		}
		return cache;
	}

	@Override
	public void run(RunNotifier notifier, PhantomProcessNotifier processNotifier) {
		notifier.fireTestStarted(cache);
		for (Entry<String, List<JavascriptTest>> testFile : tests.entrySet()) {
			processNotifier.initializeTestRun(testFile.getKey(), interpreter.getLibPaths(), extLibs);
			for (JavascriptTest test : testFile.getValue()) {
				test.run(notifier, processNotifier);
			}
		}
		notifier.fireTestFinished(cache);
	}

	@Override
	public String getTestName() {
		return testClass.getName();
	}

	@Override
	public String getTestData() {
		return "";
	}
	
	@Override
	public boolean isTest() {
		return false;
	}

	@Override
	public String getSuiteName() {
		return "";
	}
	
	
	private static class MasterJavascriptListener implements TestScannerListener {
		
		private final Map<String, List<JavascriptTest>> tests = new HashMap<>();
		
		@Override
		public void fileScanned(String data, List<JavascriptTest> testsFromData) {
			this.tests.put(data, testsFromData);
		}
		
		public Map<String, List<JavascriptTest>> getTests() {
			return tests;
		}
	}

}
