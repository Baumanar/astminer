package astminer.ast

import astminer.common.DEFAULT_TOKEN
import astminer.common.getNormalizedToken
import astminer.common.model.AstStorage
import astminer.common.model.Node
import astminer.common.normalizeToken
import astminer.common.preOrder
import astminer.common.storage.*
import java.io.File

/**
 * Stores multiple ASTs by their roots and saves them in .csv format.
 * Output consists of 3 .csv files: with node types, with tokens and with ASTs.
 */
class CsvAstStorage : AstStorage {

    private val tokensMap: RankedIncrementalIdStorage<String> = RankedIncrementalIdStorage()
    private val nodeTypesMap: RankedIncrementalIdStorage<String> = RankedIncrementalIdStorage()

    private val rootsPerEntity: MutableMap<String, Node> = HashMap()

    override fun store(root: Node, label: String) {
        for (node in root.preOrder()) {
            tokensMap.record(normalizeToken(node.getToken(), DEFAULT_TOKEN))
            nodeTypesMap.record(node.getTypeLabel())
        }
        rootsPerEntity[label] = root
    }

    override fun save(directoryPath: String) {
        File(directoryPath).mkdirs()
        dumpTokenStorage(File("$directoryPath/tokens.csv"))
        dumpNodeTypesStorage(File("$directoryPath/node_types.csv"))

        dumpAsts(File("$directoryPath/asts.csv"))
    }

    private fun dumpTokenStorage(file: File) {
        dumpIdStorageToCsv(tokensMap, "token", tokenToCsvString, file)
    }

    private fun dumpNodeTypesStorage(file: File) {
        dumpIdStorageToCsv(nodeTypesMap, "node_type", nodeTypeToCsvString, file)
    }

    private fun dumpAsts(file: File) {
        val lines = mutableListOf("id,ast")
        rootsPerEntity.forEach { id, root ->
            lines.add("$id,${astString(root)}")
        }

        writeLinesToFile(lines, file)
    }

    internal fun astString(node: Node): String {
        return "${tokensMap.getId(node.getToken())} ${nodeTypesMap.getId(node.getTypeLabel())}{${
        node.getChildren().joinToString(separator = "", transform = ::astString)
        }}"
    }
}