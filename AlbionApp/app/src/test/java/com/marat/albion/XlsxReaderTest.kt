package com.marat.albion

import com.marat.albion.data.content.XlsxReader
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class XlsxReaderTest {
    @Test fun prefixedNamespacesAndRelationshipOrderAreSupported() {
        val main = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
        val rel = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
        val files = mapOf(
            "xl/workbook.xml" to """<x:workbook xmlns:x="$main" xmlns:r="$rel"><x:sheets><x:sheet name="First" sheetId="2" r:id="chosen"/></x:sheets></x:workbook>""",
            "xl/_rels/workbook.xml.rels" to """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="chosen" Target="worksheets/sheet2.xml"/></Relationships>""",
            "xl/sharedStrings.xml" to """<x:sst xmlns:x="$main"><x:si><x:t>Shared text</x:t></x:si></x:sst>""",
            "xl/worksheets/sheet1.xml" to """<worksheet xmlns="$main"><sheetData><row r="1"><c r="A1" t="inlineStr"><is><t>Wrong sheet</t></is></c></row></sheetData></worksheet>""",
            "xl/worksheets/sheet2.xml" to """<x:worksheet xmlns:x="$main"><x:sheetData><x:row r="1"><x:c r="A1" t="inlineStr"><x:is><x:t>Inline text</x:t></x:is></x:c><x:c r="C1" t="s"><x:v>0</x:v></x:c></x:row></x:sheetData></x:worksheet>""",
        )
        val bytes = ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip -> files.forEach { (name, text) ->
                zip.putNextEntry(ZipEntry(name)); zip.write(("\uFEFF<?xml version=\"1.0\" encoding=\"utf-8\"?>" + text).toByteArray()); zip.closeEntry()
            } }
        }.toByteArray()
        assertEquals(listOf(listOf("Inline text", "", "Shared text")), XlsxReader.read(bytes.inputStream()))
    }
}
