package com.example.pokemonlab.quiz;

import java.util.List;

public record TypeQuizQuestion(
        Integer pokemonId,
        List<TypeCombination> choices) {
}