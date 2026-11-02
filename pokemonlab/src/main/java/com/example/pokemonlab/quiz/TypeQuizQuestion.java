
package com.example.pokemonlab.quiz;

import java.util.List;

/**
 * タイプクイズの1問分の情報を保持する
 *
 * @param pokemonId 出題対象のポケモンID
 * @param choices 画面に表示するタイプの選択肢一覧
 */
public record TypeQuizQuestion(Integer pokemonId,List<TypeCombination> choices) {
}
