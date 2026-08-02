package com.example.pokemonlab.quiz;

import java.util.List;

public class QuizQuestion {

    private final Integer correctPokemonId;
    private final List<Integer> choicePokemonIds;

    public QuizQuestion(
            Integer correctPokemonId,
            List<Integer> choicePokemonIds) {

        this.correctPokemonId = correctPokemonId;
        this.choicePokemonIds = choicePokemonIds;
    }

    public Integer getCorrectPokemonId() {
        return correctPokemonId;
    }

    public List<Integer> getChoicePokemonIds() {
        return choicePokemonIds;
    }
}