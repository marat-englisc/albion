from pathlib import Path
import collections
import json
import re
import sys
import openpyxl

ROOT = Path(__file__).resolve().parents[2]
OUT = Path(__file__).resolve().parent
lessons = {}
files = {p.stem: p for p in (ROOT / 'grammar').glob('*.md')}
for name, path in files.items():
    text = path.read_text(encoding='utf-8-sig')
    match = re.match(r'(\d+) - ', name)
    if not match or int(match[1]) == 0:
        continue
    n = int(match[1])
    front = text.split('---', 2)[1]
    def field(key):
        m = re.search(r'^' + key + r':[ \t]*([^\n]*)', front, re.M)
        return m[1].strip() if m else ''
    body = text.split('---', 2)[2]
    theory = body.split('## Проверь себя')[0]
    lessons[n] = dict(n=n, file=name, level=field('level'),
        prereq=[int(v) for v in re.findall(r'\d+', field('prerequisites'))],
        keywords=field('keywords'), text=text, theory=theory.strip())

wb = openpyxl.load_workbook(ROOT / 'collections' / 'grammar_tasks.xlsx', read_only=True, data_only=True)
rows = list(wb.worksheets[0].values)
headers = rows[0]
tasks = [dict(zip(headers, row), row=i) for i, row in enumerate(rows[1:], 2) if row[0] is not None]
(OUT / 'tasks.json').write_text(json.dumps(tasks, ensure_ascii=False, indent=2), encoding='utf-8')

if len(sys.argv) > 1 and sys.argv[1] == 'theory':
    low, high = map(int, sys.argv[2:4])
    for n in range(low, high + 1):
        lesson = lessons[n]
        print(f"\n=== {n} {lesson['level']} prerequisites={lesson['prereq']} ===")
        print(lesson['theory'])
    sys.exit()
if len(sys.argv) > 1 and sys.argv[1] == 'tasks':
    nums = set(map(int, sys.argv[2:]))
    for t in tasks:
        if t['Номер темы'] in nums:
            opts = [str(t.get(f'Вариант {i}') or '') for i in range(1,5)]
            print(f"row {t['row']} #{t['№']} topic {t['Номер темы']} {t['Тип']}: {t['Задание']} | {'; '.join(filter(None,opts))} => {t['Правильный ответ']} | {t['Пояснение']}")
    sys.exit()

broken = []
for name, path in files.items():
    for target in re.findall(r'\[\[([^\]|]+)(?:\|[^\]]+)?\]\]', path.read_text(encoding='utf-8-sig')):
        if target.split('#')[0].strip().removesuffix('\\') not in files:
            broken.append([name, target])
counts = collections.Counter(t['Номер темы'] for t in tasks)
kinds = collections.Counter(t['Тип'] for t in tasks)
bad = []
for t in tasks:
    for field in ('№','Уровень','Номер темы','Тема','Тип','Задание','Правильный ответ','Пояснение'):
        if t.get(field) is None or str(t[field]).strip() == '':
            bad.append([t['row'], 'empty', field])
    if t['Номер темы'] not in lessons:
        bad.append([t['row'], 'topic missing'])
    elif t['Уровень'] != lessons[t['Номер темы']]['level']:
        bad.append([t['row'], 'level mismatch'])
    if t['Тип'] == 'выбор':
        opts = [t.get(f'Вариант {i}') for i in range(1,5) if t.get(f'Вариант {i}')]
        if t['Правильный ответ'] not in opts:
            bad.append([t['row'], 'answer missing from options'])
        if len(set(opts)) != len(opts):
            bad.append([t['row'], 'duplicate options'])

visited, stack, cycles = set(), [], []
def visit(n):
    if n in stack:
        cycles.append(stack + [n])
        return
    if n in visited:
        return
    visited.add(n)
    stack.append(n)
    for p in lessons[n]['prereq']:
        if p in lessons:
            visit(p)
    stack.pop()
for n in lessons:
    visit(n)

md_stats = []
for n,l in lessons.items():
    theory = re.sub(r'```.*?```','',l['theory'],flags=re.S)
    words = re.findall(r"[A-Za-zА-Яа-яЁё]+(?:'[a-z]+)?", theory)
    md_stats.append(dict(n=n, theory_words=len(words), keywords=bool(l['keywords']),
        tasks=counts[n], kind_counts=dict(collections.Counter(t['Тип'] for t in tasks if t['Номер темы']==n))))

duplicates = collections.defaultdict(list)
for t in tasks:
    duplicates[(t['Тип'], t['Задание'], t['Правильный ответ'])].append(t['row'])
negative_variants = [dict(row=t['row'], id=t['№'], topic=t['Номер темы'], answer=t['Правильный ответ'])
    for t in tasks if re.search(r'\b(?:Do|Does|Did|Would|Could|Should|Will|Is|Are|Was|Were|Have|Has|Had|Can|Must) not (?:I|you|he|she|it|we|they)\b',str(t['Правильный ответ']), re.I)]
route, seen = [], set()
def route_visit(n):
    if n in seen:
        return
    seen.add(n)
    for p in lessons[n]['prereq']:
        if p in lessons:
            route_visit(p)
    route.append(n)
for n in sorted(lessons):
    route_visit(n)
summary = dict(files=len(files),lessons=len(lessons),level_counts=dict(collections.Counter(l['level'] for l in lessons.values())),
    tasks=len(tasks),kind_counts=dict(kinds),task_count_distribution=dict(collections.Counter(counts.values())),
    min_tasks=min(counts.values()),max_tasks=max(counts.values()),
    uncovered=[n for n in lessons if n not in counts], broken_wikilinks=broken,
    invalid_tasks=bad,duplicate_ids=[x for x,n in collections.Counter(t['№'] for t in tasks).items() if n>1],
    duplicate_prompt_answer_rows=[r for r in duplicates.values() if len(r)>1],
    cycles=cycles,missing_prereq=[[n,p] for n,l in lessons.items() for p in l['prereq'] if p not in lessons],
    empty_keywords=[n for n,l in lessons.items() if not l['keywords']],
    theory_words_min=min(x['theory_words'] for x in md_stats),theory_words_max=max(x['theory_words'] for x in md_stats),
    theory_words_median=sorted(x['theory_words'] for x in md_stats)[len(md_stats)//2],md_stats=md_stats,
    learning_route=route, suspicious_negative_variants=negative_variants,
    unspecified_input_prompts=sum(t['Тип']=='ввод' and str(t['Задание']).startswith('Впиши подходящую форму:') for t in tasks),
    mermaid_blocks=sum(p.read_text(encoding='utf-8-sig').count('```mermaid') for p in files.values()))
(OUT / 'structure.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({k:v for k,v in summary.items() if k not in ('md_stats','invalid_tasks','empty_keywords','learning_route','suspicious_negative_variants')},ensure_ascii=False,indent=2))
print('Level mismatch rows:', len(summary['invalid_tasks']))
print('Empty keywords:', len(summary['empty_keywords']))
print('Negative question variants requiring review:', len(negative_variants))
print('Learning route first 35:', route[:35])
