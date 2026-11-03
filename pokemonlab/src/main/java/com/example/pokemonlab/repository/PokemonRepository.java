
package com.example.pokemonlab.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import com.example.pokemonlab.entity.Pokemon;

/**
 * ポケモンの情報をDBから取得する
 *
 * 基本的なCRUD操作に加えて、図鑑検索や
 * 前後のポケモン取得などの処理を提供する
 */
public interface PokemonRepository extends JpaRepository<Pokemon, Integer>,JpaSpecificationExecutor<Pokemon> {

    /** DBに登録されている世代番号を重複なし・昇順で取得する */
    @Query("SELECT DISTINCT p.generation FROM Pokemon p WHERE p.generation IS NOT NULL ORDER BY p.generation")
    List<Integer> findAvailableGenerations();

    /**
     * 指定IDより小さいIDを持つ直前のポケモンを取得する
     *
     * @param id 現在表示しているポケモンのID
     * @return 直前のポケモン存在しない場合は空
     */
    Optional<Pokemon> findFirstByIdLessThanOrderByIdDesc(Integer id);

    /**
     * 指定IDより大きいIDを持つ次のポケモンを取得する
     *
     * @param id 現在表示しているポケモンのID
     * @return 次のポケモン存在しない場合は空
     */
    Optional<Pokemon> findFirstByIdGreaterThanOrderByIdAsc(Integer id);

    /**
     * ポケモンをランダムに4匹取得する
     *
     * @return ランダムに選ばれたポケモン一覧
     */
    @Query(value = "SELECT * FROM pokemon ORDER BY RAND() LIMIT 4",nativeQuery = true)

    List<Pokemon> findRandomFour();

    /**
     * 指定世代に属する、進化先が未設定のポケモンを取得する
     *
     * @param generation 検索対象の世代
     * @return 条件に一致するポケモン一覧
     */
    List<Pokemon> findByGenerationAndEvolvesToIsNull(Integer generation);

    /**
     * 進化先が未設定のポケモンをすべて取得する
     *
     * @return 進化先が未設定のポケモン一覧
     */
    List<Pokemon> findByEvolvesToIsNull();
}
