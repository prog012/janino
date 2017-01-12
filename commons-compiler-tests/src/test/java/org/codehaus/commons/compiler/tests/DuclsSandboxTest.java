
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2017 Arno Unkrig. All rights reserved.
 * Copyright (c) 2015-2016 TIBCO Software Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.commons.compiler.tests;

import java.lang.reflect.Field;
import java.security.AccessControlException;
import java.security.AllPermission;
import java.security.Permissions;
import java.util.List;

import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.nullanalysis.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.unkrig.commons.lang.security.Sandbox;
import util.JaninoTestSuite;
import util.TestUtil;

/**
 * Test cases for the combination of JANINO with {@link de.unkrig.commons.lang.security.Sandbox}.
 */
@RunWith(Parameterized.class) public
class DuclsSandboxTest extends JaninoTestSuite {

    private static final Permissions NO_PERMISSIONS = new Permissions();

    private static final Permissions ALL_PERMISSIONS = new Permissions();
    static { DuclsSandboxTest.ALL_PERMISSIONS.add(new AllPermission()); }

    /**
     * Get all available compiler factories for the "CompilerFactory" JUnit parameter.
     */
    @Parameters(name = "CompilerFactory={0}") public static List<Object[]>
    compilerFactories() throws Exception { return TestUtil.getCompilerFactoriesForParameters(); }

    public
    DuclsSandboxTest(ICompilerFactory compilerFactory) throws Exception {
        super(compilerFactory);
    }

    /**
     * Verifies that a trivial script works in the no-permissions sandbox.
     */
    @Test public void
    testReturnTrue() throws Exception {
        String script = "return true;";
        this.confinedScriptTest(script, DuclsSandboxTest.NO_PERMISSIONS).assertResultTrue();
    }

    /**
     * Verifies that it is not possible to retrieve a system property.
     */
    @Test(expected = AccessControlException.class) public void
    testGetSystemProperty() throws Exception {
        String script = "System.getProperty(\"foo\"); return true;";
        this.confinedScriptTest(script, DuclsSandboxTest.NO_PERMISSIONS).assertResultTrue();
    }

    /**
     * Verifies that it is not possible to delete a file.
     */
    @Test(expected = AccessControlException.class) public void
    testFileDelete() throws Exception {
        String script = "return new java.io.File(\"path/to/file.txt\").delete();";
        this.confinedScriptTest(script, DuclsSandboxTest.NO_PERMISSIONS).assertResultTrue();
    }

    /**
     * Verifies that {@code .class} works in the no-permissions sandbox.
     */
    @Test public void
    testDotClass() throws Exception {
        String script = "return (System.class != null);";
        this.confinedScriptTest(script, DuclsSandboxTest.NO_PERMISSIONS).assertResultTrue();
    }

    /**
     * Verifies that {@link Class#forName(String)} is accessible in the no-permissions sandbox.
     */
    @Test public void
    testClassForName() throws Exception {
        String script = "return (System.class.forName(\"java.lang.String\") != null);";
        this.confinedScriptTest(script, DuclsSandboxTest.NO_PERMISSIONS).assertResultTrue();
    }

    /**
     * Verifies that {@link Class#getDeclaredField(String)} is forbidden in the no-permissions sandbox.
     */
    @Test(expected = AccessControlException.class) public void
    testDotClassGetDeclaredField() throws Exception {
        String script = "return (String.class.getDeclaredField(\"value\") != null);";
        this.confinedScriptTest(script, DuclsSandboxTest.NO_PERMISSIONS).assertResultTrue();
    }

    /**
     * Verifies that {@link Class#getDeclaredField(String)} and {@link Field#setAccessible(boolean)} <em>are</em>
     * allowed in an <em>all-permissions</em> sandbox.
     */
    @Test public void
    testDotClassGetDeclaredFieldAllPermissions() throws Exception {
        String.class.getDeclaredField("value").setAccessible(true);
        String script = "String.class.getDeclaredField(\"value\").setAccessible(true); return true;";
        this.confinedScriptTest(script, DuclsSandboxTest.ALL_PERMISSIONS).assertResultTrue();
    }

    // ====================================== END OF TEST CASES ======================================

    /**
     * Creates and returns a {@link ScriptTest} object that executes scripts in a {@link Sandbox} with the given
     * <var>permissions</var>.
     */
    private ScriptTest
    confinedScriptTest(String script, final Permissions permissions) throws Exception {

        return new ScriptTest(script) {

            @Override protected void
            compile() throws Exception {
                this.scriptEvaluator.setThrownExceptions(new Class<?>[] { Exception.class });
                super.compile();
            }

            @Override @Nullable protected Object
            execute() throws Exception {
                Sandbox.confine(this.scriptEvaluator.getMethod().getDeclaringClass(), permissions);
                return super.execute();
            }
        };
    }
}
