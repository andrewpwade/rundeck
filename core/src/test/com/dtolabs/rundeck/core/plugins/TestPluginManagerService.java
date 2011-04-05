/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* TestPluginManagerService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/31/11 4:23 PM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.File;
import java.io.IOException;

/**
 * TestPluginManagerService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestPluginManagerService extends AbstractBaseTest {
    private static final String TEST_SERVICE = "TestService";
    Framework testFramework;
    String testnode;
    private static final String TEST_PROJECT = "TestPluginManagerService";
    private PluginManagerService service;

    public TestPluginManagerService(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestPluginManagerService.class);
    }

    protected void setUp() {
        super.setUp();
        testFramework = getFrameworkInstance();
        testnode = testFramework.getFrameworkNodeName();
        final FrameworkProject frameworkProject = testFramework.getFrameworkProjectMgr().createFrameworkProject(
            TEST_PROJECT);
        File resourcesfile = new File(frameworkProject.getNodesResourceFilePath());
        //copy test nodes to resources file
        try {
            FileUtils.copyFileStreams(new File("src/test/com/dtolabs/rundeck/core/common/test-nodes1.xml"),
                resourcesfile);
        } catch (IOException e) {
            throw new RuntimeException("Caught Setup exception: " + e.getMessage(), e);
        }
        service = PluginManagerService.getInstanceForFramework(
            getFrameworkInstance());
    }

    protected void tearDown() throws Exception {
        super.tearDown();

        File projectdir = new File(getFrameworkProjectsBase(), TEST_PROJECT);
        FileUtils.deleteDir(projectdir);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    static class testService implements PluggableService {
        String name;
        boolean isValid = true;

        public boolean isValidProviderClass(Class clazz) {
            return isValid;
        }

        Class registeredClass;
        String registeredName;
        boolean throwException = false;

        public void registerProviderClass(final Class clazz, final String name) throws PluginException {
            this.registeredClass = clazz;
            this.registeredName = name;
            if (throwException) {
                throw new PluginException("test exception");
            }
        }

        public boolean isScriptPluggable() {
            return false;
        }

        public void registerScriptProvider(ScriptPluginProvider provider) throws PluginException {
        }

        public String getName() {
            return name;
        }
    }

    public void testNullClassname() throws Exception {//test null classname
        final testService pluggableService = new testService();
        pluggableService.isValid = false;
        pluggableService.name = TEST_SERVICE;
        final Framework frameworkInstance = getFrameworkInstance();
        frameworkInstance.setService("test", pluggableService);
        try {
            service.loadProviderByClassname(null, service.getClass().getClassLoader());
            fail("should fail");
        } catch (IllegalArgumentException e) {
            assertEquals("A null java class name was specified.",
                e.getMessage());

        }

    }


    public void testNotFoundClassname() {//test class not found
        final testService pluggableService = new testService();
        pluggableService.isValid = true;
        pluggableService.name = TEST_SERVICE;
        final Framework frameworkInstance = getFrameworkInstance();
        frameworkInstance.setService("test", pluggableService);
        try {
            service.loadProviderByClassname("test.invalid.classname",
                service.getClass().getClassLoader());
            fail("should fail");
        } catch (PluginException e) {
            assertEquals("Class not found: test.invalid.classname", e.getMessage());
        }
    }

    public void testNoService() {
        final testService pluggableService = new testService();
        pluggableService.isValid = false;
        pluggableService.name = TEST_SERVICE;
        final Framework frameworkInstance = getFrameworkInstance();
        frameworkInstance.setService("test", pluggableService);
        try {
            service.loadProviderByClassname(TestInvalidService.class.getName(),
                service.getClass().getClassLoader());
            fail("should fail");
        } catch (PluginException e) {
            assertEquals("Class " + TestInvalidService.class.getName()
                         + " did not specify a valid service name: invalid: no such service", e.getMessage());
        }

    }
    public void testInvalidCheck() {//test isValid returns false
        final testService pluggableService = new testService();
        pluggableService.isValid = false;
        pluggableService.name = TEST_SERVICE;
        final Framework frameworkInstance = getFrameworkInstance();
        frameworkInstance.setService("test", pluggableService);
        try {
            service.loadProviderByClassname(TestOK.class.getName(),
                service.getClass().getClassLoader());
            fail("should fail");
        } catch (PluginException e) {
            assertEquals("Class " + TestOK.class.getName() + " was not a valid plugin class for service: " + TEST_SERVICE,
                e.getMessage());

        }

    }

    public void testNoAnnotation() {
        final testService pluggableService = new testService();
        pluggableService.isValid = true;
        pluggableService.name = TEST_SERVICE;
        final Framework frameworkInstance = getFrameworkInstance();
        frameworkInstance.setService("test", pluggableService);
        try {
            service.loadProviderByClassname(TestMissingAnnotation.class.getName(),
                service.getClass().getClassLoader());
            fail("should fail");
        } catch (PluginException e) {
            assertEquals("No Plugin annotation was found for the class: " + TestMissingAnnotation.class.getName()
                         , e.getMessage());
        }
    }

    public void testEmptyNameAnnotation() {
        final testService pluggableService = new testService();
        pluggableService.isValid = true;
        pluggableService.name = TEST_SERVICE;
        final Framework frameworkInstance = getFrameworkInstance();
        frameworkInstance.setService("test", pluggableService);
        try {
            service.loadProviderByClassname(TestEmptyName.class.getName(),
                service.getClass().getClassLoader());
            fail("should fail");
        } catch (PluginException e) {
            assertEquals("Plugin annotation 'name' cannot be empty for the class: " + TestEmptyName.class.getName()
                         , e.getMessage());
        }
    }
    public void testEmptyServiceAnnotation() {
        final testService pluggableService = new testService();
        pluggableService.isValid = true;
        pluggableService.name = TEST_SERVICE;
        final Framework frameworkInstance = getFrameworkInstance();
        frameworkInstance.setService("test", pluggableService);
        try {
            service.loadProviderByClassname(TestEmptyService.class.getName(),
                service.getClass().getClassLoader());
            fail("should fail");
        } catch (PluginException e) {
            assertEquals("Plugin annotation 'service' cannot be empty for the class: " + TestEmptyService.class.getName()
                         , e.getMessage());
        }
    }

    public void testValidPlugin() {
        final testService pluggableService = new testService();
        pluggableService.isValid = true;
        pluggableService.name = TEST_SERVICE;
        final Framework frameworkInstance = getFrameworkInstance();
        frameworkInstance.setService("test", pluggableService);
        try {
            service.loadProviderByClassname(TestOK.class.getName(),
                service.getClass().getClassLoader());
        } catch (PluginException e) {
            fail("unexpected exception: " + e);
        }
        assertNotNull(pluggableService.registeredClass);
        assertEquals(TestOK.class, pluggableService.registeredClass);
        assertNotNull(pluggableService.registeredName);
        assertEquals("test", pluggableService.registeredName);

    }

    /**
     * missing annotation
     */
    public static class TestMissingAnnotation {

    }

    /**
     * empty name
     */
    @Plugin (name = "")
    public static class TestEmptyName {

    }

    /**
     * empty name
     */
    @Plugin (name = "test", service = "")
    public static class TestEmptyService {

    }
    /**
     * empty name
     */
    @Plugin (name = "test", service = "invalid")
    public static class TestInvalidService {

    }

    /**
     * empty name
     */
    @Plugin (name = "test", service = "test")
    public static class TestOK {

    }
}
