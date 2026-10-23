import com.marat.albion.data.db.GrammarTaskEntity;
import com.marat.albion.data.games.AnswerCheck;
import com.marat.albion.data.games.Verdict;

public class GrammarReviewProbe {
    private static void check(int id, int topic, String prompt, String answer, String submission) {
        GrammarTaskEntity task = new GrammarTaskEntity("gt:" + id, 0, "", topic, "", "input", prompt, "", answer, "");
        Verdict verdict = AnswerCheck.INSTANCE.checkGrammar(submission, task.getAcceptedAnswers(), task.getRequiresExactWriting());
        System.out.println("#" + id + ": " + submission + " => " + verdict);
    }
    public static void main(String[] args) {
        check(936, 26, "Впиши подходящую форму: I ___ this rule.", "don't understand / do not understand", "understand");
        check(947, 27, "Исправь ошибку: Look! It rains. Напиши ответ целиком.", "Look! It's raining.", "Look! It is raining.");
        check(1036, 42, "Исправь ошибку: She going to study medicine. Напиши ответ целиком.", "She is going to study medicine.", "She's going to study medicine.");
        check(1190, 69, "Перефразируй: People say that he is very rich. Напиши ответ целиком.", "He is said to be very rich.", "It is said that he is very rich.");
        check(1192, 69, "Перефразируй: They report that the president is ill. Напиши ответ целиком.", "It is reported that the president is ill.", "The president is reported to be ill.");
        check(1448, 113, "Добавь хвостик: I'm right, ___?", "aren't I / are not I", "are not I");
        check(1456, 115, "Переведи: «Разве ты не устал?» Напиши ответ целиком.", "Aren't you tired? / Are not you tired?", "Are you not tired?");
        check(1558, 133, "Сделай вежливее с отрицательным вопросом: It's better to wait. Напиши ответ целиком.", "Wouldn't it be better to wait? / Would not it be better to wait?", "Would it not be better to wait?");
    }
}
