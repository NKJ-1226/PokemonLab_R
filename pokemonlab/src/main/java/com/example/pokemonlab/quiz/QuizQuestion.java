
package com.example.pokemonlab.quiz;

import java.util.List;

/**
 * 通常クイズおよび比較クイズの1問分の情報を保持する
 *
 * 正解ポケモンのIDと、画面に表示する選択肢のID一覧を管理する
 */
public class QuizQuestion {

    /** 正解となるポケモンのID */
    private final Integer correctPokemonId;

    /** 選択肢として表示するポケモンのID一覧 */
    private final List<Integer> choicePokemonIds;

    /**
     * クイズの問題情報を生成する
     *
     * @param correctPokemonId 正解ポケモンのID
     * @param choicePokemonIds 選択肢のポケモンID一覧
     */
    public QuizQuestion(Integer correctPokemonId,List<Integer> choicePokemonIds) {

        this.correctPokemonId = correctPokemonId;
        this.choicePokemonIds = choicePokemonIds;
    }

    /**
     * 正解ポケモンのIDを取得する
     *
     * @return 正解ポケモンのID
     */
    public Integer getCorrectPokemonId() {
        return correctPokemonId;
    }

    /**
     * 選択肢のポケモンID一覧を取得する
     *
     * @return 選択肢のID一覧
     */
    public List<Integer> getChoicePokemonIds() {
        return choicePokemonIds;
    }
}
