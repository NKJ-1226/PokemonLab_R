
package com.example.pokemonlab.quiz;

import java.util.ArrayList;
import java.util.List;

/**
 * クイズの進行状況を管理するクラス
 *
 * HTTPセッションに保存され、問題一覧、現在の問題番号、
 * 得点、回答結果などを保持する
 */
public class QuizSession {

    /** 現在のクイズモード */
    private QuizMode mode;

    /** 世代別クイズで指定された世代 */
    private Integer generation;

    /** 通常クイズ・比較クイズの問題一覧 */
    private List<QuizQuestion> questions = new ArrayList<>();

    /** 現在の問題番号（0から開始） */
    private int currentIndex;

    /** 現在の正解数 */
    private int score;

    /** 通常クイズ・比較クイズで選択したポケモンのID */
    private Integer selectedPokemonId;

    /** 現在の問題が正解かどうか */
    private boolean correct;

    /** 不正解だったポケモンのID一覧 */
    private List<Integer> wrongPokemonIds = new ArrayList<>();

    /** 現在の問題に回答済みかどうか（全クイズ共通） */
    private boolean answerSubmitted;

    /** タイプクイズ専用の問題一覧 */
    private List<TypeQuizQuestion> typeQuestions = new ArrayList<>();

    /** タイプクイズで選択した選択肢の位置 */
    private Integer selectedTypeIndex;

    /** タイプクイズの回答送信が完了しているか */
    private boolean typeAnswerSubmitted;

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

    /**
     * 現在出題中の通常クイズ・比較クイズの問題を取得する
     *
     * @return 現在の問題
     */
    public QuizQuestion getCurrentQuestion() {
        return questions.get(currentIndex);
    }

    /**
     * 正解数を1加算する
     */
    public void incrementScore() {
        score++;
    }

    /**
     * 次の問題へ進み、回答状態を初期化する
     *
     * 問題番号を1つ進め、前の問題の回答情報が
     * 次の問題に引き継がれないようにする
     */
    public void moveNext() {
        currentIndex++;

        // 通常クイズ・比較クイズの回答情報を初期化
        selectedPokemonId = null;

        // 全クイズ共通の回答済み状態を初期化
        answerSubmitted = false;

        // タイプクイズの回答情報を初期化
        selectedTypeIndex = null;
        typeAnswerSubmitted = false;

        // 前の問題の正誤判定を初期化
        correct = false;
    }

    /**
     * 全問題の回答が終了したかを判定する
     *
     * @return 全問題終了ならtrue
     */
    public boolean isFinished() {
        if (mode == QuizMode.TYPE_QUIZ) {
            return currentIndex >= typeQuestions.size();
        }

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

    /**
     * 現在の問題が回答済みかどうかを取得する
     *
     * @return 回答済みの場合はtrue
     */
    public boolean isAnswerSubmitted() {
        return answerSubmitted;
    }

    /**
     * 現在の問題の回答済み状態を設定する
     *
     * @param answerSubmitted 回答済みかどうか
     */
    public void setAnswerSubmitted(boolean answerSubmitted) {
        this.answerSubmitted = answerSubmitted;
    }

    public List<TypeQuizQuestion> getTypeQuestions() {
        return typeQuestions;
    }

    public void setTypeQuestions(List<TypeQuizQuestion> typeQuestions) {
        this.typeQuestions = typeQuestions;
    }

    /**
     * 現在出題中のタイプクイズの問題を取得する
     *
     * @return 現在のタイプクイズ問題
     */
    public TypeQuizQuestion getCurrentTypeQuestion() {
        return typeQuestions.get(currentIndex);
    }

    public Integer getSelectedTypeIndex() {
        return selectedTypeIndex;
    }

    public void setSelectedTypeIndex(Integer selectedTypeIndex) {
        this.selectedTypeIndex = selectedTypeIndex;
    }

    public boolean isTypeAnswerSubmitted() {
        return typeAnswerSubmitted;
    }

    public void setTypeAnswerSubmitted(boolean typeAnswerSubmitted) {
        this.typeAnswerSubmitted = typeAnswerSubmitted;
    }
}
