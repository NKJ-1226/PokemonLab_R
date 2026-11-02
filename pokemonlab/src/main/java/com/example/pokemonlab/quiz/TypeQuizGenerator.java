
package com.example.pokemonlab.quiz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import com.example.pokemonlab.entity.Pokemon;
import com.example.pokemonlab.repository.PokemonRepository;

/**
 * タイプクイズの問題を生成するクラス
 *
 * ポケモンをランダムに選び、正解のタイプ構成と
 * 不正解のタイプ構成を組み合わせて4択問題を作成する
 */
@Component
public class TypeQuizGenerator {

    /** 1回のクイズで出題する問題数 */
    private static final int QUESTION_COUNT = 10;

    /** 1問あたりの選択肢の数 */
    private static final int CHOICE_COUNT = 4;

    private final PokemonRepository pokemonRepository;

    public TypeQuizGenerator(PokemonRepository pokemonRepository) {
        this.pokemonRepository = pokemonRepository;
    }

    /**
     * タイプクイズを10問生成する
     *
     * @return 生成した問題一覧
     */
    public List<TypeQuizQuestion> generate() {

        // 第一タイプが登録されているポケモンを取得する
        List<Pokemon> pokemonList = pokemonRepository.findAll()
                .stream()
                .filter(pokemon -> pokemon.getType1() != null)
                .toList();

        // 出題順をランダムにする
        List<Pokemon> candidates = new ArrayList<>(pokemonList);
        Collections.shuffle(candidates);

        if (candidates.size() < QUESTION_COUNT) {
            throw new IllegalStateException(
                    "タイプクイズに必要なポケモンが不足しています");
        }

        // DBに登録されているタイプの組み合わせを重複なく取得する
        List<TypeCombination> combinations = pokemonList.stream()
                .map(pokemon -> new TypeCombination(
                        pokemon.getType1(),
                        pokemon.getType2()))
                .distinct()
                .toList();

        if (combinations.size() < CHOICE_COUNT) {
            throw new IllegalStateException(
                    "タイプの組み合わせが4種類未満です");
        }

        List<TypeQuizQuestion> questions = new ArrayList<>();

        for (int i = 0; i < QUESTION_COUNT; i++) {

            Pokemon pokemon = candidates.get(i);

            // 出題ポケモンの正解タイプを取得する
            TypeCombination correct = new TypeCombination(
                    pokemon.getType1(),
                    pokemon.getType2());

            // 正解以外のタイプ構成を不正解候補とする
            List<TypeCombination> wrongChoices =
                    new ArrayList<>(combinations);

            wrongChoices.remove(correct);
            Collections.shuffle(wrongChoices);

            // 正解1つと不正解3つを組み合わせる
            List<TypeCombination> choices = new ArrayList<>();
            choices.add(correct);
            choices.addAll(
                    wrongChoices.subList(0, CHOICE_COUNT - 1));

            // 正解位置が固定されないように並べ替える
            Collections.shuffle(choices);

            questions.add(new TypeQuizQuestion(
                    pokemon.getId(),
                    List.copyOf(choices)));
        }

        return questions;
    }
}
