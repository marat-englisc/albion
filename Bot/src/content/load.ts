import { createHash } from "node:crypto";
import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import ExcelJS from "exceljs";
import matter from "gray-matter";
import { AudioIndex } from "./audio-index.js";
import {
  DECKS,
  type Content,
  type ImportedNote,
  type ImportedExercise,
  type VerbForm,
} from "./types.js";

const key = (...parts: string[]) =>
  createHash("sha256")
    .update(JSON.stringify(parts.map((s) => s.normalize("NFKC").trim())))
    .digest("hex");
const lower = (s: string) => s.toLowerCase().trim();

export async function readRows(filename: string): Promise<string[][]> {
  // Streaming ignores decorative comments/drawings, including the nonstandard comment
  // relationships in these source workbooks. Only cell contents are imported.
  const workbook = new ExcelJS.stream.xlsx.WorkbookReader(filename, {
    entries: "ignore",
    sharedStrings: "cache",
    hyperlinks: "ignore",
    styles: "ignore",
    worksheets: "emit",
  });
  const rows: string[][] = [];
  let sheetCount = 0;
  for await (const sheet of workbook) {
    sheetCount++;
    for await (const row of sheet) {
      if (sheetCount === 1)
        rows[row.number - 1] = Array.from({ length: row.cellCount }, (_, i) =>
          row.getCell(i + 1).text.trim(),
        );
    }
  }
  if (!sheetCount) throw new Error(`Нет листов: ${filename}`);
  return Array.from({ length: rows.length }, (_, i) => rows[i] ?? []);
}

function column(
  headers: string[],
  test: (s: string) => boolean,
  required = false,
): number {
  const i = headers.findIndex((s) => test(lower(s)));
  if (required && i < 0)
    throw new Error(`Не найдена обязательная колонка в ${headers.join(", ")}`);
  return i;
}

export function parseNotes(
  rows: string[][],
  deck: (typeof DECKS)[number],
  audio: AudioIndex,
): ImportedNote[] {
  const headers = rows[0] ?? [];
  const irregular = deck.id === "irregular_verbs";
  const ru = column(headers, (s) => s.includes("русск"), true);
  const definition = column(
    headers,
    (s) => s.includes("англ") && /значение|определение/.test(s),
    true,
  );
  const ipa = column(headers, (s) => /ipa|транскрип/.test(s));
  const pos = column(headers, (s) => /часть речи|part of speech/.test(s));
  const level = column(headers, (s) => /cefr|уровень/.test(s));
  const comment = column(headers, (s) => /примечание/.test(s));
  const notes: ImportedNote[] = [];
  for (let i = 1; i < rows.length; i++) {
    const row = rows[i]!;
    const cell = (n: number) => row[n]?.trim() ?? "";
    const term = cell(0);
    if (!term) continue;
    if (!cell(ru))
      throw new Error(`${deck.file}:${i + 1}: пустой перевод для «${term}»`);
    const examples = headers.flatMap((h, j) =>
      /пример|example/i.test(h) && cell(j) ? [cell(j)] : [],
    );
    const forms: VerbForm[] = [];
    if (irregular) {
      for (const [label, match] of [
        ["Infinitive", "инфинитив"],
        ["Past Simple", "past simple"],
        ["Past Participle", "participle"],
      ] as const) {
        const form = column(
          headers,
          (s) => s.includes(match) && !/пример|ipa/.test(s),
          true,
        );
        const trans = column(
          headers,
          (s) => s.includes(match) && s.includes("ipa"),
        );
        forms.push({
          label,
          text: cell(form),
          ipa: cell(trans),
          examples: headers.flatMap((h, j) =>
            lower(h).includes(match) && /пример/i.test(h) && cell(j)
              ? [cell(j)]
              : [],
          ),
          audio: audio.verb(cell(form), label !== "Infinitive"),
        });
      }
    }
    notes.push({
      sourceKey: key(deck.id, term, cell(pos), cell(definition)),
      deckId: deck.id,
      term,
      ipa: cell(ipa),
      partOfSpeech: cell(pos),
      definition: cell(definition),
      translation: cell(ru),
      level: cell(level).toUpperCase(),
      examples,
      forms,
      audio: irregular ? (forms[0]?.audio ?? []) : audio.find(term, cell(pos)),
      comment: cell(comment),
      sourceRow: i + 1,
    });
  }
  return notes;
}

export function parseExercises(rows: string[][]): ImportedExercise[] {
  const headers = rows[0] ?? [];
  const col = (test: (s: string) => boolean) => column(headers, test, true);
  const id = col((s) => /^(№|id|номер)$/.test(s));
  const level = col((s) => /уровень|level/.test(s));
  const lesson = col((s) => s.startsWith("номер темы"));
  const topic = col((s) => /^(тема|topic)$/.test(s));
  const kind = col((s) => /^(тип|type)/.test(s));
  const prompt = col((s) => /^(задание|предложение)/.test(s));
  const answer = col((s) => /^(правильн|answer)/.test(s));
  const explanation = col((s) => /^(пояснени|explanation)/.test(s));
  return rows.slice(1).flatMap((row, i) => {
    const cell = (n: number) => row[n]?.trim() ?? "";
    if (!cell(prompt)) return [];
    if (!cell(answer))
      throw new Error(`grammar_tasks.xlsx:${i + 2}: пустой ответ`);
    const input = /^(ввод|input)/i.test(cell(kind));
    const options = headers.flatMap((h, j) =>
      /^(вариант|option)/i.test(h) && cell(j) ? [cell(j)] : [],
    );
    if (!input && options.length < 2)
      throw new Error(`grammar_tasks.xlsx:${i + 2}: недостаточно вариантов`);
    const rawAnswer = cell(answer);
    const gaps = (cell(prompt).match(/_{2,}/g) ?? []).length;
    // For one gap, slash separates accepted variants (is / 's). For several gaps
    // it joins their answers (Have / eaten). Newlines and | always separate variants.
    const answers = rawAnswer
      .split(input && gaps < 2 ? /\s+\/\s+|\s*\|\s*|\r?\n/ : /\s*\|\s*|\r?\n/)
      .map((s) => s.trim())
      .filter(Boolean);
    if (!input && !options.some((o) => answers.includes(o))) {
      throw new Error(
        `grammar_tasks.xlsx:${i + 2}: ответ отсутствует среди вариантов: ${rawAnswer}`,
      );
    }
    return [
      {
        sourceKey: `grammar:${cell(id) || key(cell(prompt), cell(topic))}`,
        lessonId: Number.parseInt(cell(lesson)) || null,
        topic: cell(topic),
        level: cell(level).toUpperCase(),
        kind: input ? "input" : "choice",
        prompt: cell(prompt),
        options: input ? [] : options,
        answers,
        explanation: cell(explanation),
      },
    ];
  });
}

export async function loadContent(root: string): Promise<Content> {
  const result: Content = {
    notes: [],
    lessons: [],
    exercises: [],
    warnings: [],
  };
  for (const deck of DECKS) {
    const files = (
      await readdir(path.join(root, "audio", deck.audioDir))
    ).filter((f) => /\.mp3$/i.test(f));
    const notes = parseNotes(
      await readRows(path.join(root, "collections", deck.file)),
      deck,
      new AudioIndex(files, deck.audioDir),
    );
    const seen = new Set<string>();
    for (const note of notes) {
      if (seen.has(note.sourceKey)) {
        result.warnings.push(
          `${deck.file}:${note.sourceRow}: дубликат «${note.term}»`,
        );
        continue;
      }
      seen.add(note.sourceKey);
      result.notes.push(note);
    }
    const missing = notes.filter((n) => !n.audio.length).length;
    if (missing)
      result.warnings.push(`${deck.title}: ${missing} карточек без аудио`);
  }
  const files = (await readdir(path.join(root, "grammar")))
    .filter((name) => /^(?:(?!000)\d{3}\s|Справочник).*\.md$/i.test(name))
    .sort();
  for (const filename of files) {
    const source = await readFile(path.join(root, "grammar", filename), "utf8");
    const { data, content } = matter(source);
    const id = /^\d{3}/.test(filename)
      ? Number.parseInt(filename)
      : 1000 + Number.parseInt(key(filename).slice(0, 6), 16);
    const title =
      /^#\s+(.+)$/m.exec(content)?.[1] ??
      filename.replace(/^\d+\s*-\s*|\.md$/g, "");
    if (result.lessons.some((l) => l.id === id))
      throw new Error(`Коллизия ID урока: ${filename}`);
    result.lessons.push({
      id,
      title,
      level: String(data.level ?? ""),
      filename,
      markdown: content.trim(),
      keywords: Array.isArray(data.keywords)
        ? data.keywords.join(", ")
        : String(data.keywords ?? ""),
    });
  }
  if (!result.lessons.length)
    throw new Error("В grammar/ не найдены уроки 001…999");
  const ids = new Set(result.lessons.map((l) => l.id));
  result.exercises = parseExercises(
    await readRows(path.join(root, "collections", "grammar_tasks.xlsx")),
  );
  const exerciseKeys = new Set<string>();
  for (const exercise of result.exercises) {
    if (exerciseKeys.has(exercise.sourceKey))
      throw new Error(`Дублируется ID упражнения: ${exercise.sourceKey}`);
    exerciseKeys.add(exercise.sourceKey);
    if (exercise.lessonId && !ids.has(exercise.lessonId))
      throw new Error(
        `Не найден урок ${exercise.lessonId} для ${exercise.sourceKey}`,
      );
  }
  return result;
}
