export const DECKS = [
  { id: "words", title: "Слова", file: "words.xlsx", audioDir: "words" },
  { id: "idioms", title: "Идиомы", file: "idiomas.xlsx", audioDir: "idiomas" },
  {
    id: "phrasal_verbs",
    title: "Фразовые глаголы",
    file: "phrasal_verbs.xlsx",
    audioDir: "phrasal_verbs",
  },
  {
    id: "irregular_verbs",
    title: "Неправильные глаголы",
    file: "irregular_verbs.xlsx",
    audioDir: "irregular_verbs",
  },
  {
    id: "collocations",
    title: "Коллокации",
    file: "collocations.xlsx",
    audioDir: "collocations",
  },
] as const;

export const LEVELS = ["A1", "A2", "B1", "B2", "C1", "C2"] as const;

export type VerbForm = {
  label: string;
  text: string;
  ipa: string;
  examples: string[];
  audio: string[];
};

export type ImportedNote = {
  sourceKey: string;
  deckId: string;
  term: string;
  ipa: string;
  partOfSpeech: string;
  definition: string;
  translation: string;
  level: string;
  examples: string[];
  forms: VerbForm[];
  audio: string[];
  comment: string;
  sourceRow: number;
};

export type ImportedLesson = {
  id: number;
  title: string;
  level: string;
  filename: string;
  markdown: string;
  keywords: string;
};

export type ImportedExercise = {
  sourceKey: string;
  lessonId: number | null;
  topic: string;
  level: string;
  kind: string;
  prompt: string;
  options: string[];
  answers: string[];
  explanation: string;
};

export type Content = {
  notes: ImportedNote[];
  lessons: ImportedLesson[];
  exercises: ImportedExercise[];
  warnings: string[];
};
