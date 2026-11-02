package com.example.pokemonlab.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
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
import com.example.pokemonlab.repository.PokemonRepository;
import jakarta.servlet.http.HttpSession;
import com.example.pokemonlab.quiz.TypeCombination;
import com.example.pokemonlab.quiz.TypeQuizGenerator;
import com.example.pokemonlab.quiz.TypeQuizQuestion;

@Controller
public class QuizController {

    private static final int QUESTION_COUNT = 10;
    private static final int CHOICE_COUNT = 4;

        private final PokemonRepository pokemonRepository;
        private final TypeQuizGenerator typeQuizGenerator;

        public QuizController(
                PokemonRepository pokemonRepository,
                TypeQuizGenerator typeQuizGenerator) {

        this.pokemonRepository = pokemonRepository;
        this.typeQuizGenerator = typeQuizGenerator;
        }

    @GetMapping("/quiz")
    public String quizMenu() {
        return "quiz-menu";
    }

        @PostMapping("/quiz/start")
        public String startQuiz(
                @RequestParam QuizMode mode,
                @RequestParam(required = false) Integer generation,
                HttpSession httpSession) {

        QuizSession quizSession = new QuizSession();

        quizSession.setMode(mode);
        quizSession.setGeneration(generation);
        quizSession.setCurrentIndex(0);
        quizSession.setScore(0);

        if (mode == QuizMode.TYPE_QUIZ) {

                // タイプクイズ専用の10問を生成
                quizSession.setTypeQuestions(
                        typeQuizGenerator.generate());

        } else {

                List<QuizQuestion> questions;

                if (isComparisonMode(mode)) {
                questions = createComparisonQuestions(mode);
                } else {

                List<Pokemon> candidates =
                        new ArrayList<>(getCandidates(mode, generation));

                if (candidates.size() < QUESTION_COUNT) {
                        throw new IllegalStateException(
                                "10問分のポケモンを取得できませんでした。");
                }

                Collections.shuffle(candidates);

                List<Pokemon> correctPokemonList =
                        new ArrayList<>(
                                candidates.subList(0, QUESTION_COUNT));

                questions = new ArrayList<>();

                for (Pokemon correctPokemon : correctPokemonList) {

                        List<Pokemon> wrongCandidates =
                                candidates.stream()
                                        .filter(pokemon ->
                                                !pokemon.getId().equals(
                                                        correctPokemon.getId()))
                                        .collect(Collectors.toCollection(
                                                ArrayList::new));

                        Collections.shuffle(wrongCandidates);

                        List<Integer> choiceIds = new ArrayList<>();

                        choiceIds.add(correctPokemon.getId());

                        wrongCandidates.stream()
                                .limit(CHOICE_COUNT - 1)
                                .map(Pokemon::getId)
                                .forEach(choiceIds::add);

                        Collections.shuffle(choiceIds);

                        questions.add(new QuizQuestion(
                                correctPokemon.getId(),
                                choiceIds));
                }
                }

                quizSession.setQuestions(questions);
        }

        httpSession.setAttribute("quizSession", quizSession);

        return "redirect:/quiz/question";
        }

    @GetMapping("/quiz/question")
    public String question(
            HttpSession httpSession,
            Model model) {

        QuizSession quizSession =
                (QuizSession) httpSession.getAttribute("quizSession");

        if (quizSession == null) {
            return "redirect:/quiz";
        }

        if (quizSession.isFinished()) {
            return "redirect:/quiz/result";
        }

        // タイプクイズの条件分岐
        if (quizSession.getMode() == QuizMode.TYPE_QUIZ) {

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

        QuizQuestion question = quizSession.getCurrentQuestion();

        Pokemon correctPokemon =
                pokemonRepository.findById(
                        question.getCorrectPokemonId()
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "正解ポケモンが見つかりません。"
                        )
                );

        List<Pokemon> choices =
                question.getChoicePokemonIds()
                        .stream()
                        .map(id ->
                                pokemonRepository.findById(id)
                                        .orElseThrow(() ->
                                                new IllegalArgumentException(
                                                        "選択肢のポケモンが見つかりません。"
                                                )
                                        )
                        )
                        .toList();

        model.addAttribute("pokemon", correctPokemon);
        model.addAttribute("choices", choices);
        model.addAttribute(
                "questionNumber",
                quizSession.getCurrentIndex() + 1
        );
        model.addAttribute(
                "totalQuestion",
                quizSession.getQuestions().size()
        );

        model.addAttribute("quizMode", quizSession.getMode());

        return isComparisonMode(quizSession.getMode())
                ? "quiz-comparison" : "quiz";
    }

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

        if (quizSession.getMode() == QuizMode.TYPE_QUIZ) {

                if (quizSession.isTypeAnswerSubmitted()) {
                return "redirect:/quiz/answer";
                }

                TypeQuizQuestion question =
                        quizSession.getCurrentTypeQuestion();

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

                if (selectedId == null) {
                return "redirect:/quiz/question";
                }

                // 通常クイズの回答処理
                QuizQuestion question = quizSession.getCurrentQuestion();

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

        httpSession.setAttribute("quizSession", quizSession);

        return "redirect:/quiz/answer";
        }

    @GetMapping("/quiz/answer")
    public String answerResult(
            HttpSession httpSession,
            Model model) {

        QuizSession quizSession = (QuizSession) httpSession.getAttribute("quizSession");

        if (quizSession == null) {
            return "redirect:/quiz";
        }

        if (quizSession.getMode() == QuizMode.TYPE_QUIZ) {

        if (!quizSession.isTypeAnswerSubmitted()) {
                return "redirect:/quiz/question";
        }

        TypeQuizQuestion typeQuestion = quizSession.getCurrentTypeQuestion();

        Pokemon pokemon = pokemonRepository.findById(typeQuestion.pokemonId()).orElseThrow();

        TypeCombination correctType =
                new TypeCombination(
                        pokemon.getType1(),pokemon.getType2());
                        model.addAttribute("pokemon", pokemon);
                        model.addAttribute("correct", quizSession.isCorrect());
                        model.addAttribute("correctType", correctType);
                        model.addAttribute("selectedType",typeQuestion.choices().get(quizSession.getSelectedTypeIndex()));
                        model.addAttribute("choices", typeQuestion.choices());
                        model.addAttribute("score", quizSession.getScore());
                        model.addAttribute("questionNumber",quizSession.getCurrentIndex() + 1);
                        model.addAttribute("totalQuestion",quizSession.getTypeQuestions().size());

        return "quiz-type-answer";
        }

        QuizQuestion question = quizSession.getCurrentQuestion();

        Pokemon correctPokemon =
                pokemonRepository.findById(
                        question.getCorrectPokemonId()
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "正解ポケモンが見つかりません。"
                        )
                );

        Pokemon selectedPokemon =
                pokemonRepository.findById(
                        quizSession.getSelectedPokemonId()
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "選択したポケモンが見つかりません。"
                        )
                );

        model.addAttribute("correct", quizSession.isCorrect());
        model.addAttribute("correctPokemon", correctPokemon);
        model.addAttribute("selectedPokemon", selectedPokemon);
        model.addAttribute("score", quizSession.getScore());
        model.addAttribute(
                "questionNumber",
                quizSession.getCurrentIndex() + 1
        );
        model.addAttribute(
                "totalQuestion",
                quizSession.getQuestions().size()
        );

        List<Pokemon> choices = question.getChoicePokemonIds().stream()
                .map(id -> pokemonRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "選択肢のポケモンが見つかりません。")))
                .toList();

        model.addAttribute("choices", choices);
        model.addAttribute("quizMode", quizSession.getMode());

        return isComparisonMode(quizSession.getMode())
                ? "quiz-comparison-answer" : "quiz-answer";
    }

    @PostMapping("/quiz/next")
    public String next(HttpSession httpSession) {

        QuizSession quizSession =
                (QuizSession) httpSession.getAttribute("quizSession");

        if (quizSession == null) {
            return "redirect:/quiz";
        }

        quizSession.moveNext();

        httpSession.setAttribute("quizSession", quizSession);

        if (quizSession.isFinished()) {
            return "redirect:/quiz/result";
        }

        return "redirect:/quiz/question";
    }

    @GetMapping("/quiz/result")
    public String result(HttpSession httpSession,Model model) {

        QuizSession quizSession =(QuizSession) httpSession.getAttribute("quizSession");

        if (quizSession == null) {
            return "redirect:/quiz";
        }

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
        int total =
                quizSession.getMode() == QuizMode.TYPE_QUIZ
                ? quizSession.getTypeQuestions().size(): quizSession.getQuestions().size();
                model.addAttribute("total", total);
                model.addAttribute("wrongPokemonList", wrongPokemonList);
                httpSession.removeAttribute("quizSession");

        return "quiz-result";
    }

    private List<Pokemon> getCandidates(QuizMode mode,Integer generation) {

        return switch (mode) {

            case GENERATION_FINAL -> {

                if (generation == null) {
                    throw new IllegalArgumentException(
                            "世代を選択してください。"
                    );
                }

                yield pokemonRepository
                        .findByGenerationAndEvolvesToIsNull(
                                generation
                        );
            }

            case ALL_GENERATIONS_FINAL ->
                    pokemonRepository.findByEvolvesToIsNull();

            case ALL_POKEMON ->
                    pokemonRepository.findAll();

                case HEIGHT_COMPARISON, WEIGHT_COMPARISON, TYPE_QUIZ ->
                        throw new IllegalArgumentException("このクイズは専用の問題生成処理を使用します。");
        };
    }

    // 比較クイズかどうかを判定する
    private boolean isComparisonMode(QuizMode mode) {
        return mode == QuizMode.HEIGHT_COMPARISON
                || mode == QuizMode.WEIGHT_COMPARISON;
    }

    // 高さ・重さが登録されたポケモンから、10問分の4択問題を生成する
    private List<QuizQuestion> createComparisonQuestions(QuizMode mode) {
        List<Pokemon> candidates = pokemonRepository.findAll().stream()
                .filter(pokemon -> {
                    BigDecimal value = getComparisonValue(pokemon, mode);
                    return value != null && value.compareTo(BigDecimal.ZERO) > 0;
                })
                .collect(Collectors.toCollection(ArrayList::new));

        if (candidates.size() < QUESTION_COUNT * CHOICE_COUNT) {
            throw new IllegalStateException(
                    "比較クイズに必要な40匹のポケモンがいません。");
        }

        // 最大値の同点を避けるため、数値が異なる40匹をランダムに選ぶ。
        // これならどの4匹を組み合わせても正解が1匹になる。
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
                    "比較値が異なるポケモンが40匹未満のため、問題を生成できません。");
        }

        Collections.shuffle(selected);
        List<QuizQuestion> questions = new ArrayList<>();

        for (int i = 0; i < QUESTION_COUNT; i++) {
            List<Pokemon> choices = new ArrayList<>(selected.subList(
                    i * CHOICE_COUNT, (i + 1) * CHOICE_COUNT));

            Pokemon correctPokemon = choices.stream()
                    .max(Comparator.comparing(pokemon ->
                            getComparisonValue(pokemon, mode)))
                    .orElseThrow();

            List<Integer> choiceIds = choices.stream()
                    .map(Pokemon::getId)
                    .toList();

            questions.add(new QuizQuestion(correctPokemon.getId(), choiceIds));
        }

        return questions;
    }

    // 比較する数値を取得する
    private BigDecimal getComparisonValue(Pokemon pokemon, QuizMode mode) {
        return switch (mode) {
            case HEIGHT_COMPARISON -> pokemon.getHeight();
            case WEIGHT_COMPARISON -> pokemon.getWeight();
            default -> throw new IllegalArgumentException(
                    "比較クイズではないモードです。");
        };
    }
}