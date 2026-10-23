import fs from 'node:fs/promises';
import path from 'node:path';
import { FileBlob, SpreadsheetFile } from '@oai/artifact-tool';

const dir = path.dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1'));
const root = path.resolve(dir, '../..');
const source = path.join(dir, 'grammar_tasks.original.xlsx');
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(source));
const sheet = workbook.worksheets.getItemAt(0);
if (process.argv.includes('--inspect')) {
  console.log((await workbook.inspect({kind:'workbook,sheet,table',maxChars:1500,tableMaxRows:2,tableMaxCols:6})).ndjson);
  const preview = await workbook.render({sheetName:sheet.name,range:'F935:L942',scale:1,format:'png'});
  await fs.writeFile(path.join(dir,'before.png'),new Uint8Array(await preview.arrayBuffer()));
  console.log('Before preview saved.');
} else {
  const patches = JSON.parse(await fs.readFile(path.join(dir,'patches.json'),'utf8'));
  for (const patch of patches) {
    sheet.getRange(patch.cell).values = [[patch.value]];
  }
  workbook.recalculate();
  console.log((await workbook.inspect({kind:'region',sheetId:sheet.name,range:'F935:L942',maxChars:1800,tableMaxRows:8,tableMaxCols:7})).ndjson);
  const preview = await workbook.render({sheetName:sheet.name,range:'F935:L942',scale:1,format:'png'});
  await fs.writeFile(path.join(dir,'after.png'),new Uint8Array(await preview.arrayBuffer()));
  await (await SpreadsheetFile.exportXlsx(workbook)).save(path.join(root,'collections','grammar_tasks.xlsx'));
  console.log(`Patched ${patches.length} cells.`);
}
