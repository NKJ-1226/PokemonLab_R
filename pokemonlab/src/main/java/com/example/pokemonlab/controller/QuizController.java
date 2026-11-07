
package com.example.pokemonlab.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.pokemonlab.entity.Pokemon;
import com.example.pokemonlab.quiz.QuizMode;
import com.example.pokemonlab.quiz.QuizQuestion;
import com.example.pokemonlab.quiz.QuizSession;
import com.example.pokemonlab.quiz.TypeCombination;
import com.example.pokemonlab.quiz.TypeQuizGenerator;
import com.example.pokemonlab.quiz.TypeQuizQuestion;
import com.example.pokemonlab.repository.PokemonRepository;
import jakarta.servlet.http.HttpSession;

/**
 * クイズ画面の表示と進行を管理するController
 *
 * 種族値クイズ、比較クイズ、タイプクイズに対応する
 * クイズの進行状態はHTTPセッションに保存する
 */
@Controller
public class QuizController {

    /** 1回のクイズで出題する問題数 */
    private static final int QUESTION_COUNT = 10;

    /** 1問あたりの選択肢数 */
    private static final int CHOICE_COUNT = 4;

    private final PokemonRepository pokemonRepository;
    private final TypeQuizGenerator typeQuizGenerator;

    public QuizController(
            PokemonRepository pokemonRepository,
            TypeQuizGenerator typeQuizGenerator) {

        this.pokemonRepository = pokemonRepository;
        this.typeQuizGenerator = typeQuizGenerator;
    }

    /**
     * クイズのモード選択画面を表示する
     *
     * @return クイズメニューのテンプレート名
     */
    @GetMapping("/quiz")
    public String quizMenu() {
        return "quiz-menu";
    }

    /**
     * 選択したモードでクイズを開始する
     *
     * クイズモードに応じて10問を生成し、
     * HTTPセッションに進行情報を保存する
     *
     * @param mode 選択したクイズモード
     * @param generation 世代別クイズで指定する世代
     * @param httpSession HTTPセッション
     * @return 問題画面へのリダイレクト
     */
    @PostMapping("/quiz/start")
    public String startQuiz(
            @RequestParam QuizMode mode,
            @RequestParam(required = false) Integer generation,
            HttpSession httpSession) {

        // 新しいクイズセッションを作成する
        QuizSession quizSession = new QuizSession();

        quizSession.setMode(mode);
        quizSession.setGeneration(generation);
        quizSession.setCurrentIndex(0);
        quizSession.setScore(0);

        if (mode == QuizMode.TYPE_QUIZ) {

            // タイプクイズ専用の問題を生成する
            quizSession.setTypeQuestions(
                    typeQuizGenerator.generate());

        } else {

            List<QuizQuestion> questions;

            if (isComparisonMode(mode)) {

                // 高さ・重さの比較クイズを生成する
                questions = createComparisonQuestions(mode);

            } else {

                // 指定されたモードに応じた出題候補を取得する
                List<Pokemon> candidates =
                        new ArrayList<>(getCandidates(mode, generation));

                if (candidates.size() < QUESTION_COUNT) {
                    throw new IllegalStateException(
                            "10問分のポケモンを取得できませんでした");
                }

                // 出題順をランダムにする
                Collections.shuffle(candidates);

                // 正解となるポケモンを10匹選択する
                List<Pokemon> correctPokemonList =
                        new ArrayList<>(
                                candidates.subList(0, QUESTION_COUNT));

                questions = new ArrayList<>();

                for (Pokemon correctPokemon : correctPokemonList) {

                    // 正解以外のポケモンを不正解候補とする
                    List<Pokemon> wrongCandidates =
                            candidates.stream()
                                    .filter(pokemon ->
                                            !pokemon.getId().equals(
                                                    correctPokemon.getId()))
                                    .collect(Collectors.toCollection(
                                            ArrayList::new));

                    Collections.shuffle(wrongCandidates);

                    // 正解1匹と不正解3匹の選択肢を作成する
                    List<Integer> choiceIds = new ArrayList<>();
                    choiceIds.add(correctPokemon.getId());

                    wrongCandidates.stream()
                            .limit(CHOICE_COUNT - 1)
                            .map(Pokemon::getId)
                            .forEach(choiceIds::add);

                    // 正解位置が固定されないように並べ替える
                    Collections.shuffle(choiceIds);

                    questions.add(new QuizQuestion(
                            correctPokemon.getId(),
                            choiceIds));
                }
            }

            quizSession.setQuestions(questions);
        }

        // クイズの進行情報をHTTPセッションへ保存する
        httpSession.setAttribute("quizSession", quizSession);

        return "redirect:/quiz/question";
    }

    /**
     * 現在の問題画面を表示する
     *
     * クイズモードに応じて表示するテンプレートを切り替える
     *
     * @param httpSession HTTPセッション
     * @param model 画面に渡すデータ
     * @return 問題画面のテンプレート名
     */
    @GetMapping("/quiz/question")
    public String question(
            HttpSession httpSession,
            Model model) {

        QuizSession quizSession =
                (QuizSession) httpSession.getAttribute("quizSession");

        // クイズが開始されていない場合はメニューへ戻す
        if (quizSession == null) {
            return "redirect:/quiz";
        }

        // 全問題終了後は結果画面へ移動する
        if (quizSession.isFinished()) {
            return "redirect:/quiz/result";
        }

        // 回答済みなら問題画面ではなく回答結果を表示する
        if (quizSession.isAnswerSubmitted()) {
            return "redirect:/quiz/answer";
        }

        if (quizSession.getMode() == QuizMode.TYPE_QUIZ) {

            // タイプクイズの現在の問題を取得する
            TypeQuizQuestion typeQuestion =
                    quizSession.getCurrentTypeQuestion();

            Pokemon pokemon = pokemonRepository.findById(
                    typeQuestion.pokemonId()).orElseThrow();

            model.addAttribute("pokemon", pokemon);
            model.addAttribute("choices", typeQuestion.choices());
            model.addAttribute(
                    "questionNumber",
                    quizSession.getCurrentIndex() + 1);
            model.addAttribute(
                    "totalQuestion",
                    quizSession.getTypeQuestions().size());

            return "quiz-type";
        }

        // 通常クイズ・比較クイズの問題を取得する
        QuizQuestion question = quizSession.getCurrentQuestion();

        Pokemon correctPokemon =
                pokemonRepository.findById(
                        question.getCorrectPokemonId()
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "正解ポケモンが見つかりません")
                );

        // 選択肢のIDからポケモン情報を取得する
        List<Pokemon> choices =
                question.getChoicePokemonIds()
                        .stream()
                        .map(id ->
                                pokemonRepository.findById(id)
                                        .orElseThrow(() ->
                                                new IllegalArgumentException(
                                                        "選択肢のポケモンが見つかりません")
                                        )
                        )
                        .toList();

        model.addAttribute("pokemon", correctPokemon);
        model.addAttribute("choices", choices);
        model.addAttribute(
                "questionNumber",
                quizSession.getCurrentIndex() + 1);
        model.addAttribute(
                "totalQuestion",
                quizSession.getQuestions().size());
        model.addAttribute("quizMode", quizSession.getMode());

        // 比較クイズと通常クイズで表示画面を切り替える
        return isComparisonMode(quizSession.getMode())
                ? "quiz-comparison" : "quiz";
    }

    /**
     * ユーザーが選択した回答を判定する
     *
     * 回答済みの問題は再判定せず、
     * 二重送信による得点の重複加算を防止する
     *
     * @param selectedId 通常・比較クイズで選択したポケモンID
     * @param selectedTypeIndex タイプクイズで選択した選択肢番号
     * @param httpSession HTTPセッション
     * @return 回答結果画面へのリダイレクト
     */
    @PostMapping("/quiz/answer")
    public String answer(
            @RequestParam(required = false) Integer selectedId,
            @RequestParam(required = false) Integer selectedTypeIndex,
            HttpSession httpSession) {

        QuizSession quizSession =
                (QuizSession) httpSession.getAttribute("quizSession");

        if (quizSession == null) {
            return "redirect:/quiz";
        }

        if (quizSession.isFinished()) {
            return "redirect:/quiz/result";
        }

        /*
         * 二重送信防止
         *
         * 現在の問題が回答済みの場合は、
         * 正誤判定や得点加算を再実行しない
         */
        if (quizSession.isAnswerSubmitted()) {
            return "redirect:/quiz/answer";
        }

        if (quizSession.getMode() == QuizMode.TYPE_QUIZ) {

            // タイプクイズの現在の問題を取得する
            TypeQuizQuestion question =
                    quizSession.getCurrentTypeQuestion();

            // 選択肢番号が有効な範囲内かを確認する
            if (selectedTypeIndex == null
                    || selectedTypeIndex < 0
                    || selectedTypeIndex >= question.choices().size()) {
                return "redirect:/quiz/question";
            }

            Pokemon pokemon = pokemonRepository.findById(
                    question.pokemonId()).orElseThrow();

            TypeCombination correctType =
                    new TypeCombination(
                            pokemon.getType1(),
                            pokemon.getType2());

            TypeCombination selectedType =
                    question.choices().get(selectedTypeIndex);

            boolean correct = correctType.equals(selectedType);

            // 回答結果をセッションに記録する
            quizSession.setSelectedTypeIndex(selectedTypeIndex);
            quizSession.setCorrect(correct);
            quizSession.setTypeAnswerSubmitted(true);

            if (correct) {
                quizSession.incrementScore();
            } else {
                quizSession.getWrongPokemonIds()
                        .add(pokemon.getId());
            }

        } else {

            // 回答IDが指定されていない場合は受け付けない
            if (selectedId == null) {
                return "redirect:/quiz/question";
            }

            QuizQuestion question = quizSession.getCurrentQuestion();

            /*
             * 選択肢に存在しないIDは受け付けない
             *
             * 画面のフォームを書き換えた場合でも、
             * 不正なIDを回答として処理しない
             */
            if (!question.getChoicePokemonIds().contains(selectedId)) {
                return "redirect:/quiz/question";
            }

            // 通常クイズ・比較クイズの回答を判定する
            boolean correct =
                    selectedId.equals(question.getCorrectPokemonId());

            quizSession.setSelectedPokemonId(selectedId);
            quizSession.setCorrect(correct);

            if (correct) {
                quizSession.incrementScore();
            } else {
                quizSession.getWrongPokemonIds()
                        .add(question.getCorrectPokemonId());
            }
        }

        // 全クイズ共通の回答済み状態を更新する
        quizSession.setAnswerSubmitted(true);

        httpSession.setAttribute("quizSession", quizSession);

        return "redirect:/quiz/answer";
    }

    /**
     * 現在の問題の回答結果を表示する
     *
     * 正解情報、ユーザーの回答、現在の得点を
     * 画面へ渡す
     *
     * @param httpSession HTTPセッション
     * @param model 画面に渡すデータ
     * @return 回答結果画面のテンプレート名
     */
    @GetMapping("/quiz/answer")
    public String answerResult(
            HttpSession httpSession,
            Model model) {

        QuizSession quizSession =
                (QuizSession) httpSession.getAttribute("quizSession");

        if (quizSession == null) {
            return "redirect:/quiz";
        }

        // 全問題終了後は最終結果画面へ移動する
        if (quizSession.isFinished()) {
            return "redirect:/quiz/result";
        }

        // 未回答の場合は問題画面へ戻す
        if (!quizSession.isAnswerSubmitted()) {
            return "redirect:/quiz/question";
        }

        if (quizSession.getMode() == QuizMode.TYPE_QUIZ) {

            TypeQuizQuestion typeQuestion =
                    quizSession.getCurrentTypeQuestion();

            Pokemon pokemon = pokemonRepository.findById(
                    typeQuestion.pokemonId()).orElseThrow();

            TypeCombination correctType =
                    new TypeCombination(
                            pokemon.getType1(),
                            pokemon.getType2());

            model.addAttribute("pokemon", pokemon);
            model.addAttribute("correct", quizSession.isCorrect());
            model.addAttribute("correctType", correctType);
            model.addAttribute(
                    "selectedType",
                    typeQuestion.choices().get(
                            quizSession.getSelectedTypeIndex()));
            model.addAttribute("choices", typeQuestion.choices());
            model.addAttribute("score", quizSession.getScore());
            model.addAttribute(
                    "questionNumber",
                    quizSession.getCurrentIndex() + 1);
            model.addAttribute(
                    "totalQuestion",
                    quizSession.getTypeQuestions().size());

            return "quiz-type-answer";
        }

        // 通常クイズ・比較クイズの正解情報を取得する
        QuizQuestion question = quizSession.getCurrentQuestion();

        Pokemon correctPokemon =
                pokemonRepository.findById(
                        question.getCorrectPokemonId()
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "正解ポケモンが見つかりません")
                );

        // ユーザーが選択したポケモンを取得する
        Pokemon selectedPokemon =
                pokemonRepository.findById(
                        quizSession.getSelectedPokemonId()
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "選択したポケモンが見つかりません")
                );

        model.addAttribute("correct", quizSession.isCorrect());
        model.addAttribute("correctPokemon", correctPokemon);
        model.addAttribute("selectedPokemon", selectedPokemon);
        model.addAttribute("score", quizSession.getScore());
        model.addAttribute(
                "questionNumber",
                quizSession.getCurrentIndex() + 1);
        model.addAttribute(
                "totalQuestion",
                quizSession.getQuestions().size());

        // 回答結果画面でも選択肢を表示する
        List<Pokemon> choices = question.getChoicePokemonIds()
                .stream()
                .map(id -> pokemonRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "選択肢のポケモンが見つかりません")))
                .toList();

        model.addAttribute("choices", choices);
        model.addAttribute("quizMode", quizSession.getMode());

        return isComparisonMode(quizSession.getMode())
                ? "quiz-comparison-answer" : "quiz-answer";
    }

    /**
     * 次の問題へ進む
     *
     * 回答済みの問題からのみ次へ進める
     * 未回答の場合や、重複して送信された場合は
     * 問題番号を更新しない
     *
     * @param httpSession HTTPセッション
     * @return 次の問題または結果画面へのリダイレクト
     */
    @PostMapping("/quiz/next")
    public String next(HttpSession httpSession) {

        QuizSession quizSession =
                (QuizSession) httpSession.getAttribute("quizSession");

        if (quizSession == null) {
            return "redirect:/quiz";
        }

        if (quizSession.isFinished()) {
            return "redirect:/quiz/result";
        }

        /*
         * 未回答の場合は次へ進めない
         *
         * moveNext()で回答済み状態をfalseに戻すため、
         * 同じリクエストを連続送信しても問題を飛ばさない
         */
        if (!quizSession.isAnswerSubmitted()) {
            return "redirect:/quiz/question";
        }

        // 次の問題へ進み、回答状態を初期化する
        quizSession.moveNext();

        httpSession.setAttribute("quizSession", quizSession);

        if (quizSession.isFinished()) {
            return "redirect:/quiz/result";
        }

        return "redirect:/quiz/question";
    }

    /**
     * クイズの最終結果を表示する
     *
     * 得点、総問題数、不正解だったポケモンを画面へ渡す
     * 表示後はHTTPセッションからクイズ情報を削除する
     *
     * @param httpSession HTTPセッション
     * @param model 画面に渡すデータ
     * @return 結果画面のテンプレート名
     */
    @GetMapping("/quiz/result")
    public String result(
            HttpSession httpSession,
            Model model) {

        QuizSession quizSession =
                (QuizSession) httpSession.getAttribute("quizSession");

        if (quizSession == null) {
            return "redirect:/quiz";
        }

        // 全問題が終了していなければ問題画面へ戻す
        if (!quizSession.isFinished()) {
            return "redirect:/quiz/question";
        }

        // 不正解だったポケモンを重複なく取得する
        List<Pokemon> wrongPokemonList =
                quizSession.getWrongPokemonIds()
                        .stream()
                        .distinct()
                        .map(id ->
                                pokemonRepository.findById(id)
                                        .orElseThrow()
                        )
                        .toList();

        model.addAttribute("score", quizSession.getScore());

        // クイズモードに応じて総問題数を取得する
        int total =
                quizSession.getMode() == QuizMode.TYPE_QUIZ
                        ? quizSession.getTypeQuestions().size()
                        : quizSession.getQuestions().size();

        model.addAttribute("total", total);
        model.addAttribute("wrongPokemonList", wrongPokemonList);

        // 終了したクイズの情報をセッションから削除する
        httpSession.removeAttribute("quizSession");

        return "quiz-result";
    }

    /**
     * クイズモードに応じた出題候補を取得する
     *
     * @param mode クイズモード
     * @param generation 指定世代
     * @return 出題候補のポケモン一覧
     */
    private List<Pokemon> getCandidates(
            QuizMode mode,
            Integer generation) {

        return switch (mode) {

            case GENERATION_FINAL -> {

                if (generation == null) {
                    throw new IllegalArgumentException(
                            "世代を選択してください");
                }

                yield pokemonRepository
                        .findByGenerationAndEvolvesToIsNull(generation);
            }

            case ALL_GENERATIONS_FINAL ->
                    pokemonRepository.findByEvolvesToIsNull();

            case ALL_POKEMON ->
                    pokemonRepository.findAll();

            case HEIGHT_COMPARISON, WEIGHT_COMPARISON, TYPE_QUIZ ->
                    throw new IllegalArgumentException(
                            "このクイズは専用の問題生成処理を使用します");
        };
    }

    /**
     * 指定されたモードが比較クイズかを判定する
     *
     * @param mode クイズモード
     * @return 高さ・重さ比較クイズの場合はtrue
     */
    private boolean isComparisonMode(QuizMode mode) {
        return mode == QuizMode.HEIGHT_COMPARISON
                || mode == QuizMode.WEIGHT_COMPARISON;
    }

    /**
     * 高さ・重さの比較クイズを10問生成する
     *
     * 比較値が異なる40匹を選び、4匹ずつ問題に分割する
     * これにより、各問題の最大値を持つポケモンが
     * 必ず1匹になるようにする
     *
     * @param mode 高さまたは重さの比較モード
     * @return 生成した10問の一覧
     */
    private List<QuizQuestion> createComparisonQuestions(QuizMode mode) {

        // 比較する値が登録され、0より大きいポケモンを取得する
        List<Pokemon> candidates = pokemonRepository.findAll()
                .stream()
                .filter(pokemon -> {
                    BigDecimal value = getComparisonValue(pokemon, mode);

                    return value != null
                            && value.compareTo(BigDecimal.ZERO) > 0;
                })
                .collect(Collectors.toCollection(ArrayList::new));

        if (candidates.size() < QUESTION_COUNT * CHOICE_COUNT) {
            throw new IllegalStateException(
                    "比較クイズに必要な40匹のポケモンがいません");
        }

        // 同じ比較値を持つポケモンが重複しないように選ぶ
        Collections.shuffle(candidates);

        List<Pokemon> selected = new ArrayList<>();
        List<BigDecimal> usedValues = new ArrayList<>();

        for (Pokemon pokemon : candidates) {

            BigDecimal value = getComparisonValue(pokemon, mode);

            boolean alreadyUsed = usedValues.stream()
                    .anyMatch(used -> used.compareTo(value) == 0);

            if (!alreadyUsed) {
                selected.add(pokemon);
                usedValues.add(value);
            }

            if (selected.size() == QUESTION_COUNT * CHOICE_COUNT) {
                break;
            }
        }

        if (selected.size() < QUESTION_COUNT * CHOICE_COUNT) {
            throw new IllegalStateException(
                    "比較値が異なるポケモンが40匹未満のため、問題を生成できません");
        }

        // 選択した40匹をシャッフルして4匹ずつに分割する
        Collections.shuffle(selected);

        List<QuizQuestion> questions = new ArrayList<>();

        for (int i = 0; i < QUESTION_COUNT; i++) {

            List<Pokemon> choices = new ArrayList<>(
                    selected.subList(
                            i * CHOICE_COUNT,
                            (i + 1) * CHOICE_COUNT));

            // 比較値が最も大きいポケモンを正解とする
            Pokemon correctPokemon = choices.stream()
                    .max(Comparator.comparing(
                            pokemon -> getComparisonValue(pokemon, mode)))
                    .orElseThrow();

            List<Integer> choiceIds = choices.stream()
                    .map(Pokemon::getId)
                    .toList();

            questions.add(new QuizQuestion(
                    correctPokemon.getId(),
                    choiceIds));
        }

        return questions;
    }

    /**
     * 比較クイズで使用する数値を取得する
     *
     * @param pokemon 対象ポケモン
     * @param mode 比較クイズのモード
     * @return 高さまたは重さ
     */
    private BigDecimal getComparisonValue(
            Pokemon pokemon,
            QuizMode mode) {

        return switch (mode) {
            case HEIGHT_COMPARISON -> pokemon.getHeight();
            case WEIGHT_COMPARISON -> pokemon.getWeight();
            default -> throw new IllegalArgumentException(
                    "比較クイズではないモードです");
        };
    }
}
