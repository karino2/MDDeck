package io.github.karino2.mddeck

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import java.io.BufferedWriter
import java.io.FileInputStream
import java.io.OutputStreamWriter
import java.util.Date


// similar to DocumentFile, but store metadata at first query.
data class FastFile(val uri: Uri, val name: String, val lastModified: Long, val mimeType: String, val size: Long, val resolver: ContentResolver) {
    companion object {
        private fun getLong(cur: Cursor, columnName: String) : Long {
            val index = cur.getColumnIndex(columnName)
            if (cur.isNull(index))
                return 0L
            return cur.getLong(index)
        }

        private fun getString(cur: Cursor, columnName: String) : String {
            val index = cur.getColumnIndex(columnName)
            if (cur.isNull(index))
                return ""
            return cur.getString(index)
        }

        private fun fromCursor(
            cur: Cursor,
            uri: Uri,
            resolver: ContentResolver
        ): FastFile {
            val disp = getString(cur, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val lm = getLong(cur, DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            val mimeType = getString(cur, DocumentsContract.Document.COLUMN_MIME_TYPE)
            val size = getLong(cur, DocumentsContract.Document.COLUMN_SIZE)
            val file = FastFile(uri, disp, lm, mimeType, size, resolver)
            return file
        }

        fun listFiles(resolver: ContentResolver, parent: Uri) : Sequence<FastFile> {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parent, DocumentsContract.getDocumentId(parent))
            val cursor = resolver.query(childrenUri, null,
                null, null, null, null) ?: return emptySequence()

            return sequence {
                cursor.use {cur ->
                    while(cur.moveToNext()) {
                        val docId = cur.getString(0)
                        val uri = DocumentsContract.buildDocumentUriUsingTree(parent, docId)

                        yield(fromCursor(cur, uri, resolver))
                    }
                }
            }
        }

        // Similar to DocumentFile:fromTreeUri.
        // treeUri is Intent#getData() of ACTION_OPEN_DOCUMENT_TREE
        fun fromTreeUri(context: Context, treeUri: Uri) : FastFile {
            val docId = (if(DocumentsContract.isDocumentUri(context, treeUri)) DocumentsContract.getDocumentId(treeUri) else DocumentsContract.getTreeDocumentId(treeUri))
                ?: throw IllegalArgumentException("Could not get documentUri from $treeUri")
            val treeDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId) ?: throw NullPointerException("Failed to build documentUri from $treeUri")
            val resolver = context.contentResolver
            return fromDocUri(resolver, treeDocUri) ?: throw IllegalArgumentException("Could not query from $treeUri")
        }

        fun fromDocUri(
            resolver: ContentResolver,
            treeDocUri: Uri
        ) : FastFile? {
            val cursor = resolver.query(
                treeDocUri, null,
                null, null, null, null
            ) ?: return null
            cursor.use { cur ->
                if (!cur.moveToFirst())
                    return null

                return fromCursor(cur, treeDocUri, resolver)
            }
        }

    }
    val isDirectory : Boolean
        get() = DocumentsContract.Document.MIME_TYPE_DIR == mimeType

    val isFile: Boolean
        get() = !(isDirectory || mimeType == "")

    fun readText()  = resolver.openFileDescriptor(uri, "r")!!.use {desc->
        val fis = FileInputStream(desc.fileDescriptor)
        fis.bufferedReader().use { it.readText() }
    }

    fun writeText(content: String) = resolver.openOutputStream(uri, "wt").use {
        val writer = BufferedWriter(OutputStreamWriter(it))
        writer.use {
            writer.write(content)
        }
    }


    //
    //  funcs below are for directory only
    //

    fun createFile(fileMimeType: String, fileDisplayName: String) : FastFile? {
        return DocumentsContract.createDocument(resolver, uri, fileMimeType, fileDisplayName) ?.let {
            //  this last modified might be slight different to real file lastModified, but I think it's not big deal.
            FastFile(it, fileDisplayName, (Date()).time, fileMimeType, 0, resolver)
        }
    }

    fun listFiles() =  listFiles(resolver, uri)


    fun findFile(targetDisplayName: String) = listFiles().find { it.name == targetDisplayName }

    fun createDirectory(displayName: String): FastFile? {
        val resUri = DocumentsContract.createDocument(resolver, uri, DocumentsContract.Document.MIME_TYPE_DIR, displayName) ?: return null
        return fromDocUri(resolver, resUri)
    }

    fun ensureDirectory(displayName: String) : FastFile? {
        return findFile(displayName) ?: createDirectory(displayName)
    }

    val isEmpty : Boolean
        get(){
            if (!isFile)
                return false
            return 0L == size
        }

}

/*
   pat is assumed to be date related filename such as
   "2023", "01", "31", "14872689.md" etc.

   Sorted for newer comes first.
 */
fun FastFile.listFiles(pat: Regex) = listFiles()
    .filter { pat.matches(it.name) }
    .sortedBy { it.name }
    .toList()
    .asReversed()

// ファイル一つを表す。マークダウン一つ。
data class MdCell(val dt: Date, val src: String) {
    companion object {
        fun fromFile(file: FastFile) : MdCell {
            // extension is ".md"
            val dtname = file.name.substring(0, file.name.length - 3)
            val date = Date(dtname.toLong())
            return MdCell(date, file.readText())
        }
    }

}

class DayDir(val dir: FastFile) {
    private val mdPat = "^[0-9]+.md$".toRegex()

    fun listMdFiles() = dir.listFiles(mdPat)
    fun listMds() = listMdFiles().map { MdCell.fromFile(it) }
}

class MonthDir(val dir: FastFile) {
    private val dayPat = "^[0-9][0-9]$".toRegex()
    fun listDays() = dir.listFiles(dayPat).map { DayDir(it) }

}

class YearDir(val dir: FastFile) {
    private val monthPat = "^[0-9][0-9]$".toRegex()
    fun listMonths() = dir.listFiles(monthPat).map { MonthDir(it) }
}

class RootDir(val dir: FastFile) {
    private val yearPat = "^[0-9][0-9][0-9][0-9]$".toRegex()

    fun listYears() = dir.listFiles(yearPat).map { YearDir(it) }

    data class CellPath(val yearDir: FastFile, val monthDir: FastFile, val dayDir: FastFile, val dispName: String) {
        fun createFile() : FastFile {
            return dayDir.createFile("text/markdown", dispName ) ?: throw Exception("Can't create file: ${dispName}")
        }

        fun findFile() : FastFile {
            return dayDir.findFile(dispName) ?: throw Exception("${dispName} does not exists")
        }
    }

    fun dateToCellPath(dt: Date) : CellPath {
        val year = (1900+dt.year).toString()
        val month = "%02d".format(dt.month+1)
        val day = "%02d".format(dt.date)

        val yearDir = dir.ensureDirectory(year) ?: throw Exception("Can't create year directory: $year")
        val monthDir = yearDir.ensureDirectory(month) ?: throw Exception("Can't create month directory: $month")
        val dayDir = monthDir.ensureDirectory(day) ?: throw Exception("Can't create day directory: $day")
        val dispname = "${dt.time}.md"

        return CellPath(yearDir, monthDir, dayDir, dispname)
    }


    fun saveMd(md: MdCell) : FastFile {
        val cellPath = dateToCellPath(md.dt)

        val file = cellPath.createFile()
        file.writeText(md.src)
        return file
    }

    fun updateMd(md: MdCell) : FastFile {
        val cellPath = dateToCellPath(md.dt)

        val file = cellPath.findFile()
        file.writeText(md.src)
        return file
    }

    fun listMdFiles() = sequence {
        listYears().forEach { yearDir ->
            yearDir.listMonths().forEach { monthDir->
                monthDir.listDays().forEach { dayDir->
                    dayDir.listMdFiles().forEach {
                        yield(it)
                    }
                }
            }
        }
    }

}

fun List<MdCell>.appendHead(newMdCell:MdCell) : List<MdCell> {
    val newMdCells = mutableListOf(newMdCell)
    newMdCells.addAll(this)
    return newMdCells
}

fun List<MdCell>.update(newCell: MdCell) = this.replace(newCell.dt, newCell.src)

fun List<MdCell>.replace(dt: Date, newText: String): List<MdCell> {
    return this.map { if(it.dt == dt) { MdCell(dt, newText) } else { it } }
}

/*
private fun List<Block>.deleteBlock(id: Int): List<Block> {
    return this.filter { it.id != id }
}
*/
