package org.snapmock.generator

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.path
import org.snapmock.snap.core.SnapReader
import org.snapmock.snap.core.objectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

enum class Lang { JAVA, KOTLIN }

enum class TestFramework { JUNIT }

enum class MockFramework { MOCKITO }

enum class AssertFramework { HAMCREST }

enum class Mode { PER_CLASS, PER_METHOD }

class SnapMockGeneratorCommand : CliktCommand() {
    val input by argument(help = "Input directory or file").path(mustExist = true, mustBeReadable = true)
    val output by option("-o", "--output", help = "Output directory").path(mustExist = false, mustBeWritable = true).required()
    val lang by option("-l", "--lang", help = "In which programming language generate tests").enum<Lang>().default(Lang.JAVA)
    val testFramework by option("-tf", "--test-framework", help = "Test framework").enum<TestFramework>().default(TestFramework.JUNIT)
    val mockFramework by option("-mf", "--mock-framework", help = "Mock framework").enum<MockFramework>().default(MockFramework.MOCKITO)
    val assertFramework by option("-as", "--assertion-framework", help = "Assertion framework").enum<AssertFramework>().default(AssertFramework.HAMCREST)
    val mode by option("-m", "--mode", help = "Test file generation mode").enum<Mode>().default(Mode.PER_CLASS)

    override fun run() {
        val filesToProcess: List<Path> = if (Files.isDirectory(input)) {
            Files.walk(input).filter { Files.isRegularFile(it).and(it.endsWith(".json")) }.toList()
        } else {
            listOf(input)
        }
        if (!Files.exists(output)) {
            Files.createDirectory(output)
        }
        val reader = SnapReader(objectMapper())
        val transformer = SnapTransformer(
            testGenerator(testFramework),
            mockGenerator(mockFramework),
            assertGenerator(assertFramework)
        )
        val codeGenerator = codeGenerator(lang)
        if (mode == Mode.PER_METHOD) {
            val snaps = filesToProcess.stream().map(reader::read).filter { it != null}.toList()
            snaps.stream()
                .map { snap -> snap.main.className to transformer.transform(snap) }
                .forEach { (className, test) ->
                    val testFile = codeGenerator.generate(output, className, listOf(test))
                    print("Test file written: $testFile")
                }
        } else {
            val snapsByClassName = filesToProcess.stream().map(reader::read).collect(Collectors.groupingBy { it.main.className })
            snapsByClassName.forEach { (className, snaps) ->
                val tests = snaps.map { snap -> transformer.transform(snap) }
                val testFile = codeGenerator.generate(output, className, tests)
                print("Test file written: $testFile")
            }
        }
    }
}

fun main(args: Array<String>) = SnapMockGeneratorCommand().main(args)
