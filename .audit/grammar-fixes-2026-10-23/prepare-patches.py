from pathlib import Path
import collections
import itertools
import json
import re
import sys
import openpyxl

DIR = Path(__file__).resolve().parent
wb = openpyxl.load_workbook(DIR / 'grammar_tasks.original.xlsx', read_only=True, data_only=True)
rows = list(wb.worksheets[0].values)
tasks = {int(r[0]): dict(zip(rows[0], r), row=i) for i,r in enumerate(rows[1:],2)}
patches = {}
def set_field(n, field, value):
    if tasks[n][field] != value:
        col = rows[0].index(field) + 1
        cell = f'{openpyxl.utils.get_column_letter(col)}{tasks[n]["row"]}'
        old = patches[cell]['old'] if cell in patches else tasks[n][field]
        patches[cell] = dict(cell=cell, id=n, field=field, old=old, value=value)
        tasks[n][field] = value
def prompt(n, value): set_field(n, 'Задание', value)
def answer(n, value): set_field(n, 'Правильный ответ', value)
def explanation(n, value): set_field(n, 'Пояснение', value)

if '--scan' in sys.argv:
    for n,t in tasks.items():
        if t['Тип'] != 'ввод': continue
        parts = re.split(r'_{3,}', t['Задание'])
        if len(parts) == 2:
            subject = re.search(r'\b(I|you|he|she|it|we|they|there)\s*$',parts[0],re.I)
            if subject:
                bad = [a for a in t['Правильный ответ'].split(' / ') if re.match(re.escape(subject[1])+r"(?:\s|['’])",a,re.I)]
                if bad: print(n, 'duplicate subject', bad, '|',t['Задание'])
    sys.exit()

PROMPTS = {
802:'Вставь объектную форму местоимения she: This present is for ___.',
822:'Сообщи о наличии кафе с there is или there are: ___ a good café on this street.',
830:'Дополни словами some и advice: Can you give me ___?',
839:'Вырази принадлежность с of: the windows; the house. Напиши словосочетание целиком.',
841:'Вставь притяжательный определитель для you: Is this ___ umbrella?',
842:'Вставь самостоятельную притяжательную форму для I: No, it isn\'t ___.',
847:'Вставь this или these о предметах рядом: ___ shoes are new.',
848:'Вставь that или those о предмете вдали: Look at ___ bird on the roof!',
855:'Поставь thousand в нужную форму: ___ of fans came to the stadium.',
871:'Вставь school с артиклем, если он нужен. Мама пришла как посетитель в конкретную школу: Mum went to ___ to talk to the teacher.',
873:'Вставь be в Present Simple: The keys to the car ___ on the table.',
874:'Вставь like в Present Simple: Everyone ___ this song.',
875:'Вставь be в Present Simple: Maths ___ my favourite subject.',
876:'Вставь work в Present Simple: My mum and dad ___ from home.',
877:'Вставь be в Present Simple: The quality of these products ___ excellent.',
889:'Вставь what или which для вопроса о выборе: ___ colour do you prefer — red or blue?',
924:'Составь вежливую просьбу с I would like; сокращение допустимо: ___ a glass of water, please.',
925:'Скажи «Я люблю», используя I и like: ___ reading detective stories. Это моё хобби.',
935:'Вставь snow в Present Continuous: Look! It ___.',
936:'Вставь подходящую форму not / understand: I ___ this rule.',
937:'Вставь подходящую форму belong: This bag ___ to Anna.',
938:'Вставь have в Present Continuous: We ___ dinner at the moment.',
939:'Вставь think в значении «считать, полагать»: I ___ she\'s right.',
940:'Вставь think в Present Continuous: Be quiet! I ___.',
942:'Вставь подходящую форму sleep: Shh! The baby ___.',
943:'Вставь live в Present Simple: My grandparents ___ in a village. Это их постоянное место жительства.',
944:'Вставь stay в Present Continuous для временной ситуации: I ___ at a hotel until my flat is ready.',
945:'Построй вопрос о привычке с you и go: How often ___ to the gym?',
946:'Вставь think в значении «считать, полагать»: I ___ you\'re right.',
969:'Вставь used to или would о прошлом состоянии: When I was a child, I ___ have long hair.',
970:'Вставь used to или would о повторявшемся действии: Every summer, Grandad ___ take us fishing.',
973:'Вставь подходящую форму get: It took me months to ___ used to the climate.',
974:'Используй be used to в настоящем: Don\'t worry, she ___ working at night. Она уже привыкла к ночной работе.',
975:'Вставь used to о прежней привычке: I ___ drink milk, but now I don\'t.',
981:'Вставь been или gone: Tom isn\'t here. He\'s ___ to the bank. Он ещё не вернулся.',
984:'Вставь подходящую форму finish: I ___ the report an hour ago.',
986:'Вставь подходящую форму live: She ___ in Paris from 2010 to 2015.',
988:'Вставь not / have в Present Perfect: We ___ any rain so far this month.',
999:'Вставь наречие «уже»: — Do you want to see this film? — No, I\'ve ___ seen it.',
1000:'Вставь наречие «когда-либо»: Have you ___ been to Japan?',
1004:'Вставь read в Present Perfect: I ___ three books this month.',
1005:'Вставь know в Present Perfect: She ___ him for years.',
1011:'Вставь leave в Past Perfect: When I arrived, she ___. Она ушла до моего прихода, и мы так и не увиделись.',
1012:'Вставь answer в Past Simple для следующего события: When the phone rang, I ___ it.',
1016:'Вставь know в Past Perfect: By 2010, he ___ her for ten years.',
1017:'Вставь work в Past Perfect Continuous: When I met him, he ___ there for a year.',
1018:'Вставь write в Past Perfect: She ___ five letters by noon.',
1031:'Используй will для решения прямо сейчас: — We\'ve run out of bread. — Oh, I ___ buy some.',
1032:'Используй be going to для принятого намерения: I\'ve decided — I ___ change jobs.',
1033:'Используй be going to для прогноза по видимым признакам: Look out! You ___ fall!',
1037:'Вставь start в Present Simple для расписания: The concert ___ at 7 p.m. according to the programme.',
1038:'Вставь see в Present Continuous для договорённости: I ___ the doctor tomorrow at ten.',
1039:'Построй вопрос в Present Simple о расписании: What time ___ your plane land?',
1040:'Построй вопрос с you и do в Present Continuous о планах: What ___ this weekend?',
1050:'Используй will с call для обещания: I ___ you at eight, I promise.',
1056:'Вставь graduate в Future Perfect: By next June, she ___.',
1062:'Вставь know в Future Perfect: By then, I ___ her for ten years.',
1063:'Вставь write в Future Perfect: By Friday, I ___ the essay. К пятнице эссе будет готово.',
1069:'Используй be about to в прошлом: She ___ leave when the phone rang. Она уже собиралась уйти в следующую секунду.',
1077:'Вставь start, говоря о будущем времени начала встречи: Do you know when the meeting ___?',
1079:'Используй be able to в Past Simple для конкретного успеха: The fire spread quickly, but everyone ___ escape.',
1084:'Начни просьбу о разрешении с Can, Could или May и I: ___ use your phone?',
1085:'Начни просьбу о помощи с Can, Could или Would и you: ___ help me with this box?',
1093:'Вставь must или have to о необходимости: I really ___ stop eating sweets — I decided it myself.',
1096:'Вырази запрет с must: You ___ smoke in the hospital.',
1097:'Вырази отсутствие необходимости с have to в настоящем: Tomorrow is Sunday, so I ___ get up early.',
1098:'Используй модальное need в отрицании: You ___ bring anything — we have everything.',
1108:'Выбери must или can\'t для уверенного вывода: She\'s been working for 12 hours. She ___ be exhausted.',
1109:'Выбери must или can\'t: They say he has been abroad all week. That ___ be true — I saw him here yesterday!',
1110:'Вставь may, might или could для неуверенной догадки: I\'m not sure where he is. He ___ be in the garden.',
1115:'Вырази уверенный отрицательный вывод о прошлом с can\'t или couldn\'t: She ___ seen us — she was in Spain.',
1121:'Используй needn\'t have с hurry: We ___ — the train was late, and we waited for 20 minutes. Мы действительно спешили, но зря.',
1134:'Вставь stop в Present Simple для закономерности: If you press this button, the machine ___.',
1139:'Вставь союз «когда» об ожидаемом событии: ___ I get home, I\'ll make dinner.',
1145:'Вставь have в Second Conditional: If I ___ more money, I\'d buy a house.',
1146:'Вставь rain в First Conditional: If it ___ tomorrow, we\'ll stay home. Это вполне возможно.',
1158:'Вставь be с would в результате Mixed Conditional: If we hadn\'t missed the flight, we ___ in Rome now.',
1170:'Вставь hope или wish: I ___ you pass your exam tomorrow. Это реальная надежда на будущее.',
1193:'Допиши перфектный пассивный инфинитив с be: The painting is thought ___ stolen in 1990.',
1212:'Вставь tell в Past Simple: She ___ me the truth.',
1249:'Вставь подходящую форму see: I\'ll never forget ___ the ocean for the first time.',
1250:'Вставь подходящую форму lock: Did you remember ___ the door? Спрашиваем, не забыл ли человек выполнить задачу.',
1251:'Вставь подходящую форму drink: He stopped ___ coffee because of his heart. Он прекратил пить кофе.',
1252:'Вставь подходящую форму buy: We stopped ___ petrol on the way. Остановились, чтобы купить бензин.',
1253:'Вставь подходящую форму drink: If you can\'t sleep, try ___ warm milk. Предлагаем проверить этот способ.',
1254:'Вставь подходящую форму tell: I regret ___ him my secret. Жалею о том, что уже рассказал.',
1260:'Вставь call в начальной форме или с -ing по смыслу восприятия: I heard someone ___ my name.',
1270:'Вставь пассивную форму tell после like: I don\'t like ___ what to do.',
1295:'Вставь причастную форму show с пассивным смыслом: The film ___ last night was great.',
1306:'Вставь good или well, описывая вкус: This soup tastes ___.',
1309:'Образуй прилагательное от bore: The lecture was so ___ that I fell asleep.',
1310:'Образуй прилагательное от excite о чувстве человека: I\'m ___ about the trip.',
1311:'Образуй прилагательное от confuse о причине растерянности: These instructions are very ___.',
1312:'Образуй прилагательное от embarrass о чувстве человека: She felt ___ when she fell.',
1313:'Образуй прилагательное от tire о причине усталости: It was a ___ journey.',
1339:'Вставь absolutely или really в сочетание с сильным прилагательным: The film was ___ fantastic.',
1340:'Вставь very, really или extremely перед градуируемым прилагательным: I\'m ___ tired.',
1341:'Вставь absolutely или really в сочетание с сильным прилагательным: The hotel room was ___ tiny.',
1342:'Вставь very, really или extremely для усиления: It\'s ___ cold outside.',
1357:'Вставь good или well, описывая действие: She speaks English very ___.',
1358:'Вставь quick или quickly: He\'s a ___ learner.',
1359:'Вставь hard или hardly в значении «усердно»: You\'ve worked ___ today. Have a rest.',
1360:'Вставь hard или hardly в значении «едва»: I can ___ see anything.',
1409:'Вставь each или every о каждой из двух рук: She had a ring on ___ hand.',
1410:'Вставь each или every: ___ of the players got a medal.',
1588:'Вставь be в Past Simple: A number of questions ___ raised.',
1589:'Вставь have в Present Perfect: The number of accidents ___ fallen.',
1590:'Вставь be в Present Simple: Ten dollars ___ not enough.',
}
for n,value in PROMPTS.items(): prompt(n,value)

# These are explicit, reviewed alternatives, not automatic contraction expansion.
ANSWERS = {
811:'Yes, I am.',
813:'Do they have a car?',
815:'Do you have a pen? / Have you got a pen?',
816:'I have breakfast at seven.',
889:'Which / What',
910:"I won't tell anyone.",
911:'Yes, I am.',
924:"I'd like / I would like",
931:"am not working / 'm not working",
947:"Look! It's raining. / Look! It is raining.",
978:"have lost / 've lost",
1002:"have been learning / 've been learning",
1003:"have been repairing / 've been repairing",
970:'would / used to',
1036:"She is going to study medicine. / She's going to study medicine.",
1077:'starts / will start / is starting',
1084:'Could I / Can I / May I',
1085:'Could you / Can you / Would you',
1095:'Did you have to wait long?',
1096:"mustn't / must not",
1110:'might / may / could',
1115:"can't have / cannot have / couldn't have / could not have",
1121:"needn't have hurried / need not have hurried",
1190:'He is said to be very rich. / It is said that he is very rich.',
1191:'The thief is believed to have escaped through the window. / It is believed that the thief escaped through the window.',
1192:'It is reported that the president is ill. / The president is reported to be ill.',
1198:'Where do you have your hair cut?',
1260:'call / calling',
1270:'being told / to be told',
1339:'absolutely / really',
1340:'very / really / extremely',
1341:'absolutely / really',
1342:'extremely / very / really',
1444:"isn't it / is it not",
1446:"didn't she / did she not",
1447:"haven't they / have they not",
1448:"aren't I / am I not",
1456:'Aren\'t you tired? / Are you not tired?',
1457:"Haven't you seen this film? / Didn't you see this film? / Have you not seen this film? / Did you not see this film?",
1558:"Wouldn't it be better to wait? / Would it not be better to wait?",
1578:'Wherever you are, call me.',
}
for n,value in ANSWERS.items(): answer(n,value)

prompt(910,'Сократи will not: I will not tell anyone. Напиши предложение целиком.')
prompt(886,'Задай вопрос к месту in a bank: She works in a bank. Напиши вопрос целиком.')
prompt(887,'Задай вопрос ко времени yesterday: They arrived yesterday. Напиши вопрос целиком.')
prompt(890,'Задай вопрос к подлежащему Mike: Mike broke the window. Напиши вопрос целиком.')
prompt(891,'Задай вопрос к дополнению the window: Mike broke the window. Напиши вопрос целиком.')
prompt(893,'Задай вопрос к количеству Ten people: Ten people live in this house. Напиши вопрос целиком.')
prompt(1272,'Замени the TV местоимением it: Please turn off the TV. Напиши предложение целиком.')
prompt(1555,'Сделай просьбу вежливее с I was wondering if или I was wondering whether и could: Can you help me with my CV? Напиши ответ целиком.')
prompt(1556,'Сделай вежливее, начав с I was hoping: I want to talk to you about my salary. Напиши ответ целиком.')
prompt(1570,'Ответь на приглашение с I would love, оставив to без повтора глагола: Would you like to join us? Напиши краткий ответ, начав с I; сокращение допустимо.')
answer(1555,'I was wondering if you could help me with my CV. / I was wondering whether you could help me with my CV.')
answer(1556,'I was hoping to talk to you about my salary. / I was hoping I could talk to you about my salary. / I was hoping that I could talk to you about my salary.')

# Specify the target meaning/form where the previous distractor was also grammatical.
for n,prefix in {
237:'Передай решение прямо сейчас с will: ',
243:'Используй be going to для прогноза по видимым признакам: ',
244:'Используй be going to для принятого намерения: ',
248:'Используй Present Continuous для назначенной встречи: ',
249:'Используй Present Simple для расписания: ',
368:'Вырази пожелание изменить поведение с would: ',
445:'С сожалением сообщаем новость сейчас; используй модель regret to do: ',
}.items():
    prompt(n,prefix + tasks[n]['Задание'])
prompt(158,'О постоянном владении языками, в Present Simple: She ___ French and German.')
prompt(688,'Выбери правильное предложение о машине одного брата (не нескольких).')
explanation(688,"It's = it is; brother's — принадлежность одному брату. Brothers' обозначало бы нескольких братьев и не соответствует условию.")
explanation(889,'Which обычно выделяет выбор из ограниченного набора; what тоже грамматически возможно в таком вопросе.')
explanation(970,'Would и used to подходят для повторявшихся действий в прошлом; would особенно характерно для воспоминаний с уже заданным периодом.')
explanation(1121,"Needn't have hurried / need not have hurried — спешили, хотя необходимости не было. В условии прямо сказано, что действие произошло.")
explanation(1190,'Допустимы обе пассивные модели: He is said to be… и It is said that…; сообщение относится к настоящему.')
explanation(1191,'Побег был раньше нынешнего мнения: to have escaped. Допустима и модель It is believed that the thief escaped… .')
explanation(1192,'Допустимы обе модели: It is reported that… и The president is reported to be… .')
explanation(1260,'Call представляет услышанное действие целиком; calling — его процесс. Контекст допускает оба варианта.')
explanation(1270,'После like возможны -ing и to-инфинитив. Обе пассивные формы being told и to be told здесь допустимы.')
explanation(1448,"После I'm в обычном отрицательном хвостике — aren't I; полный формальный вариант — am I not, не are not I.")
explanation(1456,"Aren't you tired? / Are you not tired? — в полной отрицательной форме not стоит после подлежащего you.")
explanation(1457,'В зависимости от прошлого контекста допустимы Present Perfect и Past Simple. В полных отрицательных вопросах not следует после you.')
explanation(1558,"Wouldn't it be better to wait? / Would it not be better to wait? — полное not стоит после подлежащего it.")
explanation(1339,'Absolutely и really сочетаются с сильным прилагательным fantastic.')
explanation(1340,'Very, really и extremely могут усиливать градуируемое прилагательное tired.')
explanation(1341,'Absolutely и really употребляются с сильным прилагательным tiny; выбираем из слов, заданных в условии.')
explanation(1342,'Very, really и extremely могут усиливать cold; выбираем из слов, заданных в условии.')
for n,full in {
858:'It is a three-hour flight.',
1128:'I would rather walk.',
1129:'I would rather go by train than fly.',
1175:'It is time for us to start.',
1176:'It is time we went home.',
1177:'It is high time you found a job.',
1330:'It is getting darker and darker.',
1480:'It is windy today.',
1482:'It is difficult for me to understand his accent.',
1484:'It is raining again.',
1495:'He is six years old. He is a six-year-old boy.',
1497:'It is a three-day trip.',
1518:'She is in hospital.',
1525:'Incredible as it may seem, it is true.',
1557:'I would appreciate it if you could send me the file today.',
1559:'I would appreciate it if you could call me back.',
1570:'I would love to.',
1602:'It is no use arguing with him.',
}.items():
    answer(n,tasks[n]['Правильный ответ']+' / '+full)

# A keyboard-friendly explicit marker for an omitted article, including multiple gaps.
for n,t in tasks.items():
    if t['Тип']=='ввод' and '—' in t['Правильный ответ']:
        values=t['Правильный ответ'].splitlines() if len(re.findall(r'_{3,}',t['Задание']))>1 else t['Правильный ответ'].split(' / ')
        revised=[]
        for value in values:
            revised.extend([value,value.replace('—','-')])
        answer(n, ('\n' if len(re.findall(r'_{3,}',t['Задание']))>1 else ' / ').join(dict.fromkeys(revised)))
        prompt(n,t['Задание']+' Если слово не нужно, введи - (дефис).')

# Propose only unambiguous, ordinary contractions; review the generated list before export.
V3 = r'(?:[a-z]+ed|been|gone|done|seen|known|had|left|lost|written|read|eaten|taken|given|got|gotten|made|met|bought|brought|taught|thought|told|found|won|come|become|run|broken|spoken|stolen|forgotten|driven|fallen|flown|grown|built|caught|sold|sent|spent|heard|felt|kept|paid|put|set|cut|hurt|let|shut|sat|slept|stood|understood|worn|chosen|risen|sung|swum|rung|begun)\b'
positive = [
    (r'\b(I) am(?=\s+[A-Za-z])',r"\1'm"),
    (r'\b(you|we|they) are(?=\s+[A-Za-z])',r"\1're"),
    (r'\b(he|she|it|that|there|what|who) is(?=\s+[A-Za-z])',r"\1's"),
    (r'\b(I|you|he|she|it|we|they) will(?=\s+[A-Za-z])',r"\1'll"),
    (r'\b(I|you|he|she|it|we|they) would(?=\s+[A-Za-z])',r"\1'd"),
    (r'\b(I|you|we|they) have(?=\s+'+V3+r')',r"\1've"),
    (r'\b(he|she|it) has(?=\s+'+V3+r')',r"\1's"),
    (r'\b(I|you|he|she|it|we|they) had(?=\s+(?:'+V3+r'|better\b))',r"\1'd"),
]
unambiguous = [(r"\b(I)'m\b",r'\1 am'),(r"\b(you|we|they)'re\b",r'\1 are'),
    (r"\b(I|you|we|they)'ve\b",r'\1 have'),(r"\b(I|you|he|she|it|we|they)'ll\b",r'\1 will')]
negative={'do not':"don't",'does not':"doesn't",'did not':"didn't",'is not':"isn't",'are not':"aren't",'was not':"wasn't",'were not':"weren't",'have not':"haven't",'has not':"hasn't",'had not':"hadn't",'will not':"won't",'would not':"wouldn't",'could not':"couldn't",'should not':"shouldn't",'must not':"mustn't",'need not':"needn't",'cannot':"can't"}
proposed=[]
for n,t in tasks.items():
    if t['Тип']!='ввод' or t['Номер темы'] in (21,123): continue
    if re.search(r'Раскрой|Сократи will not|сокращённо|сокращение\)',t['Задание'],re.I): continue
    gap_count=len(re.findall(r'_{3,}',t['Задание']))
    if gap_count>1: continue
    values=t['Правильный ответ'].split(' / ')
    expanded=list(values)
    before = re.split(r'_{3,}',t['Задание'])[0] if gap_count==1 else ''
    gap_subject = re.search(r'\b(I|you|he|she|it|we|they|there)\s*$',before,re.I)
    for _ in range(4):
        for value in list(expanded):
            for pattern,replacement in positive+unambiguous:
                for match in re.finditer(pattern,value,re.I):
                    result=value[:match.start()]+match.expand(replacement)+value[match.end():]
                    if result!=value and result not in expanded: expanded.append(result)
            for full,short in negative.items():
                for a,b in ((full,short),(short,full)):
                    result=re.sub(r'\b(I|you|he|she|it|we|they) '+re.escape(a)+r'\b',lambda m:m[1]+' '+b,value,flags=re.I)
                    if gap_subject:
                        result=re.sub(r'^'+re.escape(a)+r'\b',b,result,flags=re.I)
                    if result!=value and result not in expanded: expanded.append(result)
            if gap_subject:
                for full,short in [('am',"'m"),('is',"'s"),('are',"'re"),('will',"'ll"),('would',"'d"),('have',"'ve"),('has',"'s"),('had',"'d")]:
                    if full in ('have','has','had') and not re.match(r'^'+full+r'\s+'+V3,value,re.I): continue
                    if full=='am' and gap_subject[1].lower()!='i': continue
                    result=re.sub(r'^'+full+r'(?=\s+[A-Za-z])',short,value,flags=re.I)
                    if result!=value and result not in expanded: expanded.append(result)
    if len(expanded)>len(values):
        proposed.append(dict(id=n,original=values,added=expanded[len(values):]))
        answer(n,' / '.join(expanded))

(DIR/'contraction-proposals.json').write_text(json.dumps(proposed,ensure_ascii=False,indent=2),encoding='utf8')

(DIR/'patches.json').write_text(json.dumps(list(patches.values()),ensure_ascii=False,indent=2),encoding='utf8')
(DIR/'tasks.updated.json').write_text(json.dumps(list(tasks.values()),ensure_ascii=False,indent=2),encoding='utf8')
print(f'{len(patches)} changed cells in {len({p["id"] for p in patches.values()})} tasks')
