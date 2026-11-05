-- CreateTable
CREATE TABLE "User" (
    "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "telegramId" TEXT NOT NULL,
    "firstName" TEXT NOT NULL,
    "timezone" TEXT NOT NULL DEFAULT 'Europe/Berlin',
    "newCardsPerDay" INTEGER NOT NULL DEFAULT 20,
    "retention" REAL NOT NULL DEFAULT 0.9,
    "activeDeck" TEXT NOT NULL DEFAULT 'all',
    "level" TEXT NOT NULL DEFAULT 'all',
    "direction" TEXT NOT NULL DEFAULT 'recognition',
    "autoAudio" BOOLEAN NOT NULL DEFAULT false,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- CreateTable
CREATE TABLE "Deck" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "title" TEXT NOT NULL,
    "position" INTEGER NOT NULL
);

-- CreateTable
CREATE TABLE "Note" (
    "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "sourceKey" TEXT NOT NULL,
    "deckId" TEXT NOT NULL,
    "term" TEXT NOT NULL,
    "ipa" TEXT NOT NULL DEFAULT '',
    "partOfSpeech" TEXT NOT NULL DEFAULT '',
    "definition" TEXT NOT NULL DEFAULT '',
    "translation" TEXT NOT NULL,
    "level" TEXT NOT NULL DEFAULT '',
    "examples" JSONB NOT NULL,
    "forms" JSONB NOT NULL,
    "audio" JSONB NOT NULL,
    "comment" TEXT NOT NULL DEFAULT '',
    "sourceRow" INTEGER NOT NULL,
    "active" BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT "Note_deckId_fkey" FOREIGN KEY ("deckId") REFERENCES "Deck" ("id") ON DELETE RESTRICT ON UPDATE CASCADE
);

-- CreateTable
CREATE TABLE "UserCard" (
    "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "userId" INTEGER NOT NULL,
    "noteId" INTEGER NOT NULL,
    "direction" TEXT NOT NULL,
    "due" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "stability" REAL NOT NULL DEFAULT 0,
    "difficulty" REAL NOT NULL DEFAULT 0,
    "elapsedDays" INTEGER NOT NULL DEFAULT 0,
    "scheduledDays" INTEGER NOT NULL DEFAULT 0,
    "learningSteps" INTEGER NOT NULL DEFAULT 0,
    "reps" INTEGER NOT NULL DEFAULT 0,
    "lapses" INTEGER NOT NULL DEFAULT 0,
    "state" INTEGER NOT NULL DEFAULT 0,
    "lastReview" DATETIME,
    "introducedDay" TEXT,
    "suspended" BOOLEAN NOT NULL DEFAULT false,
    "version" INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT "UserCard_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User" ("id") ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT "UserCard_noteId_fkey" FOREIGN KEY ("noteId") REFERENCES "Note" ("id") ON DELETE RESTRICT ON UPDATE CASCADE
);

-- CreateTable
CREATE TABLE "StudySession" (
    "userId" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "token" TEXT NOT NULL,
    "cardId" INTEGER NOT NULL,
    "revealed" BOOLEAN NOT NULL DEFAULT false,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "StudySession_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User" ("id") ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT "StudySession_cardId_fkey" FOREIGN KEY ("cardId") REFERENCES "UserCard" ("id") ON DELETE CASCADE ON UPDATE CASCADE
);

-- CreateTable
CREATE TABLE "ReviewLog" (
    "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "userId" INTEGER NOT NULL,
    "cardId" INTEGER NOT NULL,
    "rating" INTEGER NOT NULL,
    "reviewedAt" DATETIME NOT NULL,
    "localDay" TEXT NOT NULL,
    "previous" JSONB NOT NULL,
    "fsrsLog" JSONB NOT NULL,
    "versionAfter" INTEGER NOT NULL,
    "undone" BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT "ReviewLog_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User" ("id") ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT "ReviewLog_cardId_fkey" FOREIGN KEY ("cardId") REFERENCES "UserCard" ("id") ON DELETE CASCADE ON UPDATE CASCADE
);

-- CreateTable
CREATE TABLE "Lesson" (
    "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "title" TEXT NOT NULL,
    "level" TEXT NOT NULL,
    "filename" TEXT NOT NULL,
    "markdown" TEXT NOT NULL,
    "keywords" TEXT NOT NULL,
    "active" BOOLEAN NOT NULL DEFAULT true
);

-- CreateTable
CREATE TABLE "LessonProgress" (
    "userId" INTEGER NOT NULL,
    "lessonId" INTEGER NOT NULL,
    "lastPage" INTEGER NOT NULL DEFAULT 0,
    "completedAt" DATETIME,

    PRIMARY KEY ("userId", "lessonId"),
    CONSTRAINT "LessonProgress_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User" ("id") ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT "LessonProgress_lessonId_fkey" FOREIGN KEY ("lessonId") REFERENCES "Lesson" ("id") ON DELETE RESTRICT ON UPDATE CASCADE
);

-- CreateTable
CREATE TABLE "Exercise" (
    "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "sourceKey" TEXT NOT NULL,
    "lessonId" INTEGER,
    "topic" TEXT NOT NULL,
    "level" TEXT NOT NULL,
    "kind" TEXT NOT NULL,
    "prompt" TEXT NOT NULL,
    "options" JSONB NOT NULL,
    "answers" JSONB NOT NULL,
    "explanation" TEXT NOT NULL,
    "active" BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT "Exercise_lessonId_fkey" FOREIGN KEY ("lessonId") REFERENCES "Lesson" ("id") ON DELETE SET NULL ON UPDATE CASCADE
);

-- CreateTable
CREATE TABLE "ExerciseSession" (
    "userId" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "token" TEXT NOT NULL,
    "exerciseId" INTEGER NOT NULL,
    "lessonId" INTEGER,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "ExerciseSession_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User" ("id") ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT "ExerciseSession_exerciseId_fkey" FOREIGN KEY ("exerciseId") REFERENCES "Exercise" ("id") ON DELETE RESTRICT ON UPDATE CASCADE
);

-- CreateTable
CREATE TABLE "ExerciseAttempt" (
    "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "userId" INTEGER NOT NULL,
    "exerciseId" INTEGER NOT NULL,
    "answer" TEXT NOT NULL,
    "correct" BOOLEAN NOT NULL,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "ExerciseAttempt_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User" ("id") ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT "ExerciseAttempt_exerciseId_fkey" FOREIGN KEY ("exerciseId") REFERENCES "Exercise" ("id") ON DELETE RESTRICT ON UPDATE CASCADE
);

-- CreateTable
CREATE TABLE "AudioCache" (
    "path" TEXT NOT NULL PRIMARY KEY,
    "signature" TEXT NOT NULL,
    "fileId" TEXT NOT NULL
);

-- CreateIndex
CREATE UNIQUE INDEX "User_telegramId_key" ON "User"("telegramId");

-- CreateIndex
CREATE UNIQUE INDEX "Note_sourceKey_key" ON "Note"("sourceKey");

-- CreateIndex
CREATE INDEX "Note_deckId_active_level_idx" ON "Note"("deckId", "active", "level");

-- CreateIndex
CREATE INDEX "UserCard_userId_direction_suspended_due_idx" ON "UserCard"("userId", "direction", "suspended", "due");

-- CreateIndex
CREATE UNIQUE INDEX "UserCard_userId_noteId_direction_key" ON "UserCard"("userId", "noteId", "direction");

-- CreateIndex
CREATE UNIQUE INDEX "StudySession_token_key" ON "StudySession"("token");

-- CreateIndex
CREATE INDEX "ReviewLog_userId_localDay_undone_idx" ON "ReviewLog"("userId", "localDay", "undone");

-- CreateIndex
CREATE UNIQUE INDEX "Lesson_filename_key" ON "Lesson"("filename");

-- CreateIndex
CREATE UNIQUE INDEX "Exercise_sourceKey_key" ON "Exercise"("sourceKey");

-- CreateIndex
CREATE INDEX "Exercise_lessonId_active_idx" ON "Exercise"("lessonId", "active");

-- CreateIndex
CREATE UNIQUE INDEX "ExerciseSession_token_key" ON "ExerciseSession"("token");

-- CreateIndex
CREATE INDEX "ExerciseAttempt_userId_exerciseId_idx" ON "ExerciseAttempt"("userId", "exerciseId");
