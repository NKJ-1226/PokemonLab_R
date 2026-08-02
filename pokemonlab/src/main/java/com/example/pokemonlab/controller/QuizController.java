package com.example.pokemonlab.controller;

import java.util.ArrayList;
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

@Controller
public class QuizController {

    private static final int QUESTION_COUNT = 10;
    private static final int CHOICE_COUNT = 4;

    private final PokemonRepository pokemonRepository;

    public QuizController(PokemonRepository pokemonRepository) {
        this.pokemonRepository = pokemonRepository;
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

        List<Pokemon> candidates =
                new ArrayList<>(getCandidates(mode, generation));

        if (candidates.size() < QUESTION_COUNT) {
            throw new IllegalStateException(
                    "10問分のポケモンを取得できませんでした。"
            );
        }

        Collections.shuffle(candidates);

        List<Pokemon> correctPokemonList =
                new ArrayList<>(
                        candidates.subList(0, QUESTION_COUNT)
                );

        List<QuizQuestion> questions = new ArrayList<>();

        for (Pokemon correctPokemon : correctPokemonList) {

            List<Pokemon> wrongCandidates =
                    candidates.stream()
                            .filter(pokemon ->
                                    !pokemon.getId().equals(
                                            correctPokemon.getId()
                                    )
                            )
                            .collect(
                                    Collectors.toCollection(
                                            ArrayList::new
                                    )
                            );

            Collections.shuffle(wrongCandidates);

            List<Integer> choiceIds = new ArrayList<>();

            choiceIds.add(correctPokemon.getId());

            wrongCandidates.stream()
                    .limit(CHOICE_COUNT - 1)
                    .map(Pokemon::getId)
                    .forEach(choiceIds::add);

            Collections.shuffle(choiceIds);

            questions.add(
                    new QuizQuestion(
                            correctPokemon.getId(),
                            choiceIds
                    )
            );
        }

        QuizSession quizSession = new QuizSession();

        quizSession.setMode(mode);
        quizSession.setGeneration(generation);
        quizSession.setQuestions(questions);
        quizSession.setCurrentIndex(0);
        quizSession.setScore(0);

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

        return "quiz";
    }

    @PostMapping("/quiz/answer")
    public String answer(
            @RequestParam Integer selectedId,
            HttpSession httpSession) {

        QuizSession quizSession =
                (QuizSession) httpSession.getAttribute("quizSession");

        if (quizSession == null) {
            return "redirect:/quiz";
        }

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

        httpSession.setAttribute("quizSession", quizSession);

        return "redirect:/quiz/answer";
    }

    @GetMapping("/quiz/answer")
    public String answerResult(
            HttpSession httpSession,
            Model model) {

        QuizSession quizSession =
                (QuizSession) httpSession.getAttribute("quizSession");

        if (quizSession == null) {
            return "redirect:/quiz";
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

        return "quiz-answer";
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
    public String result(
            HttpSession httpSession,
            Model model) {

        QuizSession quizSession =
                (QuizSession) httpSession.getAttribute("quizSession");

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
        model.addAttribute(
                "total",
                quizSession.getQuestions().size()
        );
        model.addAttribute("wrongPokemonList", wrongPokemonList);

        httpSession.removeAttribute("quizSession");

        return "quiz-result";
    }

    private List<Pokemon> getCandidates(
            QuizMode mode,
            Integer generation) {

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
        };
    }
}