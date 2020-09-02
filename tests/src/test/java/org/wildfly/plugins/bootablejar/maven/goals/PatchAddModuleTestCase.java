/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.plugins.bootablejar.maven.goals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.wildfly.plugins.bootablejar.patching.Module;
import static org.wildfly.plugins.bootablejar.patching.PatchingTestUtil.FILE_SEPARATOR;
import static org.wildfly.plugins.bootablejar.patching.PatchingTestUtil.RELATIVE_MODULES_PATH;
import static org.wildfly.plugins.bootablejar.patching.PatchingTestUtil.RELATIVE_PATCHES_PATH;
import static org.wildfly.plugins.bootablejar.patching.PatchingTestUtil.buildModulePatch;
import static org.wildfly.plugins.bootablejar.patching.PatchingTestUtil.randomString;
import org.wildfly.plugins.bootablejar.patching.ResourceItem;

/**
 * @author jdenise
 */
public class PatchAddModuleTestCase extends AbstractBootableJarMojoTestCase {
    public PatchAddModuleTestCase() {
        super("test15-pom.xml", true, null);
    }

    @Test
    public void testAddModulePatch()
            throws Exception {
        String patchid = randomString();
        String layerPatchID = randomString();
        Path patchContentDir = createTestDirectory("patch-test-content", patchid);
        final String moduleName = "org.wildfly.test." + randomString();
        final ResourceItem resourceItem1 = new ResourceItem("testFile1", "content1".getBytes(StandardCharsets.UTF_8));
        final ResourceItem resourceItem2 = new ResourceItem("testFile2", "content2".getBytes(StandardCharsets.UTF_8));

        Module newModule = new Module.Builder(moduleName)
                .miscFile(resourceItem1)
                .miscFile(resourceItem2)
                .build();
        final Path dir = getTestDir();
        buildModulePatch(patchContentDir, false, dir, patchid, newModule, layerPatchID);
        BuildBootableJarMojo mojo = lookupMojo("package");
        assertNotNull(mojo);
        mojo.execute();
        Path home = checkAndGetWildFlyHome(dir, true, true, null, null);
        try {
            // original module doesn't exist
            final String modulePath = home.toString() + FILE_SEPARATOR + RELATIVE_MODULES_PATH
                    + FILE_SEPARATOR + moduleName.replace(".", FILE_SEPARATOR) + FILE_SEPARATOR + "main";
            assertFalse(Files.exists(Paths.get(modulePath)));
            final String patchedModulePath = home.toString() + FILE_SEPARATOR + RELATIVE_PATCHES_PATH
                    + FILE_SEPARATOR + layerPatchID + FILE_SEPARATOR + moduleName.replace(".", FILE_SEPARATOR) + FILE_SEPARATOR + "main";
            assertTrue(Files.exists(Paths.get(patchedModulePath)));
            checkJar(dir, true, true, null, null);
            checkDeployment(dir, true);
        } finally {
            BuildBootableJarMojo.deleteDir(home);
        }
    }
}