package io.github.sickfar.snapmock.generator

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.path
import io.github.sickfar.snapmock.core.PathSource
import io.github.sickfar.snapmock.core.SnapReader
import io.github.sickfar.snapmock.core.Source
import io.github.sickfar.snapmock.core.objectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

enum class Lang { JAVA, KOTLIN }

enum class TestFramework { JUNIT }

enum class MockFramework { MOCKITO }

enum class AssertFramework { HAMCREST }

enum class Mode { PER_SNAP_FILE, PER_CLASS, PER_METHOD }

fun process(
    mockFramework: MockFramework,
    testFramework: TestFramework,
    assertFramework: AssertFramework,
    lang: Lang,
    mode: Mode,
    output: Path,
    snapSources: List<Source>
) {
    val reader = SnapReader(objectMapper())
    val transformer = SnapTransformer(
        mockGenerator(mockFramework, testFramework),
        assertGenerator(assertFramework, testFramework)
    )
    val codeGenerator = codeGenerator(lang, mode, output)
    when (mode) {
        Mode.PER_CLASS -> {
            val snapsByClassName = snapSources.stream().map(reader::read).collect(Collectors.groupingBy { it.snap.main.className })
            snapsByClassName.forEach { (className, snaps) ->
                val tests = snaps.map { snap -> transformer.transform(snap) }
                val testFile = codeGenerator.generate(
                    className, null, tests
                )
                print("Test file written: $testFile")
            }
        }

        Mode.PER_METHOD -> {
            val snapsByClassAndMethodName =
                snapSources.stream().map(reader::read).collect(Collectors.groupingBy { it.snap.main.className to it.snap.main.methodName })
            snapsByClassAndMethodName.forEach { (classNameAndMethodName, snaps) ->
                val tests = snaps.map { snap -> transformer.transform(snap) }
                val testFile = codeGenerator.generate(
                    classNameAndMethodName.first, classNameAndMethodName.second, tests
                )
                print("Test file written: $testFile")
            }
        }

        Mode.PER_SNAP_FILE -> {
            snapSources.stream().map(reader::read).map { snap -> (snap.snap.main.className to snap.snap.main.methodName) to transformer.transform(snap) }
                .forEach { (classNameAndMethodName, test) ->
                    val testFile = codeGenerator.generate(
                        classNameAndMethodName.first, classNameAndMethodName.second, listOf(test)
                    )
                    print("Test file written: $testFile")
                }
        }
    }
}

class SnapMockGeneratorCommand : CliktCommand("snapmock-gen") {
    private val input by argument(help = "Input directory or file").path(mustExist = true, mustBeReadable = true)
    private val output by option("-o", "--output", help = "Output directory").path(
        mustExist = false, mustBeWritable = true
    ).required()
    private val lang by option("-l", "--lang", help = "In which programming language generate tests").enum<Lang>()
        .default(Lang.JAVA)
    private val testFramework by option("-tf", "--test-framework", help = "Test framework").enum<TestFramework>()
        .default(TestFramework.JUNIT)
    private val mockFramework by option("-mf", "--mock-framework", help = "Mock framework").enum<MockFramework>()
        .default(MockFramework.MOCKITO)
    private val assertFramework by option(
        "-as", "--assertion-framework", help = "Assertion framework"
    ).enum<AssertFramework>().default(AssertFramework.HAMCREST)
    private val mode by option("-m", "--mode", help = "Test file generation mode").enum<Mode>().default(Mode.PER_CLASS)

    override fun run() {
        val snaps = if (Files.isDirectory(input)) {
            Files.walk(input).filter { Files.isRegularFile(it).and(it.endsWith(".json")) }.toList()
        } else {
            listOf(input)
        }.stream().map { PathSource(it) }.toList()
        if (!Files.exists(output)) {
            Files.createDirectory(output)
        }
        process(mockFramework, testFramework, assertFramework, lang, mode, output, snaps)
    }
}

fun main(args: Array<String>) = SnapMockGeneratorCommand().main(args)
