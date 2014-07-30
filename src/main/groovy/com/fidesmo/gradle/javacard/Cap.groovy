/*
 * Copyright 2014 Fidesmo AB
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
 *
 */

package com.fidesmo.gradle.javacard

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile

class Cap extends DefaultTask {

    String group = 'build'
    String description = 'Create a cap for installation on a smart card'

    CapExtension capExtension
    JavacardExtension jcExtension

    @Input
    String classesDir

    @Input
    String capsDir

    @InputDirectory
    File getSourcePackagePath() {
        capExtension.validate()
        new File(classesDir + File.separator + capExtension.sourcePackage.replace('.', File.separator))
    }

    @OutputFile
    File getCapFile() {
        capExtension.validate()
        new File(capsDir + File.separator + capExtension.sourcePackage.replace('.', File.separator) + '/javacard/ndef.cap') // FIXME: hard coded stuff
    }


    protected def osDependent(Closure closure) {

        def options = new Object(){
            def windows
            def others
        }

        closure.setDelegate(options)
        closure.setResolveStrategy(Closure.DELEGATE_ONLY)
        closure.call()

        def os = System.getProperty('os.name').toLowerCase()
        if(os.contains('windows')) {
            options.windows
        } else {
            options.others
        }
    }

    protected def findExecutable(String name) {
        [ jcExtension.sdk.getPath(), 'bin', osDependent { windows =  "${name}.bat"; others = name }].join(File.separator)
    }

    @TaskAction
    def create() {
        capExtension.validate()
        project.exec {
            commandLine(
                osDependent {
                    def converter = findExecutable('converter')
                    windows = converter
                    others = [ 'sh',  converter]
                })

            args([ '-out', 'CAP',
                   '-d',  capsDir,
                   '-classdir', classesDir,
                   '-exportpath', [ jcExtension.sdk.getPath(), "api_export_files"].join(File.separator) ])

            args(capExtension.applets.collect {
                     aid, className -> [ '-applet', aid, capExtension.sourcePackage + '.' + className ] } .flatten())

            args([ capExtension.sourcePackage, capExtension.aid, capExtension.version ])
        }
    }
}
