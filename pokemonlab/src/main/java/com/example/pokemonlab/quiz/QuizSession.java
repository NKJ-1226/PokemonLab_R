package com.example.pokemonlab.quiz;

import java.util.ArrayList;
import java.util.List;

public class QuizSession {

    private QuizMode mode;
    private Integer generation;

    private List<QuizQuestion> questions = new ArrayList<>();

    private int currentIndex;
    private int score;

    private Integer selectedPokemonId;
    private boolean correct;
    private List<Integer> wrongPokemonIds = new ArrayList<>();

    public QuizMode getMode() {
        return mode;
    }

    public void setMode(QuizMode mode) {
        this.mode = mode;
    }

    public Integer getGeneration() {
        return generation;
    }

    public void setGeneration(Integer generation) {
        this.generation = generation;
    }

    public List<QuizQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuizQuestion> questions) {
        this.questions = questions;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public QuizQuestion getCurrentQuestion() {
        return questions.get(currentIndex);
    }

    public void incrementScore() {
        score++;
    }

    public void moveNext() {
        currentIndex++;
    }

    public boolean isFinished() {
        return currentIndex >= questions.size();
    }

    public Integer getSelectedPokemonId() {
        return selectedPokemonId;
    }

    public void setSelectedPokemonId(Integer selectedPokemonId) {
        this.selectedPokemonId = selectedPokemonId;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public List<Integer> getWrongPokemonIds() {
        return wrongPokemonIds;
    }

    public void setWrongPokemonIds(List<Integer> wrongPokemonIds) {
        this.wrongPokemonIds = wrongPokemonIds;
    }    
}