
package com.example.pokemonlab.quiz;

/**
 * クイズの出題モードを定義する列挙型
 *
 * 選択したモードに応じて、出題対象や問題形式を切り替える
 */
public enum QuizMode {

    /** 指定した世代の最終進化ポケモンを対象とする種族値クイズ */
    GENERATION_FINAL,

    /** 全世代の最終進化ポケモンを対象とする種族値クイズ */
    ALL_GENERATIONS_FINAL,

    /** 全ポケモンを対象とする種族値クイズ */
    ALL_POKEMON,

    /** ポケモンの高さを比較するクイズ */
    HEIGHT_COMPARISON,

    /** ポケモンの重さを比較するクイズ */
    WEIGHT_COMPARISON,

    /** ポケモンのタイプクイズ */
    TYPE_QUIZ
}
