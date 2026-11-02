package com.example.pokemonlab.quiz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import com.example.pokemonlab.entity.Pokemon;
import com.example.pokemonlab.repository.PokemonRepository;

@Component
public class TypeQuizGenerator {

    private static final int QUESTION_COUNT = 10;
    private static final int CHOICE_COUNT = 4;

    private final PokemonRepository pokemonRepository;

    public TypeQuizGenerator(PokemonRepository pokemonRepository) {
        this.pokemonRepository = pokemonRepository;
    }

    public List<TypeQuizQuestion> generate() {

        List<Pokemon> pokemonList = pokemonRepository.findAll()
                .stream()
                .filter(p -> p.getType1() != null)
                .toList();

        List<Pokemon> candidates = new ArrayList<>(pokemonList);
        Collections.shuffle(candidates);

        if (candidates.size() < QUESTION_COUNT) {
            throw new IllegalStateException(
                    "タイプクイズに必要なポケモンが不足しています。");
        }

        // DBに存在するタイプの組み合わせを取得
        List<TypeCombination> combinations = pokemonList.stream()
                .map(p -> new TypeCombination(
                        p.getType1(), p.getType2()))
                .distinct()
                .toList();

        if (combinations.size() < CHOICE_COUNT) {
            throw new IllegalStateException(
                    "タイプの組み合わせが4種類未満です。");
        }

        List<TypeQuizQuestion> questions = new ArrayList<>();

        for (int i = 0; i < QUESTION_COUNT; i++) {

            Pokemon pokemon = candidates.get(i);

            TypeCombination correct = new TypeCombination(
                    pokemon.getType1(), pokemon.getType2());

            List<TypeCombination> wrongChoices =
                    new ArrayList<>(combinations);

            wrongChoices.remove(correct);
            Collections.shuffle(wrongChoices);

            List<TypeCombination> choices = new ArrayList<>();
            choices.add(correct);
            choices.addAll(
                    wrongChoices.subList(0, CHOICE_COUNT - 1));

            Collections.shuffle(choices);

            questions.add(new TypeQuizQuestion(
                    pokemon.getId(),
                    List.copyOf(choices)));
        }

        return questions;
    }
}